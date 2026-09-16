package com.example.cafe_flow_v3.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "usuarios",
    indices = [Index(value = ["email"], unique = true)] // no puede haber 2 cuentas con el mismo correo
)
data class Usuario(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val nombre: String,

    val email: String,

    @ColumnInfo(name = "password_hash")
    val passwordHash: String,

    val rol: Rol,

    val activo: Boolean = true
)
