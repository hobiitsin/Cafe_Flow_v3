package com.example.cafe_flow_v3.ui.ventas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafe_flow_v3.data.entity.Categoria
import com.example.cafe_flow_v3.data.entity.MetodoPago
import com.example.cafe_flow_v3.data.entity.OrigenPedido
import com.example.cafe_flow_v3.data.entity.Producto
import com.example.cafe_flow_v3.data.model.ItemCarrito
import com.example.cafe_flow_v3.data.repository.AuthRepository
import com.example.cafe_flow_v3.data.repository.PedidoRepository
import com.example.cafe_flow_v3.data.repository.ProductoRepository
import com.example.cafe_flow_v3.data.repository.ResultadoVenta
import com.example.cafe_flow_v3.util.Dinero
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch



// Una tarjeta del menu
data class ProductoMenuUi(
    val producto: Producto,
    val categoria: String, // para escoger el ícono
    val enCarrito: Int     // cuántos de este producto ya van en el carrito
) {
    val agotado: Boolean get() = producto.stock <= 0
    val bajoInventario: Boolean get() = !agotado && producto.stock <= producto.stockMinimo
    val puedeAgregar: Boolean get() = enCarrito < producto.stock
}


data class VentaConfirmada(
    val pedidoId: Long,
    val total: String,
    val cambio: String?,
    val origen: OrigenPedido
)

data class VentasUiState(
    val cargando: Boolean = true,
    // Menú
    val categorias: List<Categoria> = emptyList(),
    val categoriaSeleccionada: Long? = null, // null = "Todas"
    val productos: List<ProductoMenuUi> = emptyList(),
    // Carrito y cobro
    val carrito: List<ItemCarrito> = emptyList(),
    val origen: OrigenPedido = OrigenPedido.MOSTRADOR,
    val folioExterno: String = "",
    val metodoPago: MetodoPago = MetodoPago.EFECTIVO,
    val efectivoRecibido: String = "",
    val procesando: Boolean = false,
    val error: String? = null,
    val ventaConfirmada: VentaConfirmada? = null
) {
    val totalCentavos: Long get() = carrito.sumOf { it.subtotalCentavos }
    val articulos: Int get() = carrito.sumOf { it.cantidad }
    val esExterno: Boolean get() = origen != OrigenPedido.MOSTRADOR

    // Cambio a regresar. null si no es efectivo o no escribió monto. Negativo = falta dinero.
    val cambioCentavos: Long?
        get() = if (metodoPago != MetodoPago.EFECTIVO) null
        else Dinero.aCentavos(efectivoRecibido)?.let { it - totalCentavos }

    fun cantidadEnCarrito(productoId: Long): Int =
        carrito.filter { it.producto.id == productoId }.sumOf { it.cantidad }

    fun puedeAumentar(item: ItemCarrito): Boolean =
        cantidadEnCarrito(item.producto.id) < item.producto.stock
}

// ---------- ViewModel ----------

