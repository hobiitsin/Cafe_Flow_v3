package com.example.cafe_flow_v3.data.repository

import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import com.example.cafe_flow_v3.data.AppDatabase
import com.example.cafe_flow_v3.data.entity.DetallePedido
import com.example.cafe_flow_v3.data.entity.EstadoPedido
import com.example.cafe_flow_v3.data.entity.MetodoPago
import com.example.cafe_flow_v3.data.entity.OrigenPedido
import com.example.cafe_flow_v3.data.entity.Pedido
import com.example.cafe_flow_v3.data.model.ItemCarrito
import com.example.cafe_flow_v3.data.model.PedidoConDetalles
import kotlinx.coroutines.flow.Flow

sealed interface ResultadoVenta {
    data class Exito(val pedidoId: Long, val totalCentavos: Long) : ResultadoVenta
    data class Error(val mensaje: String) : ResultadoVenta
}

// ---------- Reglas de los estados ----------
// El orden del enum importa: PENDIENTE -> EN_PREPARACION -> LISTO -> COMPLETADO

fun EstadoPedido.esFinal(): Boolean =
    this == EstadoPedido.COMPLETADO || this == EstadoPedido.CANCELADO

// Para el botón "Avanzar" en la pantalla de Pedidos. null = ya no avanza.
fun EstadoPedido.siguiente(): EstadoPedido? = when (this) {
    EstadoPedido.PENDIENTE -> EstadoPedido.EN_PREPARACION
    EstadoPedido.EN_PREPARACION -> EstadoPedido.LISTO
    EstadoPedido.LISTO -> EstadoPedido.COMPLETADO
    EstadoPedido.COMPLETADO, EstadoPedido.CANCELADO -> null
}

/**
 * Ventas y pedidos. Aquí están las operaciones que tocan varias tablas,
 * por eso recibe la base completa (para usar transacciones).
 */
class PedidoRepository(private val db: AppDatabase) {

    private val pedidoDao = db.pedidoDao()
    private val productoDao = db.productoDao()

    // ---------- Lectura ----------

    fun observarPorEstado(estado: EstadoPedido): Flow<List<PedidoConDetalles>> =
        pedidoDao.observarPorEstado(estado)

    fun observarExternos(): Flow<List<PedidoConDetalles>> = pedidoDao.observarExternos()

    fun observarPorOrigen(origen: OrigenPedido): Flow<List<PedidoConDetalles>> =
        pedidoDao.observarPorOrigen(origen)

    suspend fun obtenerDetalle(pedidoId: Long): PedidoConDetalles? =
        pedidoDao.buscarConDetalles(pedidoId)

    // ---------- Registrar una venta ----------

    /**
     * Guarda el pedido, sus renglones y descuenta inventario en UNA transacción.
     * Si un producto no alcanza, se deshace todo (no queda nada a medias).
     *
     * El pedido nace PENDIENTE: cuenta como venta del día hasta que se marca COMPLETADO.
     */
    suspend fun registrarVenta(
        items: List<ItemCarrito>,
        metodoPago: MetodoPago,
        usuarioId: Long?,
        origen: OrigenPedido = OrigenPedido.MOSTRADOR,
        folioExterno: String? = null
    ): ResultadoVenta {
        if (items.isEmpty()) return ResultadoVenta.Error("Agrega al menos un producto")
        if (items.any { it.cantidad <= 0 }) return ResultadoVenta.Error("Hay productos con cantidad inválida")
        if (origen != OrigenPedido.MOSTRADOR && folioExterno.isNullOrBlank()) {
            return ResultadoVenta.Error("Escribe el folio del pedido de la plataforma")
        }

        return try {
            db.withTransaction<ResultadoVenta> {
                // Usamos el precio y stock ACTUALES de la base, no los del carrito
                // (pudieron cambiar mientras el cajero armaba el pedido)
                val lineas = items.map { item ->
                    val actual = productoDao.buscarPorId(item.producto.id)
                        ?: throw VentaRechazada("${item.producto.nombre} ya no existe")

                    if (!actual.disponible) {
                        throw VentaRechazada("${actual.nombre} no está disponible")
                    }
                    if (productoDao.descontarStock(actual.id, item.cantidad) == 0) {
                        throw VentaRechazada("No hay suficiente ${actual.nombre} (quedan ${actual.stock})")
                    }
                    item.copy(producto = actual)
                }

                val total = lineas.sumOf { it.subtotalCentavos }

                val pedidoId = pedidoDao.insertarPedido(
                    Pedido(
                        usuarioId = usuarioId,
                        estado = EstadoPedido.PENDIENTE,
                        origen = origen,
                        folioExterno = folioExterno?.trim(),
                        metodoPago = metodoPago,
                        totalCentavos = total
                    )
                )

                pedidoDao.insertarDetalles(
                    lineas.map {
                        DetallePedido(
                            pedidoId = pedidoId,
                            productoId = it.producto.id,
                            cantidad = it.cantidad,
                            precioUnitarioCentavos = it.producto.precioCentavos,
                            notas = it.notas?.trim()?.ifBlank { null }
                        )
                    }
                )

                ResultadoVenta.Exito(pedidoId, total)
            }
        } catch (e: VentaRechazada) {
            // Al lanzar la excepción dentro de withTransaction, Room deshace todo
            ResultadoVenta.Error(e.message ?: "No se pudo registrar la venta")
        } catch (e: SQLiteException) {
            ResultadoVenta.Error("Error al guardar la venta. Intenta de nuevo.")
        }
    }

    // ---------- Cambiar estado ----------

    /**
     * Avanza o cancela un pedido.
     * - No se puede regresar a un estado anterior.
     * - Completado y cancelado ya no se tocan.
     * - Al cancelar, el inventario se regresa.
     */
    suspend fun cambiarEstado(pedidoId: Long, nuevo: EstadoPedido): Result<Unit> =
        try {
            db.withTransaction<Result<Unit>> {
                val pedido = pedidoDao.buscarConDetalles(pedidoId)
                    ?: return@withTransaction fallo("El pedido no existe")

                val actual = pedido.pedido.estado
                if (actual.esFinal()) {
                    return@withTransaction fallo("Un pedido completado o cancelado ya no se puede modificar")
                }
                if (nuevo.ordinal <= actual.ordinal) {
                    return@withTransaction fallo("Un pedido no puede regresar a un estado anterior")
                }

                if (nuevo == EstadoPedido.CANCELADO) {
                    pedido.detalles.forEach {
                        productoDao.reponerStock(it.detalle.productoId, it.detalle.cantidad)
                    }
                }

                pedidoDao.actualizarEstado(pedidoId, nuevo)
                Result.success(Unit)
            }
        } catch (e: SQLiteException) {
            fallo("Error al actualizar el pedido. Intenta de nuevo.")
        }

    suspend fun avanzar(pedidoId: Long, estadoActual: EstadoPedido): Result<Unit> {
        val siguiente = estadoActual.siguiente() ?: return fallo("Este pedido ya no puede avanzar")
        return cambiarEstado(pedidoId, siguiente)
    }

    suspend fun cancelar(pedidoId: Long): Result<Unit> =
        cambiarEstado(pedidoId, EstadoPedido.CANCELADO)

    private class VentaRechazada(mensaje: String) : Exception(mensaje)
}
