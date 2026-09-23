package com.example.cafe_flow_v3.data.repository

import com.example.cafe_flow_v3.data.dao.PedidoDao
import com.example.cafe_flow_v3.data.dao.ReporteDao
import com.example.cafe_flow_v3.data.model.PedidoConDetalles
import com.example.cafe_flow_v3.data.model.VentaPorHora
import com.example.cafe_flow_v3.data.model.VentaPorOrigen
import com.example.cafe_flow_v3.data.model.VentaPorProducto
import com.example.cafe_flow_v3.util.Fechas
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

// Todo lo que muestra el Dashboard, en un solo objeto
data class ResumenDashboard(
    val ventasHoyCentavos: Long = 0,
    val variacionVsAyer: Int? = null, // +18 = 18% más que ayer. null si ayer no hubo ventas.
    val pendientes: Int = 0,
    val completadosHoy: Int = 0,
    val externosHoy: Int = 0,
    val ventasPorHora: List<VentaPorHora> = emptyList(),
    val recientes: List<PedidoConDetalles> = emptyList()
)

class ReporteRepository(
    private val reporteDao: ReporteDao,
    private val pedidoDao: PedidoDao
) {

    /**
     * Junta las 7 consultas del dashboard en un solo Flow.
     * Si cualquiera cambia (nueva venta, pedido completado...), se emite un resumen nuevo.
     *
     * Nota: "hoy" se calcula al llamar la función. Si la app queda abierta
     * después de medianoche, hay que volver a llamarla.
     */
    fun resumenDelDia(): Flow<ResumenDashboard> {
        val inicioHoy = Fechas.inicioDelDia()
        val finHoy = Fechas.finDelDia()
        val inicioAyer = Fechas.inicioDelDia(diasAtras = 1)
        val finAyer = Fechas.finDelDia(diasAtras = 1)

        // combine acepta máximo 5 flows con tipos distintos, así que lo hacemos en dos partes
        val metricas = combine(
            reporteDao.observarVentasEntre(inicioHoy, finHoy),
            reporteDao.observarVentasEntre(inicioAyer, finAyer),
            reporteDao.observarPendientes(),
            reporteDao.observarCompletadosEntre(inicioHoy, finHoy),
            reporteDao.observarExternosEntre(inicioHoy, finHoy)
        ) { ventasHoy, ventasAyer, pendientes, completados, externos ->
            ResumenDashboard(
                ventasHoyCentavos = ventasHoy,
                variacionVsAyer = calcularVariacion(ventasHoy, ventasAyer),
                pendientes = pendientes,
                completadosHoy = completados,
                externosHoy = externos
            )
        }

        return combine(
            metricas,
            reporteDao.observarVentasPorHora(inicioHoy, finHoy),
            pedidoDao.observarRecientes(limite = 3)
        ) { resumen, porHora, recientes ->
            resumen.copy(ventasPorHora = porHora, recientes = recientes)
        }
    }

    // ---------- Pantalla de Reportes ----------
    // Reciben un rango en milisegundos (inicio y fin). El ViewModel decide
    // si es un día, la última semana, etc. usando Fechas.

    fun ventasEntre(inicio: Long, fin: Long): Flow<Long> =
        reporteDao.observarVentasEntre(inicio, fin)

    fun pedidosCompletadosEntre(inicio: Long, fin: Long): Flow<Int> =
        reporteDao.observarCompletadosEntre(inicio, fin)

    fun ventasPorHoraEntre(inicio: Long, fin: Long): Flow<List<VentaPorHora>> =
        reporteDao.observarVentasPorHora(inicio, fin)

    fun ventasPorProductoEntre(inicio: Long, fin: Long): Flow<List<VentaPorProducto>> =
        reporteDao.observarVentasPorProducto(inicio, fin)

    fun ventasPorPlataformaEntre(inicio: Long, fin: Long): Flow<List<VentaPorOrigen>> =
        reporteDao.observarVentasPorOrigen(inicio, fin)

    private fun calcularVariacion(hoy: Long, ayer: Long): Int? =
        if (ayer == 0L) null else ((hoy - ayer) * 100 / ayer).toInt()
}