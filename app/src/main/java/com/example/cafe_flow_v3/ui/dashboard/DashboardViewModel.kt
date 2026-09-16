package com.example.cafe_flow_v3.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafe_flow_v3.data.entity.EstadoPedido
import com.example.cafe_flow_v3.data.entity.OrigenPedido
import com.example.cafe_flow_v3.data.model.PedidoConDetalles
import com.example.cafe_flow_v3.data.model.VentaPorHora
import com.example.cafe_flow_v3.data.repository.AuthRepository
import com.example.cafe_flow_v3.data.repository.ReporteRepository
import com.example.cafe_flow_v3.data.repository.ResumenDashboard
import com.example.cafe_flow_v3.util.Dinero
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

// ---------- Lo que la pantalla necesita, ya listo para mostrarse ----------

data class DashboardUiState(
    val cargando: Boolean = true,
    val saludo: String = "",
    val nombreUsuario: String = "",
    val ventasHoy: String = "$0",
    val variacion: String? = null,         // "+18%", "-5%" o null si ayer no hubo ventas
    val variacionPositiva: Boolean = true, // para pintarla verde o roja
    val pendientes: Int = 0,
    val completados: Int = 0,
    val externos: Int = 0,
    val barras: List<BarraHora> = emptyList(),
    val recientes: List<PedidoResumenUi> = emptyList()
)

// Una barra de la gráfica "Ventas por hora"
data class BarraHora(
    val etiqueta: String,   // "8h"
    val total: String,      // "$145.00"
    val altura: Float,      // 0.0 a 1.0 (proporción respecto a la barra más alta)
    val destacada: Boolean  // terracota si es de las horas fuertes
)

// Un renglón de "Pedidos recientes"
data class PedidoResumenUi(
    val id: Long,
    val folio: String,       // "#16"
    val descripcion: String, // "1x Espresso Doble, 1x Croissant"
    val total: String,       // "$180.00"
    val estado: EstadoPedido,
    val origen: OrigenPedido
) {
    val esExterno: Boolean get() = origen != OrigenPedido.MOSTRADOR
}

// ---------- ViewModel ----------

class DashboardViewModel(
    reporteRepository: ReporteRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    /**
     * Junta el resumen de la base + el usuario que inició sesión.
     * stateIn lo convierte en StateFlow para que Compose lo pueda leer.
     * WhileSubscribed(5_000): si sales de la pantalla, deja de consultar
     * a los 5 segundos (pero sobrevive a una rotación).
     */
    val uiState: StateFlow<DashboardUiState> =
        combine(
            reporteRepository.resumenDelDia(),
            authRepository.usuarioActual
        ) { resumen, usuario ->
            resumen.aUiState(nombreUsuario = usuario?.nombre.orEmpty())
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState()
        )

    fun cerrarSesion() {
        authRepository.cerrarSesion()
    }
}

// ---------- Conversión: datos de la base -> datos de pantalla ----------

private const val ALTURA_MINIMA = 0.04f     // para que una hora sin ventas se vea como una rayita
private const val UMBRAL_DESTACADA = 0.4f   // >= 40% de la hora más fuerte = terracota
private val HORAS_GRAFICA = listOf(8, 10, 12, 14, 16, 18)

private fun ResumenDashboard.aUiState(nombreUsuario: String) = DashboardUiState(
    cargando = false,
    saludo = saludoSegunHora(),
    nombreUsuario = nombreUsuario,
    ventasHoy = Dinero.formatear(ventasHoyCentavos, mostrarCentavos = false),
    variacion = variacionVsAyer?.let { if (it >= 0) "+$it%" else "$it%" },
    variacionPositiva = (variacionVsAyer ?: 0) >= 0,
    pendientes = pendientes,
    completados = completadosHoy,
    externos = externosHoy,
    barras = construirBarras(ventasPorHora),
    recientes = recientes.map { it.aResumenUi() }
)

// Agrupa las ventas en bloques de 2 horas: 8h = 8:00-9:59, 10h = 10:00-11:59...
private fun construirBarras(ventas: List<VentaPorHora>): List<BarraHora> {
    val totales = HORAS_GRAFICA.map { inicioBloque ->
        ventas
            .filter { bloqueDe(it.hora) == inicioBloque }
            .sumOf { it.totalCentavos }
    }
    val maximo = totales.maxOrNull() ?: 0L

    return HORAS_GRAFICA.zip(totales) { hora, total ->
        val proporcion = if (maximo == 0L) 0f else total.toFloat() / maximo
        BarraHora(
            etiqueta = "${hora}h",
            total = Dinero.formatear(total),
            altura = proporcion.coerceAtLeast(ALTURA_MINIMA),
            destacada = proporcion >= UMBRAL_DESTACADA
        )
    }
}

// 9 -> 8, 13 -> 12, 7 -> 8 (antes de abrir), 22 -> 18 (después de cerrar)
private fun bloqueDe(hora: Int): Int = (hora.coerceIn(8, 19) / 2) * 2

private fun saludoSegunHora(): String =
    when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Buenos días"
        in 12..18 -> "Buenas tardes"
        else -> "Buenas noches"
    }

private fun PedidoConDetalles.aResumenUi() = PedidoResumenUi(
    id = pedido.id,
    folio = "#${pedido.id}",
    descripcion = resumen(),
    total = Dinero.formatear(pedido.totalCentavos),
    estado = pedido.estado,
    origen = pedido.origen
)