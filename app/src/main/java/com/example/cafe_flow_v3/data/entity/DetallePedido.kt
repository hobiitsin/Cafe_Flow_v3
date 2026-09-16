package com.example.cafe_flow_v3.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "detalle_pedido",
    foreignKeys = [
        ForeignKey(
            entity = Pedido::class,
            parentColumns = ["id"],
            childColumns = ["pedido_id"],
            onDelete = ForeignKey.CASCADE // si se borra el pedido, se borran sus renglones
        ),
        ForeignKey(
            entity = Producto::class,
            parentColumns = ["id"],
            childColumns = ["producto_id"],
            onDelete = ForeignKey.RESTRICT // no deja borrar un producto que ya se vendió
        )
    ],
    indices = [Index("pedido_id"), Index("producto_id")]
)
data class DetallePedido(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "pedido_id")
    val pedidoId: Long,

    @ColumnInfo(name = "producto_id")
    val productoId: Long,

    val cantidad: Int,

    // Copia del precio al momento de la venta. Si luego cambia el precio, este no se mueve.
    @ColumnInfo(name = "precio_unitario_centavos")
    val precioUnitarioCentavos: Long,

    // Personalización: "leche de almendra, sin azúcar"
    val notas: String? = null
)
