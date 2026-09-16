package com.example.cafe_flow_v3.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categorias",
    indices = [Index(value = ["nombre"], unique = true)]
)
data class Categoria(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val nombre: String // "Bebidas calientes", "Postres", etc.
)
