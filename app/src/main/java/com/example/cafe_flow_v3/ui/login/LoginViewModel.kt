package com.example.cafe_flow_v3.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafe_flow_v3.data.repository.AuthRepository
import com.example.cafe_flow_v3.data.repository.ResultadoAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Todo lo que la pantalla de Login necesita para dibujarse
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val cargando: Boolean = false,
    val error: String? = null,
    val loginExitoso: Boolean = false // la pantalla lo lee para navegar al dashboard
)

/**
 * El ViewModel sobrevive a rotaciones de pantalla: lo que escribiste no se borra.
 * La pantalla solo LEE uiState y LLAMA funciones; no toca la base directamente.
 */
class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // ---------- Eventos que manda la pantalla ----------

    fun onEmailChange(valor: String) {
        _uiState.update { it.copy(email = valor, error = null) } // al escribir se quita el error
    }

    fun onPasswordChange(valor: String) {
        _uiState.update { it.copy(password = valor, error = null) }
    }

    fun onTogglePasswordVisible() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun iniciarSesion() {
        val estado = _uiState.value
        if (estado.cargando) return // evita doble clic

        _uiState.update { it.copy(cargando = true, error = null) }

        viewModelScope.launch {
            when (val resultado = authRepository.iniciarSesion(estado.email, estado.password)) {
                is ResultadoAuth.Exito -> _uiState.update {
                    it.copy(cargando = false, loginExitoso = true, password = "")
                }
                is ResultadoAuth.Error -> _uiState.update {
                    it.copy(cargando = false, error = resultado.mensaje)
                }
            }
        }
    }

    // La pantalla lo llama después de navegar, para no navegar dos veces
    fun onNavegacionHecha() {
        _uiState.update { it.copy(loginExitoso = false) }
    }
}