class VentasViewModel(
    productoRepository: ProductoRepository,
    private val pedidoRepository: PedidoRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // Lo que el usuario va armando (carrito, metodo de pago, etc.)
    private val _estado = MutableStateFlow(VentasUiState())

    // Cuando cambia la categoría seleccionada, cambiamos de consulta
    @OptIn(ExperimentalCoroutinesApi::class)
    private val menu = _estado
        .map { it.categoriaSeleccionada }
        .distinctUntilChanged()
        .flatMapLatest { categoriaId -> productoRepository.observarMenu(categoriaId) }

    // Estado final = lo que armó el usuario + categorías y productos de la base
    val uiState: StateFlow<VentasUiState> =
        combine(_estado, productoRepository.observarCategorias(), menu) { estado, categorias, productos ->
            val nombres = categorias.associate { it.id to it.nombre }
            val porId = productos.associateBy { it.id }
            estado.copy(
                cargando = false,
                categorias = categorias,
                productos = productos.map {
                    ProductoMenuUi(
                        producto = it,
                        categoria = nombres[it.categoriaId].orEmpty(),
                        enCarrito = estado.cantidadEnCarrito(it.id)
                    )
                },
                // Si cambió el stock o el precio de algo que va en el carrito, lo refrescamos
                carrito = estado.carrito.map { item ->
                    porId[item.producto.id]?.let { item.copy(producto = it) } ?: item
                }
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = VentasUiState()
        )

    // ---------- Menú ----------

    fun seleccionarCategoria(categoriaId: Long?) {
        _estado.update { it.copy(categoriaSeleccionada = categoriaId) }
    }

    // ---------- Carrito ----------

    fun agregar(producto: Producto) {
        _estado.update { estado ->
            if (estado.cantidadEnCarrito(producto.id) >= producto.stock) {
                val mensaje = if (producto.stock <= 0) "${producto.nombre} está agotado"
                else "Solo quedan ${producto.stock} de ${producto.nombre}"
                return@update estado.copy(error = mensaje)
            }

            // Si ya hay un renglón de ese producto sin notas, le sumamos 1
            val index = estado.carrito.indexOfFirst {
                it.producto.id == producto.id && it.notas.isNullOrBlank()
            }
            val carrito = if (index >= 0) {
                estado.carrito.toMutableList().also {
                    it[index] = it[index].copy(producto = producto, cantidad = it[index].cantidad + 1)
                }
            } else {
                estado.carrito + ItemCarrito(producto)
            }
            estado.copy(carrito = carrito, error = null)
        }
    }

    fun cambiarCantidad(index: Int, delta: Int) {
        _estado.update { estado ->
            val item = estado.carrito.getOrNull(index) ?: return@update estado
            val nueva = item.cantidad + delta
            when {
                nueva <= 0 -> estado.copy(
                    carrito = estado.carrito.filterIndexed { i, _ -> i != index },
                    error = null
                )
                delta > 0 && !estado.puedeAumentar(item) -> estado.copy(
                    error = "Solo quedan ${item.producto.stock} de ${item.producto.nombre}"
                )
                else -> estado.copy(
                    carrito = estado.carrito.toMutableList().also { it[index] = item.copy(cantidad = nueva) },
                    error = null
                )
            }
        }
    }

    fun eliminar(index: Int) {
        _estado.update { estado ->
            estado.copy(carrito = estado.carrito.filterIndexed { i, _ -> i != index }, error = null)
        }
    }

    fun cambiarNotas(index: Int, notas: String) {
        _estado.update { estado ->
            val item = estado.carrito.getOrNull(index) ?: return@update estado
            estado.copy(carrito = estado.carrito.toMutableList().also { it[index] = item.copy(notas = notas) })
        }
    }

    fun vaciarCarrito() {
        _estado.update {
            it.copy(carrito = emptyList(), efectivoRecibido = "", folioExterno = "", error = null)
        }
    }

    // ---------- Cobro ----------

    fun seleccionarOrigen(origen: OrigenPedido) {
        _estado.update {
            val esExterno = origen != OrigenPedido.MOSTRADOR
            it.copy(
                origen = origen,
                // Los pedidos de plataforma ya vienen pagados
                metodoPago = when {
                    esExterno -> MetodoPago.PLATAFORMA
                    it.metodoPago == MetodoPago.PLATAFORMA -> MetodoPago.EFECTIVO
                    else -> it.metodoPago
                },
                folioExterno = if (esExterno) it.folioExterno else "",
                efectivoRecibido = if (esExterno) "" else it.efectivoRecibido,
                error = null
            )
        }
    }

    fun onFolioChange(valor: String) {
        _estado.update { it.copy(folioExterno = valor.uppercase(), error = null) }
    }

    fun seleccionarMetodoPago(metodo: MetodoPago) {
        _estado.update {
            it.copy(
                metodoPago = metodo,
                efectivoRecibido = if (metodo == MetodoPago.EFECTIVO) it.efectivoRecibido else "",
                error = null
            )
        }
    }

    fun onEfectivoChange(valor: String) {
        // Solo números y un punto decimal
        val limpio = valor.filter { it.isDigit() || it == '.' }
        if (limpio.count { it == '.' } > 1) return
        _estado.update { it.copy(efectivoRecibido = limpio, error = null) }
    }

    fun cobrar() {
        val estado = _estado.value
        if (estado.procesando) return

        // Si escribió cuánto le dieron, validamos que alcance
        if (estado.metodoPago == MetodoPago.EFECTIVO && estado.efectivoRecibido.isNotBlank()) {
            val cambio = estado.cambioCentavos
            if (cambio == null) {
                _estado.update { it.copy(error = "Escribe un monto de efectivo válido") }
                return
            }
            if (cambio < 0) {
                _estado.update { it.copy(error = "El efectivo recibido no alcanza") }
                return
            }
        }

        _estado.update { it.copy(procesando = true, error = null) }

        viewModelScope.launch {
            val resultado = pedidoRepository.registrarVenta(
                items = estado.carrito,
                metodoPago = estado.metodoPago,
                usuarioId = authRepository.usuarioActual.value?.id,
                origen = estado.origen,
                folioExterno = estado.folioExterno.takeIf { estado.esExterno }
            )

            when (resultado) {
                is ResultadoVenta.Exito -> {
                    // El cambio se calcula con el total REAL que guardó la base
                    val cambio = Dinero.aCentavos(estado.efectivoRecibido)
                        ?.let { recibido -> Dinero.formatear(recibido - resultado.totalCentavos) }

                    // Carrito limpio para la siguiente venta (conservamos la categoría)
                    _estado.update {
                        VentasUiState(
                            categoriaSeleccionada = it.categoriaSeleccionada,
                            ventaConfirmada = VentaConfirmada(
                                pedidoId = resultado.pedidoId,
                                total = Dinero.formatear(resultado.totalCentavos),
                                cambio = cambio,
                                origen = estado.origen
                            )
                        )
                    }
                }
                is ResultadoVenta.Error -> _estado.update {
                    it.copy(procesando = false, error = resultado.mensaje)
                }
            }
        }
    }

    fun cerrarConfirmacion() {
        _estado.update { it.copy(ventaConfirmada = null) }
    }

    fun limpiarError() {
        _estado.update { it.copy(error = null) }
    }
}