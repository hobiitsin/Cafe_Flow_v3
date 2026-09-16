package com.example.cafe_flow_v3.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pedidos",
    foreignKeys = [
        ForeignKey(
            entity = Usuario::class,
            parentColumns = ["id"],
            childColumns = ["usuario_id"],
            onDelete = ForeignKey.SET_NULL // si se borra el empleado, el pedido se conserva
        )
    ],
    // Indices en las columnas que más vamos a filtrar en el dashboard
    indices = [Index("usuario_id"), Index("fecha_hora"), Index("estado")]
)
data class Pedido(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "usuario_id")
    val usuarioId: Long?,

    @ColumnInfo(name = "fecha_hora")
    val fechaHora: Long = System.currentTimeMillis(),

    val estado: EstadoPedido = EstadoPedido.PENDIENTE,

    val origen: OrigenPedido = OrigenPedido.MOSTRADOR,

    // Numero de orden de DiDi / Uber / Rappi devolvera valor nulo si no es de aplicacion
    @ColumnInfo(name = "folio_externo")
    val folioExterno: String? = null,

    @ColumnInfo(name = "metodo_pago")
    val metodoPago: MetodoPago,

    @ColumnInfo(name = "total_centavos")
    val totalCentavos: Long
)
