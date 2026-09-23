package com.example.cafe_flow_v3.ui.productos

import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafe_flow_v3.data.entity.Categoria
import com.example.cafe_flow_v3.data.entity.Producto
import com.example.cafe_flow_v3.data.repository.ProductoRepository
import com.example.cafe_flow_v3.util.Dinero
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

// ---------- Modelos para la pantalla ----------

data class ProductoListaUi(
    val producto: Producto,
    val categoria: String,
    val precio: String // "$110.00"
)

// Lo que el usuario va escribiendo en el formulario (todo como texto)
data class FormularioProducto(
    val id: Long = 0,                 // 0 = producto nuevo
    val nombre: String = "",
    val categoriaId: Long? = null,
    val precio: String = "",          // en pesos: "45.50"
    val stock: String = "0",          // solo se escribe al crear
    val stockMinimo: String = "5",
    val disponible: Boolean = true,
    val stockActual: Int = 0          // al editar, solo para mostrarlo
) {
    val esNuevo: Boolean get() = id == 0L
}

data class MensajeProductos(
    val texto: String,
    val esError: Boolean
)

data class ProductosUiState(
    val cargando: Boolean = true,
    val categorias: List<Categoria> = emptyList(),
    val totalProductos: Int = 0,
    val productos: List<ProductoListaUi> = emptyList(),
    val busqueda: String = "",
    val categoriaFiltro: Long? = null,
    // Formulario (null = se ve la lista)
    val formulario: FormularioProducto? = null,
    val errorFormulario: String? = null,
    val guardando: Boolean = false,
    val confirmarEliminar: Boolean = false,
    // Diálogo "Nueva categoría"
    val dialogoCategoria: Boolean = false,
    val nombreCategoria: String = "",
    val errorCategoria: String? = null,
    val mensaje: MensajeProductos? = null
)

// ---------- ViewModel ----------

