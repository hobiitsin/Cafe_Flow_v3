package com.example.cafe_flow_v3.ui.productos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cafe_flow_v3.ui.AppViewModelProvider
import com.example.cafe_flow_v3.ui.theme.*
import kotlinx.coroutines.delay

private val ErrorRed = Color(0xFFC62828)
private val ErrorBg = Color(0xFFFFEBEE)

@Composable
fun ProductosScreen(
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProductosViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsState()

    // Con el formulario abierto, "atrás" regresa a la lista (no al menú)
    BackHandler(enabled = state.formulario != null) {
        viewModel.cerrarFormulario()
    }

    LaunchedEffect(state.mensaje) {
        if (state.mensaje?.esError == false) {
            delay(3_000)
            viewModel.cerrarMensaje()
        }
    }

    val formulario = state.formulario
    if (formulario == null) {
        ListaProductos(state = state, viewModel = viewModel, onVolver = onVolver, modifier = modifier)
    } else {
        FormularioProductoContenido(form = formulario, state = state, viewModel = viewModel, modifier = modifier)
    }

    // Diálogo: nueva categoría
    if (state.dialogoCategoria) {
        AlertDialog(
            onDismissRequest = viewModel::cerrarNuevaCategoria,
            containerColor = CardBg,
            title = { Text("Nueva categoría", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, color = DarkBrown) },
            text = {
                Column {
                    Campo(
                        valor = state.nombreCategoria,
                        onCambio = viewModel::onNombreCategoriaChange,
                        etiqueta = "Nombre (ej. Tés, Smoothies)"
                    )
                    state.errorCategoria?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(it, color = ErrorRed, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::guardarCategoria,
                    colors = ButtonDefaults.buttonColors(containerColor = Terracotta)
                ) { Text("Crear", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cerrarNuevaCategoria) { Text("Cancelar", color = DarkBrown) }
            }
        )
    }

    // Diálogo: confirmar eliminación
    if (state.confirmarEliminar && formulario != null) {
        AlertDialog(
            onDismissRequest = viewModel::cancelarEliminar,
            containerColor = CardBg,
            icon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = ErrorRed) },
            title = {
                Text("¿Eliminar ${formulario.nombre}?", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, color = DarkBrown)
            },
            text = {
                Text(
                    "Solo se puede eliminar si nunca se ha vendido. Si ya tiene ventas, mejor ocúltalo del menú.",
                    color = TextGray
                )
            },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmarEliminar,
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) { Text("Eliminar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelarEliminar) { Text("Cancelar", color = DarkBrown) }
            }
        )
    }
}

// ================= LISTA =================

@Composable
private fun ListaProductos(
    state: ProductosUiState,
    viewModel: ProductosViewModel,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Encabezado con flecha de regreso
            Row(
                modifier = Modifier.padding(start = 8.dp, end = 12.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onVolver) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Regresar", tint = DarkBrown)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Productos", fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Serif, color = DarkBrown)
                    Text("${state.totalProductos} en el catálogo", fontSize = 13.sp, color = TextGray)
                }
                TextButton(onClick = viewModel::abrirNuevaCategoria) {
                    Icon(Icons.Outlined.CreateNewFolder, contentDescription = null, tint = Terracotta, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Categoría", color = Terracotta, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // Buscador
            OutlinedTextField(
                value = state.busqueda,
                onValueChange = viewModel::onBusquedaChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("Buscar producto", color = TextGray.copy(alpha = 0.7f)) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = TextGray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Terracotta,
                    unfocusedBorderColor = LightBorder,
                    focusedContainerColor = CardBg,
                    unfocusedContainerColor = CardBg
                )
            )

            // Filtro por categoría
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    ChipOpcion("Todas", state.categoriaFiltro == null) { viewModel.filtrarCategoria(null) }
                }
                items(state.categorias, key = { it.id }) { categoria ->
                    ChipOpcion(categoria.nombre, state.categoriaFiltro == categoria.id) {
                        viewModel.filtrarCategoria(categoria.id)
                    }
                }
            }

            state.mensaje?.let {
                BannerMensaje(
                    mensaje = it,
                    onCerrar = viewModel::cerrarMensaje,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                state.cargando -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Terracotta)
                }

                state.productos.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No hay productos que mostrar", color = TextGray, fontSize = 14.sp)
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    // Espacio abajo para el botón "Nuevo producto"
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.productos, key = { it.producto.id }) { item ->
                        FilaProducto(
                            item = item,
                            onClick = { viewModel.editar(item.producto) },
                            onDisponibleChange = { viewModel.cambiarDisponibilidad(item.producto, it) }
                        )
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = viewModel::nuevoProducto,
            containerColor = Terracotta,
            contentColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Nuevo producto", fontWeight = FontWeight.Bold) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        )
    }
}

