package com.example.cafe_flow_v3.ui.pedidos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafe_flow_v3.data.entity.EstadoPedido
import com.example.cafe_flow_v3.data.entity.MetodoPago
import com.example.cafe_flow_v3.data.entity.OrigenPedido
import com.example.cafe_flow_v3.data.model.PedidoConDetalles
import com.example.cafe_flow_v3.data.repository.PedidoRepository
import com.example.cafe_flow_v3.data.repository.esFinal
import com.example.cafe_flow_v3.data.repository.siguiente
import com.example.cafe_flow_v3.ui.common.etiqueta
import com.example.cafe_flow_v3.util.Dinero
import com.example.cafe_flow_v3.util.Fechas
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------- Modelos para la pantalla ----------

data class LineaPedidoUi(
    val cantidad: Int,
    val nombre: String,
    val notas: String?
)

data class PedidoUi(
    val id: Long,
    val folio: String,          // "#16"
    val hora: String,           // "17:00" (o "15/09 17:00" si no es de hoy)
    val espera: String?,        // "Hace 12 min" (solo pedidos activos)
    val esperaLarga: Boolean,   // lleva mucho esperando -> se pinta en naranja
    val estado: EstadoPedido,
    val origen: OrigenPedido,
    val folioExterno: String?,
    val metodoPago: MetodoPago,
    val total: String,
    val lineas: List<LineaPedidoUi>
)

data class MensajePedidos(
    val texto: String,
    val esError: Boolean
)

data class PedidosUiState(
    val cargando: Boolean = true,
    val estadoSeleccionado: EstadoPedido = EstadoPedido.PENDIENTE,
    val origenFiltro: OrigenPedido? = null,          // null = todos
    val conteos: Map<EstadoPedido, Int> = emptyMap(), // para "Pendientes 3"
    val pedidos: List<PedidoUi> = emptyList(),
    val procesandoId: Long? = null,                  // pedido que se está actualizando
    val mensaje: MensajePedidos? = null,
    val pedidoPorCancelar: PedidoUi? = null          // abre el diálogo de confirmación
)

// A partir de estos minutos, un pedido activo se marca como "espera larga"
private const val MINUTOS_ESPERA_LARGA = 15L

// ---------- ViewModel ----------

class PedidosViewModel(
    private val pedidoRepository: PedidoRepository
) : ViewModel() {

    private val _estado = MutableStateFlow(PedidosUiState())

    // Escuchamos los 5 estados a la vez: así tenemos las listas Y los conteos de cada pestaña
    private val pedidosPorEstado: Flow<Map<EstadoPedido, List<PedidoConDetalles>>> =
        combine(EstadoPedido.entries.map { pedidoRepository.observarPorEstado(it) }) { listas ->
            EstadoPedido.entries.zip(listas).toMap()
        }

    // "Reloj" que avisa cada 30 segundos para actualizar el "Hace X min"
    private val reloj: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(30_000)
        }
    }

    val uiState: StateFlow<PedidosUiState> =
        combine(_estado, pedidosPorEstado, reloj) { estado, porEstado, ahora ->
            // Filtro por plataforma (Mostrador, DiDi, Uber, Rappi)
            val filtrados = porEstado.mapValues { (_, lista) ->
                lista.filter { estado.origenFiltro == null || it.pedido.origen == estado.origenFiltro }
            }

            // Activos: el más antiguo arriba (es el más urgente).
            // Completados/cancelados: el más reciente arriba.
            val lista = filtrados[estado.estadoSeleccionado].orEmpty().let {
                if (estado.estadoSeleccionado.esFinal()) it.reversed() else it
            }

            estado.copy(
                cargando = false,
                conteos = filtrados.mapValues { it.value.size },
                pedidos = lista.map { it.aUi(ahora) }
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PedidosUiState()
        )

    // ---------- Filtros ----------

    fun seleccionarEstado(estado: EstadoPedido) {
        _estado.update { it.copy(estadoSeleccionado = estado) }
    }

    fun filtrarOrigen(origen: OrigenPedido?) {
        _estado.update { it.copy(origenFiltro = origen) }
    }

    // ---------- Acciones ----------

    fun avanzar(pedido: PedidoUi) {
        if (_estado.value.procesandoId != null) return
        val siguiente = pedido.estado.siguiente() ?: return

        _estado.update { it.copy(procesandoId = pedido.id, mensaje = null) }

        viewModelScope.launch {
            val resultado = pedidoRepository.avanzar(pedido.id, pedido.estado)
            _estado.update {
                it.copy(
                    procesandoId = null,
                    mensaje = resultado.fold(
                        onSuccess = { MensajePedidos("Pedido ${pedido.folio} ahora está: ${siguiente.etiqueta()}", esError = false) },
                        onFailure = { e -> MensajePedidos(e.message ?: "No se pudo actualizar el pedido", esError = true) }
                    )
                )
            }
        }
    }

    fun pedirCancelacion(pedido: PedidoUi) {
        _estado.update { it.copy(pedidoPorCancelar = pedido) }
    }

    fun descartarCancelacion() {
        _estado.update { it.copy(pedidoPorCancelar = null) }
    }

    fun confirmarCancelacion() {
        val pedido = _estado.value.pedidoPorCancelar ?: return

        _estado.update { it.copy(pedidoPorCancelar = null, procesandoId = pedido.id, mensaje = null) }

        viewModelScope.launch {
            val resultado = pedidoRepository.cancelar(pedido.id)
            _estado.update {
                it.copy(
                    procesandoId = null,
                    mensaje = resultado.fold(
                        onSuccess = { MensajePedidos("Pedido ${pedido.folio} cancelado. Se regresó el inventario.", esError = false) },
                        onFailure = { e -> MensajePedidos(e.message ?: "No se pudo cancelar el pedido", esError = true) }
                    )
                )
            }
        }
    }

    fun cerrarMensaje() {
        _estado.update { it.copy(mensaje = null) }
    }
}

// ---------- Conversión: base de datos -> pantalla ----------

private fun PedidoConDetalles.aUi(ahora: Long): PedidoUi {
    val activo = !pedido.estado.esFinal()
    val minutos = (ahora - pedido.fechaHora) / 60_000

    return PedidoUi(
        id = pedido.id,
        folio = "#${pedido.id}",
        hora = formatearHora(pedido.fechaHora),
        // Los pedidos "del futuro" de los datos de prueba no muestran espera
        espera = if (activo && minutos >= 0) textoEspera(minutos) else null,
        esperaLarga = activo && minutos >= MINUTOS_ESPERA_LARGA,
        estado = pedido.estado,
        origen = pedido.origen,
        folioExterno = pedido.folioExterno,
        metodoPago = pedido.metodoPago,
        total = Dinero.formatear(pedido.totalCentavos),
        lineas = detalles.map {
            LineaPedidoUi(
                cantidad = it.detalle.cantidad,
                nombre = it.producto.nombre,
                notas = it.detalle.notas
            )
        }
    )
}

private fun textoEspera(minutos: Long): String = when {
    minutos < 1 -> "Justo ahora"
    minutos < 60 -> "Hace $minutos min"
    else -> "Hace ${minutos / 60} h ${minutos % 60} min"
}

private fun formatearHora(millis: Long): String {
    val patron = if (millis >= Fechas.inicioDelDia()) "HH:mm" else "dd/MM HH:mm"
    return SimpleDateFormat(patron, Locale.getDefault()).format(Date(millis))
}
