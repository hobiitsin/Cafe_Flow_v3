package com.example.cafe_flow_v3.ui.ventas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cafe_flow_v3.data.entity.MetodoPago
import com.example.cafe_flow_v3.data.entity.OrigenPedido
import com.example.cafe_flow_v3.data.model.ItemCarrito
import com.example.cafe_flow_v3.ui.AppViewModelProvider
import com.example.cafe_flow_v3.ui.common.etiqueta
import com.example.cafe_flow_v3.ui.theme.*
import com.example.cafe_flow_v3.util.Dinero

private val ErrorRed = Color(0xFFC62828)
private val ErrorBg = Color(0xFFFFEBEE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VentasScreen(
    onVerPedidos: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VentasViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsState()
    var mostrarCarrito by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Si el carrito se vacía o la venta se registró, cerramos la hoja del carrito
    LaunchedEffect(state.carrito.isEmpty(), state.ventaConfirmada) {
        if (state.carrito.isEmpty() || state.ventaConfirmada != null) {
            mostrarCarrito = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Encabezado
            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp)) {
                Text(
                    text = "Nueva venta",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Serif,
                    color = DarkBrown
                )
                Text(
                    text = "Toca un producto para agregarlo al carrito",
                    fontSize = 13.sp,
                    color = TextGray
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Categorías
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    ChipOpcion(
                        texto = "Todas",
                        seleccionado = state.categoriaSeleccionada == null,
                        onClick = { viewModel.seleccionarCategoria(null) }
                    )
                }
                items(state.categorias, key = { it.id }) { categoria ->
                    ChipOpcion(
                        texto = categoria.nombre,
                        seleccionado = state.categoriaSeleccionada == categoria.id,
                        onClick = { viewModel.seleccionarCategoria(categoria.id) }
                    )
                }
            }

            // Errores al agregar (con la hoja cerrada)
            if (!mostrarCarrito) {
                state.error?.let {
                    MensajeError(
                        mensaje = it,
                        onCerrar = viewModel::limpiarError,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Productos
            when {
                state.cargando -> Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Terracotta)
                }

                state.productos.isEmpty() -> Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay productos en esta categoría", color = TextGray, fontSize = 14.sp)
                }

                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    // Espacio abajo para que la barra del carrito no tape la última fila
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.productos, key = { it.producto.id }) { item ->
                        ProductoCard(item = item, onClick = { viewModel.agregar(item.producto) })
                    }
                }
            }
        }

        // Barra flotante del carrito
        if (state.carrito.isNotEmpty()) {
            BarraCarrito(
                articulos = state.articulos,
                totalCentavos = state.totalCentavos,
                onClick = { mostrarCarrito = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }

    // Hoja inferior con el carrito y el cobro
    if (mostrarCarrito) {
        ModalBottomSheet(
            onDismissRequest = { mostrarCarrito = false },
            sheetState = sheetState,
            containerColor = BgCream
        ) {
            CarritoContenido(state = state, viewModel = viewModel)
        }
    }

    // Confirmación de venta
    state.ventaConfirmada?.let { venta ->
        AlertDialog(
            onDismissRequest = viewModel::cerrarConfirmacion,
            containerColor = CardBg,
            icon = {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = GreenBadgeText,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text("¡Venta registrada!", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, color = DarkBrown)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Pedido #${venta.pedidoId} · ${venta.origen.etiqueta()}", color = DarkBrown)
                    Text("Total: ${venta.total}", color = DarkBrown, fontWeight = FontWeight.Bold)
                    venta.cambio?.let {
                        Text("Cambio: $it", color = GreenBadgeText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Quedó como Pendiente en la pestaña Pedidos.",
                        color = TextGray,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::cerrarConfirmacion,
                    colors = ButtonDefaults.buttonColors(containerColor = Terracotta)
                ) {
                    Text("Nueva venta", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.cerrarConfirmacion()
                    onVerPedidos()
                }) {
                    Text("Ver pedidos", color = Terracotta)
                }
            }
        )
    }
}

// ---------- Tarjeta de producto ----------

@Composable
private fun ProductoCard(item: ProductoMenuUi, onClick: () -> Unit) {
    val producto = item.producto
    val forma = RoundedCornerShape(16.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (item.agotado) 0.55f else 1f)
            .clip(forma)
            .clickable(enabled = item.puedeAgregar, onClick = onClick),
        shape = forma,
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = if (item.enCarrito > 0) BorderStroke(1.5.dp, Terracotta) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Box {
            Column(modifier = Modifier.padding(14.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(BgCream),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconoCategoria(item.categoria),
                        contentDescription = null,
                        tint = Terracotta,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = producto.nombre,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkBrown,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Dinero.formatear(producto.precioCentavos),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = DarkBrown
                )
                Spacer(modifier = Modifier.height(6.dp))
                when {
                    item.agotado -> Insignia("Agotado", ErrorBg, ErrorRed)
                    item.bajoInventario -> Insignia("Quedan ${producto.stock}", OrangeBadgeBg, OrangeBadgeText)
                    else -> Text("Stock: ${producto.stock}", fontSize = 11.sp, color = TextGray)
                }
            }

            // Cuántos van en el carrito
            if (item.enCarrito > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Terracotta),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${item.enCarrito}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Ícono según el nombre de la categoría
private fun iconoCategoria(categoria: String): ImageVector {
    val nombre = categoria.lowercase()
    return when {
        "fría" in nombre || "fria" in nombre -> Icons.Outlined.LocalDrink
        "pan" in nombre -> Icons.Outlined.BakeryDining
        "postre" in nombre -> Icons.Outlined.Cake
        "aliment" in nombre || "comida" in nombre -> Icons.Outlined.Restaurant
        else -> Icons.Outlined.Coffee
    }
}

// ---------- Barra inferior "Ver carrito" ----------

@Composable
private fun BarraCarrito(
    articulos: Int,
    totalCentavos: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkBrown)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Terracotta),
            contentAlignment = Alignment.Center
        ) {
            Text("$articulos", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Ver carrito", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(
                text = if (articulos == 1) "1 producto" else "$articulos productos",
                color = BgCream.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }
        Text(
            text = Dinero.formatear(totalCentavos),
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 17.sp
        )
    }
}

// ---------- Contenido de la hoja del carrito ----------

@Composable
private fun CarritoContenido(state: VentasUiState, viewModel: VentasViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Carrito",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Serif,
                color = DarkBrown
            )
            TextButton(onClick = viewModel::vaciarCarrito) {
                Text("Vaciar", color = ErrorRed, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Renglones
        state.carrito.forEachIndexed { index, item ->
            LineaCarrito(
                item = item,
                puedeAumentar = state.puedeAumentar(item),
                onMenos = { viewModel.cambiarCantidad(index, -1) },
                onMas = { viewModel.cambiarCantidad(index, +1) },
                onEliminar = { viewModel.eliminar(index) },
                onNotas = { viewModel.cambiarNotas(index, it) }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Tipo de pedido
        TituloSeccion("Tipo de pedido")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OrigenPedido.entries.forEach { origen ->
                ChipOpcion(
                    texto = origen.etiqueta(),
                    seleccionado = state.origen == origen,
                    onClick = { viewModel.seleccionarOrigen(origen) }
                )
            }
        }
        if (state.esExterno) {
            Spacer(modifier = Modifier.height(10.dp))
            CampoTexto(
                valor = state.folioExterno,
                onCambio = viewModel::onFolioChange,
                placeholder = "Folio de ${state.origen.etiqueta()} (ej. DD-12345)"
            )
        }

        // Método de pago
        TituloSeccion("Método de pago")
        if (state.esExterno) {
            Text("Pagado en ${state.origen.etiqueta()}", color = TextGray, fontSize = 14.sp)
        } else {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(MetodoPago.EFECTIVO, MetodoPago.TARJETA, MetodoPago.TRANSFERENCIA).forEach { metodo ->
                    ChipOpcion(
                        texto = metodo.etiqueta(),
                        seleccionado = state.metodoPago == metodo,
                        onClick = { viewModel.seleccionarMetodoPago(metodo) }
                    )
                }
            }
        }

        // Efectivo recibido y cambio
        if (state.metodoPago == MetodoPago.EFECTIVO) {
            Spacer(modifier = Modifier.height(10.dp))
            CampoTexto(
                valor = state.efectivoRecibido,
                onCambio = viewModel::onEfectivoChange,
                placeholder = "Efectivo recibido (opcional)",
                teclado = KeyboardType.Decimal,
                prefijo = "$"
            )
            state.cambioCentavos?.let { cambio ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (cambio >= 0) "Cambio: ${Dinero.formatear(cambio)}"
                    else "Faltan ${Dinero.formatear(-cambio)}",
                    color = if (cambio >= 0) GreenBadgeText else ErrorRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        HorizontalDivider(color = LightBorder, modifier = Modifier.padding(vertical = 16.dp))

        // Total
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Total", fontSize = 16.sp, color = TextGray)
            Text(
                text = Dinero.formatear(state.totalCentavos),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = DarkBrown
            )
        }

        state.error?.let {
            Spacer(modifier = Modifier.height(10.dp))
            MensajeError(mensaje = it, onCerrar = viewModel::limpiarError)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = viewModel::cobrar,
            enabled = !state.procesando && state.carrito.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Terracotta,
                disabledContainerColor = Terracotta.copy(alpha = 0.6f)
            )
        ) {
            if (state.procesando) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            } else {
                Text(
                    text = "Cobrar ${Dinero.formatear(state.totalCentavos)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun LineaCarrito(
    item: ItemCarrito,
    puedeAumentar: Boolean,
    onMenos: () -> Unit,
    onMas: () -> Unit,
    onEliminar: () -> Unit,
    onNotas: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.producto.nombre, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DarkBrown)
                    Text("${Dinero.formatear(item.producto.precioCentavos)} c/u", fontSize = 12.sp, color = TextGray)
                }
                IconButton(onClick = onEliminar) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Quitar", tint = TextGray)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Contador(cantidad = item.cantidad, puedeAumentar = puedeAumentar, onMenos = onMenos, onMas = onMas)
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = Dinero.formatear(item.subtotalCentavos),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = DarkBrown
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            CampoTexto(
                valor = item.notas.orEmpty(),
                onCambio = onNotas,
                placeholder = "Notas: sin azúcar, leche de almendra..."
            )
        }
    }
}

