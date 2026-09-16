package com.example.cafe_flow_v3.data.repository

internal fun fallo(mensaje: String): Result<Nothing> =
    Result.failure(IllegalStateException(mensaje))