class ProductosViewModel(
    private val productoRepository: ProductoRepository
) : ViewModel() {

    private val _estado = MutableStateFlow(ProductosUiState())

    // Copia de TODOS los productos (sin filtros) para tener el stock más reciente al guardar
    private var todosLosProductos: List<Producto> = emptyList()

    val uiState: StateFlow<ProductosUiState> =
        combine(
            _estado,
            productoRepository.observarProductos(),
            productoRepository.observarCategorias()
        ) { estado, productos, categorias ->
            todosLosProductos = productos

            val nombres = categorias.associate { it.id to it.nombre }
            val texto = estado.busqueda.trim()

            estado.copy(
                cargando = false,
                categorias = categorias,
                totalProductos = productos.size,
                productos = productos
                    .filter { estado.categoriaFiltro == null || it.categoriaId == estado.categoriaFiltro }
                    .filter { texto.isEmpty() || it.nombre.contains(texto, ignoreCase = true) }
                    .map {
                        ProductoListaUi(
                            producto = it,
                            categoria = nombres[it.categoriaId].orEmpty(),
                            precio = Dinero.formatear(it.precioCentavos)
                        )
                    }
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProductosUiState()
        )

    // ---------- Lista ----------

    fun onBusquedaChange(valor: String) {
        _estado.update { it.copy(busqueda = valor) }
    }

    fun filtrarCategoria(categoriaId: Long?) {
        _estado.update { it.copy(categoriaFiltro = categoriaId) }
    }

    // Switch "Disponible" directo desde la lista
    fun cambiarDisponibilidad(producto: Producto, disponible: Boolean) {
        viewModelScope.launch {
            val mensaje = try {
                productoRepository.cambiarDisponibilidad(producto, disponible)
                MensajeProductos(
                    if (disponible) "${producto.nombre} ya aparece en el menú"
                    else "${producto.nombre} se ocultó del menú",
                    esError = false
                )
            } catch (e: SQLiteException) {
                MensajeProductos("No se pudo actualizar ${producto.nombre}", esError = true)
            }
            _estado.update { it.copy(mensaje = mensaje) }
        }
    }

    // ---------- Abrir / cerrar formulario ----------

    fun nuevoProducto() {
        val estado = uiState.value
        _estado.update {
            it.copy(
                formulario = FormularioProducto(
                    // Si estás filtrando por una categoría, la preseleccionamos
                    categoriaId = estado.categoriaFiltro ?: estado.categorias.firstOrNull()?.id
                ),
                errorFormulario = null,
                mensaje = null
            )
        }
    }

    fun editar(producto: Producto) {
        _estado.update {
            it.copy(
                formulario = FormularioProducto(
                    id = producto.id,
                    nombre = producto.nombre,
                    categoriaId = producto.categoriaId,
                    precio = BigDecimal.valueOf(producto.precioCentavos, 2).toPlainString(),
                    stock = producto.stock.toString(),
                    stockMinimo = producto.stockMinimo.toString(),
                    disponible = producto.disponible,
                    stockActual = producto.stock
                ),
                errorFormulario = null,
                mensaje = null
            )
        }
    }

    fun cerrarFormulario() {
        _estado.update { it.copy(formulario = null, errorFormulario = null, confirmarEliminar = false) }
    }

    // ---------- Campos del formulario ----------

    private fun actualizarFormulario(cambio: (FormularioProducto) -> FormularioProducto) {
        _estado.update { estado ->
            estado.formulario?.let { estado.copy(formulario = cambio(it), errorFormulario = null) } ?: estado
        }
    }

    fun onNombreChange(valor: String) = actualizarFormulario { it.copy(nombre = valor.take(40)) }

    fun onCategoriaChange(categoriaId: Long) = actualizarFormulario { it.copy(categoriaId = categoriaId) }

    fun onPrecioChange(valor: String) {
        val limpio = valor.filter { it.isDigit() || it == '.' }
        if (limpio.count { it == '.' } > 1) return
        actualizarFormulario { it.copy(precio = limpio.take(9)) }
    }

    fun onStockChange(valor: String) = actualizarFormulario { it.copy(stock = valor.filter(Char::isDigit).take(5)) }

    fun onStockMinimoChange(valor: String) = actualizarFormulario { it.copy(stockMinimo = valor.filter(Char::isDigit).take(5)) }

    fun onDisponibleChange(valor: Boolean) = actualizarFormulario { it.copy(disponible = valor) }

    // ---------- Guardar ----------

    fun guardar() {
        val estado = _estado.value
        val form = estado.formulario ?: return
        if (estado.guardando) return

        val precio = Dinero.aCentavos(form.precio)
        val stock = form.stock.toIntOrNull()
        val stockMinimo = form.stockMinimo.toIntOrNull()

        val error = when {
            form.nombre.isBlank() -> "Escribe el nombre del producto"
            form.categoriaId == null -> "Elige una categoría"
            precio == null || precio <= 0 -> "Escribe un precio válido mayor a $0"
            form.esNuevo && stock == null -> "Escribe el stock inicial"
            stockMinimo == null -> "Escribe el stock mínimo"
            else -> null
        }
        if (error != null || precio == null || stockMinimo == null || form.categoriaId == null) {
            _estado.update { it.copy(errorFormulario = error) }
            return
        }

        // Al editar NO usamos el stock del formulario: tomamos el más reciente de la base,
        // por si hubo una venta mientras el formulario estaba abierto.
        val stockFinal = if (form.esNuevo) stock ?: 0
        else todosLosProductos.find { it.id == form.id }?.stock ?: form.stockActual

        val producto = Producto(
            id = form.id,
            categoriaId = form.categoriaId,
            nombre = form.nombre,
            precioCentavos = precio,
            stock = stockFinal,
            stockMinimo = stockMinimo,
            disponible = form.disponible
        )

        _estado.update { it.copy(guardando = true, errorFormulario = null) }

        viewModelScope.launch {
            val resultado = productoRepository.guardarProducto(producto)
            _estado.update {
                resultado.fold(
                    onSuccess = { _ ->
                        it.copy(
                            guardando = false,
                            formulario = null,
                            mensaje = MensajeProductos(
                                if (form.esNuevo) "${producto.nombre.trim()} se agregó al menú"
                                else "Cambios guardados en ${producto.nombre.trim()}",
                                esError = false
                            )
                        )
                    },
                    onFailure = { e ->
                        it.copy(guardando = false, errorFormulario = e.message ?: "No se pudo guardar")
                    }
                )
            }
        }
    }

    // ---------- Eliminar ----------

    fun pedirEliminar() {
        _estado.update { it.copy(confirmarEliminar = true) }
    }

    fun cancelarEliminar() {
        _estado.update { it.copy(confirmarEliminar = false) }
    }

    fun confirmarEliminar() {
        val form = _estado.value.formulario ?: return
        val producto = todosLosProductos.find { it.id == form.id } ?: return

        _estado.update { it.copy(confirmarEliminar = false, guardando = true) }

        viewModelScope.launch {
            val resultado = productoRepository.eliminarProducto(producto)
            _estado.update {
                resultado.fold(
                    onSuccess = { _ ->
                        it.copy(
                            guardando = false,
                            formulario = null,
                            mensaje = MensajeProductos("${producto.nombre} se eliminó", esError = false)
                        )
                    },
                    // Si ya tiene ventas, el repositorio sugiere ocultarlo en lugar de borrarlo
                    onFailure = { e ->
                        it.copy(guardando = false, errorFormulario = e.message ?: "No se pudo eliminar")
                    }
                )
            }
        }
    }

    // ---------- Nueva categoría ----------

    fun abrirNuevaCategoria() {
        _estado.update { it.copy(dialogoCategoria = true, nombreCategoria = "", errorCategoria = null) }
    }

    fun cerrarNuevaCategoria() {
        _estado.update { it.copy(dialogoCategoria = false, nombreCategoria = "", errorCategoria = null) }
    }

    fun onNombreCategoriaChange(valor: String) {
        _estado.update { it.copy(nombreCategoria = valor.take(30), errorCategoria = null) }
    }

    fun guardarCategoria() {
        val nombre = _estado.value.nombreCategoria

        viewModelScope.launch {
            val resultado = productoRepository.crearCategoria(nombre)
            _estado.update { estado ->
                resultado.fold(
                    onSuccess = { nuevoId ->
                        estado.copy(
                            dialogoCategoria = false,
                            nombreCategoria = "",
                            // Si el formulario está abierto, dejamos seleccionada la nueva categoría
                            formulario = estado.formulario?.copy(categoriaId = nuevoId),
                            mensaje = MensajeProductos("Categoría ${nombre.trim()} creada", esError = false)
                        )
                    },
                    onFailure = { e ->
                        estado.copy(errorCategoria = e.message ?: "No se pudo crear la categoría")
                    }
                )
            }
        }
    }

    fun cerrarMensaje() {
        _estado.update { it.copy(mensaje = null) }
    }
}
