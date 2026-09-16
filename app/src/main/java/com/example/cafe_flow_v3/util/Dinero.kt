package com.example.cafe_flow_v3.util


import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * Conversión entre centavos (lo que guarda la base) y texto (lo que ve el usuario).
 */
object Dinero {

    private val MEXICO: Locale = Locale.Builder().setLanguage("es").setRegion("MX").build()

    // 1245000 -> "$12,450.00"   |   sin centavos -> "$12,450"
    fun formatear(centavos: Long, mostrarCentavos: Boolean = true): String {
        val formato = NumberFormat.getCurrencyInstance(MEXICO).apply {
            if (!mostrarCentavos) {
                minimumFractionDigits = 0
                maximumFractionDigits = 0
            }
        }
        return formato.format(BigDecimal.valueOf(centavos, 2))
    }

    // "45.5" -> 4550   |   "$1,200" -> 120000   |   "abc" -> null
    fun aCentavos(texto: String): Long? {
        val limpio = texto.replace("$", "").replace(",", "").trim()
        if (limpio.isEmpty()) return null

        return try {
            val valor = BigDecimal(limpio)
            if (valor.signum() < 0) {
                null
            } else {
                valor.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
            }
        } catch (e: NumberFormatException) {
            null
        } catch (e: ArithmeticException) {
            null
        }
    }
}
