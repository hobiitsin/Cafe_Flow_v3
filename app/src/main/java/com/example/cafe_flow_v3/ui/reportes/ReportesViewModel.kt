package com.example.cafe_flow_v3.ui.reportes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafe_flow_v3.data.model.VentaPorHora
import com.example.cafe_flow_v3.data.repository.ReporteRepository
import com.example.cafe_flow_v3.ui.common.etiqueta
import com.example.cafe_flow_v3.util.Dinero
import com.example.cafe_flow_v3.util.Fechas
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Modelos para la pantalla

enum class PeriodoReporte { DIA, SEMANA }

data class BarraReporte(
    val etiqueta: String,  // "10h" o "Lun"
    val valor: String,     // "$345"
    val altura: Float,     // 0.0 a 1.0
    val destacada: Boolean
)

// Un renglon de "Por plataforma" o "Mas vendidos"
data class FilaReporte(
    val titulo: String,
    val detalle: String,   // "3 pedidos" / "12 vendidos"
    val total: String,     // "$1,250.00"
    val porcentaje: Int,   // % del total del periodo
    val proporcion: Float  // para la barrita (0.0 a 1.0)
)

data class ReportesUiState(
    val cargando: Boolean = true,
    val periodo: PeriodoReporte = PeriodoReporte.DIA,
    val diasAtras: Int = 0,
    val etiquetaPeriodo: String = "Hoy",
    val ventasTotales: String = "$0",
    val pedidos: Int = 0,
    val ticketPromedio: String = "$0",
    val tituloGrafica: String = "Ventas por hora",
    val grafica: List<BarraReporte> = emptyList(),
    val plataformas: List<FilaReporte> = emptyList(),
    val productos: List<FilaReporte> = emptyList()
) {
    val puedeAvanzar: Boolean get() = periodo == PeriodoReporte.DIA && diasAtras > 0
}

private data class Seleccion(
    val periodo: PeriodoReporte,
    val diasAtras: Int
)

private const val DIAS_MAXIMOS_ATRAS = 365
private const val MAX_PRODUCTOS = 10
private const val ALTURA_MINIMA = 0.04f
private const val UMBRAL_DESTACADA = 0.4f
private val HORAS_GRAFICA = listOf(8, 10, 12, 14, 16, 18)
private val MEXICO: Locale = Locale.Builder().setLanguage("es").setRegion("MX").build()

// ViewModel

