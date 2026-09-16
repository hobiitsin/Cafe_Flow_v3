package com.example.cafe_flow_v3.data.repository

import android.database.sqlite.SQLiteConstraintException
import android.util.Patterns
import com.example.cafe_flow_v3.data.dao.UsuarioDao
import com.example.cafe_flow_v3.data.entity.Rol
import com.example.cafe_flow_v3.data.entity.Usuario
import com.example.cafe_flow_v3.util.SeguridadPassword
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

sealed interface ResultadoAuth {
    data class Exito(val usuario: Usuario) : ResultadoAuth
    data class Error(val mensaje: String) : ResultadoAuth
}

/**
 * Login, registro y "quién está usando la app".
 * La sesión vive en memoria: al cerrar la app hay que volver a iniciar sesión.
 */
class AuthRepository(private val usuarioDao: UsuarioDao) {

    private val _usuarioActual = MutableStateFlow<Usuario?>(null)
    val usuarioActual: StateFlow<Usuario?> = _usuarioActual.asStateFlow()

    suspend fun iniciarSesion(email: String, password: String): ResultadoAuth {
        val correo = normalizarEmail(email)
        if (correo.isEmpty() || password.isEmpty()) {
            return ResultadoAuth.Error("Escribe tu correo y contraseña")
        }

        // Mismo mensaje si no existe el correo o si la contraseña está mal:
        // así no le decimos a un extraño qué correos sí están registrados
        val usuario = usuarioDao.buscarPorEmail(correo)
            ?: return ResultadoAuth.Error(CREDENCIALES_INVALIDAS)

        // Verificar el hash usa mucho CPU: lo sacamos del hilo principal
        val passwordCorrecta = withContext(Dispatchers.Default) {
            SeguridadPassword.verificar(password, usuario.passwordHash)
        }
        if (!passwordCorrecta) return ResultadoAuth.Error(CREDENCIALES_INVALIDAS)

        if (!usuario.activo) {
            return ResultadoAuth.Error("Tu cuenta está desactivada. Habla con el administrador.")
        }

        _usuarioActual.value = usuario
        return ResultadoAuth.Exito(usuario)
    }

    suspend fun registrar(
        nombre: String,
        email: String,
        password: String,
        confirmacion: String,
        rol: Rol = Rol.CAJERO
    ): ResultadoAuth {
        val nombreLimpio = nombre.trim()
        val correo = normalizarEmail(email)

        when {
            nombreLimpio.length < 2 ->
                return ResultadoAuth.Error("Escribe tu nombre")
            !Patterns.EMAIL_ADDRESS.matcher(correo).matches() ->
                return ResultadoAuth.Error("El correo no es válido")
            password.length < 8 ->
                return ResultadoAuth.Error("La contraseña debe tener al menos 8 caracteres")
            password != confirmacion ->
                return ResultadoAuth.Error("Las contraseñas no coinciden")
        }

        if (usuarioDao.buscarPorEmail(correo) != null) {
            return ResultadoAuth.Error(CORREO_DUPLICADO)
        }

        val hash = withContext(Dispatchers.Default) { SeguridadPassword.hashear(password) }
        val nuevo = Usuario(nombre = nombreLimpio, email = correo, passwordHash = hash, rol = rol)

        return try {
            val id = usuarioDao.insertar(nuevo)
            ResultadoAuth.Exito(nuevo.copy(id = id))
        } catch (e: SQLiteConstraintException) {
            // Por si dos registros con el mismo correo llegan al mismo tiempo
            ResultadoAuth.Error(CORREO_DUPLICADO)
        }
    }

    fun cerrarSesion() {
        _usuarioActual.value = null
    }

    // "  Barista@CafeFlow.com " -> "barista@cafeflow.com"
    private fun normalizarEmail(email: String) = email.trim().lowercase()

    private companion object {
        const val CREDENCIALES_INVALIDAS = "Correo o contraseña incorrectos"
        const val CORREO_DUPLICADO = "Ya existe una cuenta con ese correo"
    }
}
