package com.example.cafe_flow_v3.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.example.cafe_flow_v3.data.entity.DetallePedido
import com.example.cafe_flow_v3.data.entity.OrigenPedido
import com.example.cafe_flow_v3.data.entity.Pedido
import com.example.cafe_flow_v3.data.entity.Producto

// Estas NO son tablas. Son "moldes" para el resultado de consultas
// que juntan varias tablas o que calculan totales.

// ---------- Pedido con sus productos ----------

// Un renglón del pedido + el producto al que pertenece (para mostrar el nombre)
data class DetalleConProducto(
    @Embedded
    val detalle: DetallePedido,

    @Relation(
        parentColumn = "producto_id",
        entityColumn = "id"
    )
    val producto: Producto
)

// Un pedido completo: "#1024 · 1x Espresso Doble, 1x Croissant"
data class PedidoConDetalles(
    @Embedded
    val pedido: Pedido,

    @Relation(
        entity = DetallePedido::class, // obligatorio cuando la relación es anidada
        parentColumn = "id",
        entityColumn = "pedido_id"
    )
    val detalles: List<DetalleConProducto>
) {
    // Texto listo para la tarjeta de "Pedidos recientes"
    fun resumen(): String =
        detalles.joinToString(", ") { "${it.detalle.cantidad}x ${it.producto.nombre}" }
}

// ---------- Reportes ----------

// Gráfica "Ventas por hora": hora = 8, 9, 10...
data class VentaPorHora(
    val hora: Int,
    val totalCentavos: Long
)

// Reporte "Ventas por producto"
data class VentaPorProducto(
    val productoId: Long,
    val nombre: String,
    val cantidad: Int,
    val totalCentavos: Long
)

// Reporte "Ventas por plataforma"
data class VentaPorOrigen(
    val origen: OrigenPedido,
    val pedidos: Int,
    val totalCentavos: Long
)