class ReportesViewModel(
    private val reporteRepository: ReporteRepository
) : ViewModel() {

    private val seleccion = MutableStateFlow(Seleccion(PeriodoReporte.DIA, diasAtras = 0))

    // Cada vez que cambias de día o de periodo se arman consultas nuevas
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ReportesUiState> =
        seleccion
            .flatMapLatest { reporteDe(it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ReportesUiState()
            )

    // Acciones

    fun seleccionarPeriodo(periodo: PeriodoReporte) {
        seleccion.update { Seleccion(periodo, diasAtras = 0) }
    }

    fun diaAnterior() {
        seleccion.update { it.copy(diasAtras = (it.diasAtras + 1).coerceAtMost(DIAS_MAXIMOS_ATRAS)) }
    }

    fun diaSiguiente() {
        seleccion.update { it.copy(diasAtras = (it.diasAtras - 1).coerceAtLeast(0)) }
    }

    // Armado del reporte

    private fun reporteDe(sel: Seleccion): Flow<ReportesUiState> {
        // Rango del periodo
        val (inicio, fin) = when (sel.periodo) {
            PeriodoReporte.DIA -> Fechas.inicioDelDia(sel.diasAtras) to Fechas.finDelDia(sel.diasAtras)
            PeriodoReporte.SEMANA -> Fechas.inicioDelDia(6) to Fechas.finDelDia(0)
        }

        // La grafica cambia según el periodo
        val grafica: Flow<List<BarraReporte>> = when (sel.periodo) {
            PeriodoReporte.DIA ->
                reporteRepository.ventasPorHoraEntre(inicio, fin).map { barrasPorHora(it) }

            // 7 consultas, una por día (de hace 6 días hasta hoy)
            PeriodoReporte.SEMANA ->
                combine((6 downTo 0).map { d ->
                    reporteRepository.ventasEntre(Fechas.inicioDelDia(d), Fechas.finDelDia(d))
                }) { totales -> barrasPorDia(totales.toList()) }
        }

        return combine(
            reporteRepository.ventasEntre(inicio, fin),
            reporteRepository.pedidosCompletadosEntre(inicio, fin),
            reporteRepository.ventasPorProductoEntre(inicio, fin),
            reporteRepository.ventasPorPlataformaEntre(inicio, fin),
            grafica
        ) { ventas, pedidos, porProducto, porPlataforma, barras ->
            ReportesUiState(
                cargando = false,
                periodo = sel.periodo,
                diasAtras = sel.diasAtras,
                etiquetaPeriodo = etiquetaPeriodo(sel),
                ventasTotales = Dinero.formatear(ventas, mostrarCentavos = false),
                pedidos = pedidos,
                ticketPromedio = if (pedidos == 0) "$0" else Dinero.formatear(ventas / pedidos),
                tituloGrafica = if (sel.periodo == PeriodoReporte.DIA) "Ventas por hora" else "Ventas por día",
                grafica = barras,
                plataformas = porPlataforma.map {
                    FilaReporte(
                        titulo = it.origen.etiqueta(),
                        detalle = if (it.pedidos == 1) "1 pedido" else "${it.pedidos} pedidos",
                        total = Dinero.formatear(it.totalCentavos),
                        porcentaje = porcentaje(it.totalCentavos, ventas),
                        proporcion = proporcion(it.totalCentavos, ventas)
                    )
                },
                productos = porProducto.take(MAX_PRODUCTOS).map {
                    FilaReporte(
                        titulo = it.nombre,
                        detalle = if (it.cantidad == 1) "1 vendido" else "${it.cantidad} vendidos",
                        total = Dinero.formatear(it.totalCentavos),
                        porcentaje = porcentaje(it.totalCentavos, ventas),
                        proporcion = proporcion(it.totalCentavos, ventas)
                    )
                }
            )
        }
    }
}

// Utilidades

private fun porcentaje(parte: Long, total: Long): Int =
    if (total == 0L) 0 else (parte * 100 / total).toInt()

private fun proporcion(parte: Long, total: Long): Float =
    if (total == 0L) 0f else (parte.toFloat() / total).coerceIn(0f, 1f)

// Bloques de 2 horas
private fun barrasPorHora(ventas: List<VentaPorHora>): List<BarraReporte> {
    val totales = HORAS_GRAFICA.map { inicioBloque ->
        ventas.filter { bloqueDe(it.hora) == inicioBloque }.sumOf { it.totalCentavos }
    }
    return construirBarras(HORAS_GRAFICA.map { "${it}h" }, totales)
}

private fun bloqueDe(hora: Int): Int = (hora.coerceIn(8, 19) / 2) * 2

private fun barrasPorDia(totales: List<Long>): List<BarraReporte> {
    val etiquetas = (6 downTo 0).map { d ->
        if (d == 0) "Hoy"
        else SimpleDateFormat("EEE", MEXICO)
            .format(Date(Fechas.inicioDelDia(d)))
            .replace(".", "")
            .replaceFirstChar { it.uppercase() }
    }
    return construirBarras(etiquetas, totales)
}

private fun construirBarras(etiquetas: List<String>, totales: List<Long>): List<BarraReporte> {
    val maximo = totales.maxOrNull() ?: 0L
    return etiquetas.zip(totales) { etiqueta, total ->
        val p = if (maximo == 0L) 0f else total.toFloat() / maximo
        BarraReporte(
            etiqueta = etiqueta,
            valor = if (total == 0L) "—" else Dinero.formatear(total, mostrarCentavos = false),
            altura = p.coerceAtLeast(ALTURA_MINIMA),
            destacada = p >= UMBRAL_DESTACADA
        )
    }
}

private fun etiquetaPeriodo(sel: Seleccion): String = when {
    sel.periodo == PeriodoReporte.SEMANA -> "Últimos 7 días"
    sel.diasAtras == 0 -> "Hoy"
    sel.diasAtras == 1 -> "Ayer"
    // "Lunes 21 de septiembre"
    else -> SimpleDateFormat("EEEE d 'de' MMMM", MEXICO)
        .format(Date(Fechas.inicioDelDia(sel.diasAtras)))
        .replaceFirstChar { it.uppercase() }
}