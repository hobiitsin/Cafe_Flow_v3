package com.example.cafe_flow_v3.ui.inventario

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
fun InventarioScreen(
    modifier: Modifier = Modifier,
    viewModel: InventarioViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsState()

    // Los mensajes de exito se borran solos
    LaunchedEffect(state.mensaje) {
        if (state.mensaje?.esError == false) {
            delay(3_000)
            viewModel.cerrarMensaje()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Encabezado
        item {
            Text(
                text = "Inventario",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Serif,
                color = DarkBrown
            )
            Text(
                text = "Existencias y alertas de tus productos",
                fontSize = 13.sp,
                color = TextGray
            )
        }

        // Tarjetas de resumen
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TarjetaResumen(
                    titulo = "Productos",
                    valor = state.totalProductos,
                    icono = Icons.Outlined.Inventory2,
                    fondoIcono = BgCream,
                    colorIcono = DarkBrown,
                    seleccionada = state.filtro == FiltroInventario.TODOS,
                    onClick = { viewModel.seleccionarFiltro(FiltroInventario.TODOS) },
                    modifier = Modifier.weight(1f)
                )
                TarjetaResumen(
                    titulo = "Stock bajo",
                    valor = state.totalBajo,
                    icono = Icons.Outlined.WarningAmber,
                    fondoIcono = OrangeBadgeBg,
                    colorIcono = OrangeBadgeText,
                    seleccionada = state.filtro == FiltroInventario.BAJO,
                    onClick = { viewModel.seleccionarFiltro(FiltroInventario.BAJO) },
                    modifier = Modifier.weight(1f)
                )
                TarjetaResumen(
                    titulo = "Agotados",
                    valor = state.totalAgotados,
                    icono = Icons.Outlined.RemoveShoppingCart,
                    fondoIcono = ErrorBg,
                    colorIcono = ErrorRed,
                    seleccionada = state.filtro == FiltroInventario.AGOTADOS,
                    onClick = { viewModel.seleccionarFiltro(FiltroInventario.AGOTADOS) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Buscador
        item {
            OutlinedTextField(
                value = state.busqueda,
                onValueChange = viewModel::onBusquedaChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("Buscar producto", color = TextGray.copy(alpha = 0.7f)) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = TextGray) },
                trailingIcon = if (state.busqueda.isNotEmpty()) {
                    {
                        IconButton(onClick = { viewModel.onBusquedaChange("") }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Borrar búsqueda", tint = TextGray)
                        }
                    }
                } else null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Terracotta,
                    unfocusedBorderColor = LightBorder,
                    focusedContainerColor = CardBg,
                    unfocusedContainerColor = CardBg
                )
            )
        }

        // Mensaje
        state.mensaje?.let { mensaje ->
            item {
                BannerMensaje(mensaje = mensaje, onCerrar = viewModel::cerrarMensaje)
            }
        }

        // Lista
        when {
            state.cargando -> item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Terracotta)
                }
            }

            state.productos.isEmpty() -> item {
                Text(
                    text = when {
                        state.busqueda.isNotBlank() -> "No hay productos que coincidan con \"${state.busqueda}\""
                        state.filtro == FiltroInventario.BAJO -> "Ningún producto tiene stock bajo 🎉"
                        state.filtro == FiltroInventario.AGOTADOS -> "No hay productos agotados 🎉"
                        else -> "Todavía no hay productos"
                    },
                    fontSize = 14.sp,
                    color = TextGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp)
                )
            }

            else -> items(state.productos, key = { it.producto.id }) { item ->
                FilaProducto(
                    item = item,
                    onReabastecer = { viewModel.abrirReabastecer(item.producto) }
                )
            }
        }
    }

    // Dialogo de reabastecer
    state.productoAReabastecer?.let { producto ->
        AlertDialog(
            onDismissRequest = { if (!state.procesando) viewModel.cerrarReabastecer() },
            containerColor = CardBg,
            title = {
                Text(
                    text = "Reabastecer",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Black,
                    color = DarkBrown
                )
            },
            text = {
                Column {
                    Text(producto.nombre, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkBrown)
                    Text(
                        text = "Stock actual: ${producto.stock} · mínimo ${producto.stockMinimo}",
                        fontSize = 13.sp,
                        color = TextGray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = state.cantidad,
                        onValueChange = viewModel::onCantidadChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !state.procesando,
                        shape = RoundedCornerShape(10.dp),
                        label = { Text("Cantidad que llegó") },
                        isError = state.errorCantidad != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Terracotta,
                            focusedLabelColor = Terracotta,
                            unfocusedBorderColor = LightBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Botones
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5, 10, 20).forEach { n ->
                            ChipRapido(texto = "+$n", onClick = { viewModel.sumarRapido(n) })
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    state.errorCantidad?.let {
                        Text(it, color = ErrorRed, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    } ?: state.stockResultante?.let {
                        Text(
                            text = "Quedará en $it",
                            color = GreenBadgeText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmarReabastecer,
                    enabled = !state.procesando,
                    colors = ButtonDefaults.buttonColors(containerColor = Terracotta)
                ) {
                    if (state.procesando) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Text("Agregar", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cerrarReabastecer, enabled = !state.procesando) {
                    Text("Cancelar", color = DarkBrown)
                }
            }
        )
    }
}

// Fila de producto

@Composable
private fun FilaProducto(
    item: ProductoInventarioUi,
    onReabastecer: () -> Unit
) {
    val producto = item.producto
    val (texto, fondo, color) = when (item.nivel) {
        NivelStock.AGOTADO -> Triple("Agotado", ErrorBg, ErrorRed)
        NivelStock.BAJO -> Triple("Stock bajo", OrangeBadgeBg, OrangeBadgeText)
        NivelStock.OK -> Triple("En stock", GreenBadgeBg, GreenBadgeText)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(producto.nombre, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DarkBrown)
                    Text(
                        text = buildString {
                            append(item.categoria)
                            if (!producto.disponible) append(" · Oculto del menú")
                        },
                        fontSize = 12.sp,
                        color = TextGray
                    )
                }
                Insignia(texto = texto, fondo = fondo, color = color)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${producto.stock}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = if (item.nivel == NivelStock.OK) DarkBrown else color
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "en stock · mín. ${producto.stockMinimo}",
                            fontSize = 12.sp,
                            color = TextGray,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    BarraStock(
                        proporcion = item.proporcion,
                        color = if (item.nivel == NivelStock.OK) GreenBadgeText else color
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                FilledTonalButton(
                    onClick = onReabastecer,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = BgCream,
                        contentColor = Terracotta
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Outlined.AddBox, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reabastecer", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Barra del stock
@Composable
private fun BarraStock(proporcion: Float, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(LightBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(proporcion.coerceAtLeast(0.02f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
    }
}

// Componentes

@Composable
private fun TarjetaResumen(
    titulo: String,
    valor: Int,
    icono: ImageVector,
    fondoIcono: Color,
    colorIcono: Color,
    seleccionada: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val forma = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(forma)
            .background(CardBg)
            .border(if (seleccionada) 1.5.dp else 1.dp, if (seleccionada) Terracotta else LightBorder, forma)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(fondoIcono),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, contentDescription = null, tint = colorIcono, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("$valor", fontSize = 20.sp, fontWeight = FontWeight.Black, color = DarkBrown)
        Text(titulo, fontSize = 11.sp, color = TextGray, maxLines = 1)
    }
}

@Composable
private fun ChipRapido(texto: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(BgCream)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(texto, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Terracotta)
    }
}

@Composable
private fun Insignia(texto: String, fondo: Color, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(fondo)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(texto, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun BannerMensaje(mensaje: MensajeInventario, onCerrar: () -> Unit) {
    val fondo = if (mensaje.esError) ErrorBg else GreenBadgeBg
    val color = if (mensaje.esError) ErrorRed else GreenBadgeText

    Row(
        modifier = Modifier
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