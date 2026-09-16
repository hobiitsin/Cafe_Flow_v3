package com.example.cafe_flow_v3.util

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Hashea contraseñas con PBKDF2 + sal aleatoria.
 *
 * Lo que se guarda en la columna password_hash tiene la forma:
 *   "iteraciones:sal:hash"   (sal y hash en Base64)
 *
 * Así nadie puede leer la contraseña aunque abra la base de datos.
 */
object SeguridadPassword {

    // SHA1 porque la variante SHA256 solo existe desde Android 8 y tu minSdk es 24.
    // En PBKDF2, SHA1 sigue siendo seguro.
    private const val ALGORITMO = "PBKDF2WithHmacSHA1"
    private const val ITERACIONES = 100_000   // más = más lento para un atacante (y para el login)
    private const val LONGITUD_BITS = 256
    private const val LONGITUD_SAL = 16

    fun hashear(password: String): String {
        val sal = ByteArray(LONGITUD_SAL).also { SecureRandom().nextBytes(it) }
        val hash = derivar(password, sal, ITERACIONES)
        return "$ITERACIONES:${sal.aBase64()}:${hash.aBase64()}"
    }

    fun verificar(password: String, guardado: String): Boolean {
        val partes = guardado.split(":")
        if (partes.size != 3) return false

        return try {
            val iteraciones = partes[0].toInt()
            val sal = Base64.decode(partes[1], Base64.NO_WRAP)
            val esperado = Base64.decode(partes[2], Base64.NO_WRAP)
            val calculado = derivar(password, sal, iteraciones)
            // Comparación en tiempo constante (no revela cuántos bytes coincidieron)
            MessageDigest.isEqual(esperado, calculado)
        } catch (e: IllegalArgumentException) {
            false // el texto guardado estaba mal formado
        }
    }

    private fun derivar(password: String, sal: ByteArray, iteraciones: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), sal, iteraciones, LONGITUD_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun ByteArray.aBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
}