@Composable
private fun Contador(
    cantidad: Int,
    puedeAumentar: Boolean,
    onMenos: () -> Unit,
    onMas: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(BgCream)
    ) {
        IconButton(onClick = onMenos, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Remove, contentDescription = "Quitar uno", tint = DarkBrown, modifier = Modifier.size(18.dp))
        }
        Text(
            text = "$cantidad",
            fontWeight = FontWeight.Bold,
            color = DarkBrown,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 24.dp)
        )
        IconButton(onClick = onMas, enabled = puedeAumentar, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Agregar uno",
                tint = if (puedeAumentar) DarkBrown else TextGray.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ---------- Componentes pequeños ----------

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
private fun Insignia(texto: String, fondo: Color, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(fondo)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(texto, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun TituloSeccion(texto: String) {
    Text(
        text = texto,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextGray,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
    )
}

@Composable
private fun CampoTexto(
    valor: String,
    onCambio: (String) -> Unit,
    placeholder: String,
    teclado: KeyboardType = KeyboardType.Text,
    prefijo: String? = null
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        placeholder = { Text(placeholder, fontSize = 13.sp, color = TextGray.copy(alpha = 0.7f)) },
        prefix = if (prefijo != null) {
            { Text(prefijo, color = DarkBrown) }
        } else null,
        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = DarkBrown),
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
private fun MensajeError(
    mensaje: String,
    onCerrar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ErrorBg)
            .clickable(onClick = onCerrar)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(mensaje, color = ErrorRed, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Icon(Icons.Outlined.Close, contentDescription = "Cerrar", tint = ErrorRed, modifier = Modifier.size(16.dp))
    }
}
