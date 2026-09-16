package com.example.cafe_flow_v3.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "productos",
    foreignKeys = [
        ForeignKey(
            entity = Categoria::class,
            parentColumns = ["id"],
            childColumns = ["categoria_id"],
            onDelete = ForeignKey.RESTRICT // no deja borrar una categoría que todavía tiene productos
        )
    ],
    indices = [Index("categoria_id")]
)
data class Producto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "categoria_id")
    val categoriaId: Long,

    val nombre: String,

    // $45.50 se guarda como 4550
    @ColumnInfo(name = "precio_centavos")
    val precioCentavos: Long,

    val stock: Int = 0,

    // Cuando stock <= stockMinimo, se muestra la alerta de bajo inventario
    @ColumnInfo(name = "stock_minimo")
    val stockMinimo: Int = 5,

    // Para ocultar un producto del menu sin borrarlo
    val disponible: Boolean = true
)