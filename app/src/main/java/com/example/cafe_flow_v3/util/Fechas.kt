package com.example.cafe_flow_v3.util

import java.util.Calendar

/**
 * Rangos de tiempo en milisegundos, con la zona horaria del teléfono.
 * Usamos Calendar (y no java.time) porque java.time requiere Android 8
 * y tu minSdk es 24.
 */
object Fechas {

    // 00:00:00.000 de hoy (o de hace N días)
    fun inicioDelDia(diasAtras: Int = 0): Long =
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -diasAtras)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    // 23:59:59.999 de hoy (o de hace N días)
    fun finDelDia(diasAtras: Int = 0): Long =
        Calendar.getInstance().apply {
            timeInMillis = inicioDelDia(diasAtras)
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis - 1

    // Una hora concreta: aLasHoras(13, 20) = hoy a la 1:20 pm
    fun aLasHoras(hora: Int, minuto: Int = 0, diasAtras: Int = 0): Long =
        Calendar.getInstance().apply {
            timeInMillis = inicioDelDia(diasAtras)
            set(Calendar.HOUR_OF_DAY, hora)
            set(Calendar.MINUTE, minuto)
        }.timeInMillis
}