@Composable
private fun FilaProducto(
    item: ProductoListaUi,
    onClick: () -> Unit,
    onDisponibleChange: (Boolean) -> Unit
) {
    val producto = item.producto
    val forma = RoundedCornerShape(16.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .clickable(onClick = onClick),
        shape = forma,
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (producto.disponible) 1f else 0.5f)
            ) {
                Text(producto.nombre, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DarkBrown)
                Text(
                    text = "${item.categoria} · Stock ${producto.stock}",
                    fontSize = 12.sp,
                    color = TextGray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(item.precio, fontSize = 16.sp, fontWeight = FontWeight.Black, color = DarkBrown)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Switch(
                    checked = producto.disponible,
                    onCheckedChange = onDisponibleChange,
                    colors = colorSwitch()
                )
                Text(
                    text = if (producto.disponible) "En menú" else "Oculto",
                    fontSize = 10.sp,
                    color = TextGray
                )
            }
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = "Editar",
                tint = TextGray,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

// ================= FORMULARIO =================

@Composable
private fun FormularioProductoContenido(
    form: FormularioProducto,
    state: ProductosUiState,
    viewModel: ProductosViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        // Encabezado
        Row(
            modifier = Modifier.padding(start = 8.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = viewModel::cerrarFormulario) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Regresar", tint = DarkBrown)
            }
            Text(
                text = if (form.esNuevo) "Nuevo producto" else "Editar producto",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Serif,
                color = DarkBrown
            )
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {

            Etiqueta("Nombre")
            Campo(valor = form.nombre, onCambio = viewModel::onNombreChange, etiqueta = "Ej. Chai Latte")

            // Categoría
            Row(verticalAlignment = Alignment.CenterVertically) {
                Etiqueta("Categoría", modifier = Modifier.weight(1f))
                TextButton(onClick = viewModel::abrirNuevaCategoria) {
                    Text("+ Nueva", color = Terracotta, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.categorias.forEach { categoria ->
                    ChipOpcion(categoria.nombre, form.categoriaId == categoria.id) {
                        viewModel.onCategoriaChange(categoria.id)
                    }
                }
            }

            Etiqueta("Precio")
            Campo(
                valor = form.precio,
                onCambio = viewModel::onPrecioChange,
                etiqueta = "0.00",
                teclado = KeyboardType.Decimal,
                prefijo = "$"
            )

            // Stock
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    if (form.esNuevo) {
                        Etiqueta("Stock inicial")
                        Campo(valor = form.stock, onCambio = viewModel::onStockChange, etiqueta = "0", teclado = KeyboardType.Number)
                    } else {
                        Etiqueta("Stock actual")
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BgCream)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text("${form.stockActual}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkBrown)
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Etiqueta("Stock mínimo")
                    Campo(valor = form.stockMinimo, onCambio = viewModel::onStockMinimoChange, etiqueta = "5", teclado = KeyboardType.Number)
                }
            }
            Text(
                text = if (form.esNuevo) "Cuando el stock llegue al mínimo, saldrá la alerta en Inventario."
                else "El stock se ajusta desde Inventario → Reabastecer.",
                fontSize = 12.sp,
                color = TextGray,
                modifier = Modifier.padding(top = 6.dp)
            )

            // Disponible
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBg)
                    .border(1.dp, LightBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Disponible en el menú", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkBrown)
                    Text("Si lo apagas, no aparece en Ventas", fontSize = 12.sp, color = TextGray)
                }
                Switch(
                    checked = form.disponible,
                    onCheckedChange = viewModel::onDisponibleChange,
                    colors = colorSwitch()
                )
            }

            state.errorFormulario?.let {
                Spacer(modifier = Modifier.height(14.dp))
                Text(it, color = ErrorRed, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = viewModel::guardar,
                enabled = !state.guardando,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Terracotta,
                    disabledContainerColor = Terracotta.copy(alpha = 0.6f)
                )
            ) {
                if (state.guardando) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                } else {
                    Text(
                        text = if (form.esNuevo) "Agregar producto" else "Guardar cambios",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            if (!form.esNuevo) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = viewModel::pedirEliminar,
                    enabled = !state.guardando,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Eliminar producto", color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ================= COMPONENTES =================

@Composable
private fun colorSwitch() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = Terracotta,
    uncheckedThumbColor = Color.White,
    uncheckedTrackColor = LightBorder,
    uncheckedBorderColor = LightBorder
)

@Composable
private fun Etiqueta(texto: String, modifier: Modifier = Modifier) {
    Text(
        text = texto,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = DarkBrown,
        modifier = modifier.padding(top = 18.dp, bottom = 6.dp)
    )
}

@Composable
private fun Campo(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    teclado: KeyboardType = KeyboardType.Text,
    prefijo: String? = null
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        placeholder = { Text(etiqueta, color = TextGray.copy(alpha = 0.6f)) },
        prefix = if (prefijo != null) {
            { Text(prefijo, color = DarkBrown) }
        } else null,
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Terracotta,
            unfocusedBorderColor = LightBorder,
            focusedContainerColor = CardBg,
            unfocusedContainerColor = CardBg
        )
    )
}

@Composable
private fun ChipOpcion(texto: String, seleccionado: Boolean, onClick: () -> Unit) {
    val forma = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .clip(forma)
            .background(if (seleccionado) Terracotta else CardBg)
            .border(1.dp, if (seleccionado) Terracotta else LightBorder, forma)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = texto,
            fontSize = 13.sp,
            fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Medium,
            color = if (seleccionado) Color.White else DarkBrown
        )
    }
}

@Composable
private fun BannerMensaje(
    mensaje: MensajeProductos,
    onCerrar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fondo = if (mensaje.esError) ErrorBg else GreenBadgeBg
    val color = if (mensaje.esError) ErrorRed else GreenBadgeText

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(fondo)
            .clickable(onClick = onCerrar)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (mensaje.esError) Icons.Outlined.ErrorOutline else Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(mensaje.texto, color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Icon(Icons.Outlined.Close, contentDescription = "Cerrar", tint = color, modifier = Modifier.size(16.dp))
    }
}