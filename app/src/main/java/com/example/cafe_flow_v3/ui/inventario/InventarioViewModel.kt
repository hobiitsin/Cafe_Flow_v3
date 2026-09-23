package com.example.cafe_flow_v3.ui.inventario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafe_flow_v3.data.entity.Producto
import com.example.cafe_flow_v3.data.repository.ProductoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Modelo para la pantalla

enum class FiltroInventario { TODOS, BAJO, AGOTADOS }

enum class NivelStock { AGOTADO, BAJO, OK }

data class ProductoInventarioUi(
    val producto: Producto,
    val categoria: String
) {

    val nivel: NivelStock
        get() = when {
            producto.stock <= 0 -> NivelStock.AGOTADO
            producto.stock <= producto.stockMinimo -> NivelStock.BAJO
            else -> NivelStock.OK
        }

    // Barra stock 100%
    val proporcion: Float
        get() {
            val referencia = (producto.stockMinimo * 2).coerceAtLeast(1)
            return (producto.stock.toFloat() / referencia).coerceIn(0f, 1f)
        }
}

data class MensajeInventario(
    val texto: String,
    val esError: Boolean
)

data class InventarioUiState(
    val cargando: Boolean = true,
    val busqueda: String = "",
    val filtro: FiltroInventario = FiltroInventario.TODOS,
    // Tarjetas de resumen
    val totalProductos: Int = 0,
    val totalBajo: Int = 0,
    val totalAgotados: Int = 0,
    // Lista filtrada, ordenada
    val productos: List<ProductoInventarioUi> = emptyList(),
    // Mensaj de reabastecer
    val productoAReabastecer: Producto? = null,
    val cantidad: String = "",
    val errorCantidad: String? = null,
    val procesando: Boolean = false,
    val mensaje: MensajeInventario? = null
) {
    // Vista previa en el mensaje stock ejemplo: ("Quedará en 13")
    val stockResultante: Int?
        get() {
            val actual = productoAReabastecer?.stock ?: return null
            val agregar = cantidad.toIntOrNull() ?: return null
            return actual + agregar
        }
}

private const val CANTIDAD_MAXIMA = 10_000

// ViewModel

class InventarioViewModel(
    private val productoRepository: ProductoRepository
) : ViewModel() {

    private val _estado = MutableStateFlow(InventarioUiState())

    val uiState: StateFlow<InventarioUiState> =
        combine(
            _estado,
            productoRepository.observarProductos(),
            productoRepository.observarCategorias()
        ) { estado, productos, categorias ->
            val nombres = categorias.associate { it.id to it.nombre }
            val todos = productos.map { ProductoInventarioUi(it, nombres[it.categoriaId].orEmpty()) }

            val texto = estado.busqueda.trim()
            val visibles = todos
                .filter {
                    when (estado.filtro) {
                        FiltroInventario.TODOS -> true
                        FiltroInventario.BAJO -> it.nivel == NivelStock.BAJO
                        FiltroInventario.AGOTADOS -> it.nivel == NivelStock.AGOTADO
                    }
                }
                .filter { texto.isEmpty() || it.producto.nombre.contains(texto, ignoreCase = true) }
                // Primero lo urgente: agotados, luego bajos, luego el resto; dentro de cada grupo por nombre
                .sortedWith(compareBy({ it.nivel.ordinal }, { it.producto.nombre }))

            estado.copy(
                cargando = false,
                totalProductos = todos.size,
                totalBajo = todos.count { it.nivel == NivelStock.BAJO },
                totalAgotados = todos.count { it.nivel == NivelStock.AGOTADO },
                productos = visibles,
                // Si el producto del diálogo cambió en la base, mostramos su stock actualizado
                productoAReabastecer = estado.productoAReabastecer?.let { abierto ->
                    productos.find { it.id == abierto.id } ?: abierto
                }
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = InventarioUiState()
        )

    // Filtros

    fun onBusquedaChange(valor: String) {
        _estado.update { it.copy(busqueda = valor) }
    }

    fun seleccionarFiltro(filtro: FiltroInventario) {
        _estado.update { it.copy(filtro = filtro) }
    }

    // Reabastecer

    fun abrirReabastecer(producto: Producto) {
        _estado.update {
            it.copy(productoAReabastecer = producto, cantidad = "", errorCantidad = null, mensaje = null)
        }
    }

    fun cerrarReabastecer() {
        _estado.update { it.copy(productoAReabastecer = null, cantidad = "", errorCantidad = null) }
    }

    fun onCantidadChange(valor: String) {
        val limpio = valor.filter { it.isDigit() }.take(5)
        _estado.update { it.copy(cantidad = limpio, errorCantidad = null) }
    }

    // Botones +5, +10, +20, ya se suma a lo que ya esta
    fun sumarRapido(cantidad: Int) {
        _estado.update {
            val actual = it.cantidad.toIntOrNull() ?: 0
            it.copy(cantidad = (actual + cantidad).coerceAtMost(CANTIDAD_MAXIMA).toString(), errorCantidad = null)
        }
    }

    fun confirmarReabastecer() {
        val estado = _estado.value
        if (estado.procesando) return
        val producto = estado.productoAReabastecer ?: return
        val cantidad = estado.cantidad.toIntOrNull()

        val error = when {
            cantidad == null || cantidad <= 0 -> "Escribe una cantidad mayor a 0"
            cantidad > CANTIDAD_MAXIMA -> "La cantidad máxima es $CANTIDAD_MAXIMA"
            else -> null
        }
        if (error != null || cantidad == null) {
            _estado.update { it.copy(errorCantidad = error) }
            return
        }

        _estado.update { it.copy(procesando = true) }

        viewModelScope.launch {
            val resultado = productoRepository.reabastecer(producto.id, cantidad)
            _estado.update {
                resultado.fold(
                    onSuccess = { _ ->
                        it.copy(
                            procesando = false,
                            productoAReabastecer = null,
                            cantidad = "",
                            mensaje = MensajeInventario(
                                "Se agregaron $cantidad a ${producto.nombre} (ahora hay ${producto.stock + cantidad})",
                                esError = false
                            )
                        )
                    },
                    onFailure = { e ->
                        it.copy(procesando = false, errorCantidad = e.message ?: "No se pudo actualizar el stock")
                    }
                )
            }
        }
    }

    fun cerrarMensaje() {
        _estado.update { it.copy(mensaje = null) }
    }
}