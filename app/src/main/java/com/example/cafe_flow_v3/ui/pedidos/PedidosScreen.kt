package com.example.cafe_flow_v3.ui.pedidos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cafe_flow_v3.coloresEstado
import com.example.cafe_flow_v3.data.entity.EstadoPedido
import com.example.cafe_flow_v3.data.entity.OrigenPedido
import com.example.cafe_flow_v3.ui.AppViewModelProvider
import com.example.cafe_flow_v3.ui.common.etiqueta
import com.example.cafe_flow_v3.ui.theme.*
import kotlinx.coroutines.delay

private val ErrorRed = Color(0xFFC62828)
private val ErrorBg = Color(0xFFFFEBEE)

@Composable
fun PedidosScreen(
    modifier: Modifier = Modifier,
    viewModel: PedidosViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsState()

    // Los mensajes de éxito se quitan solos a los 3 segundos
    LaunchedEffect(state.mensaje) {
        if (state.mensaje?.esError == false) {
            delay(3_000)
            viewModel.cerrarMensaje()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {

        // Encabezado
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp)) {
            Text(
                text = "Pedidos",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Serif,
                color = DarkBrown
            )
            Text(
                text = "Avanza cada pedido conforme se prepara y entrega",
                fontSize = 13.sp,
                color = TextGray
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Pestañas por estado, con su conteo
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(EstadoPedido.entries) { estado ->
                ChipEstado(
                    texto = estado.etiquetaPlural(),
                    conteo = state.conteos[estado] ?: 0,
                    seleccionado = state.estadoSeleccionado == estado,
                    onClick = { viewModel.seleccionarEstado(estado) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filtro por plataforma
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                ChipOrigen(
                    texto = "Todos",
                    seleccionado = state.origenFiltro == null,
                    onClick = { viewModel.filtrarOrigen(null) }
                )
            }
            items(OrigenPedido.entries) { origen ->
                ChipOrigen(
                    texto = origen.etiqueta(),
                    seleccionado = state.origenFiltro == origen,
                    onClick = { viewModel.filtrarOrigen(origen) }
                )
            }
        }

        // Mensaje de éxito o error
        state.mensaje?.let { mensaje ->
            BannerMensaje(
                mensaje = mensaje,
                onCerrar = viewModel::cerrarMensaje,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Lista
        when {
            state.cargando -> Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Terracotta)
            }

            state.pedidos.isEmpty() -> EstadoVacio(
                texto = "No hay pedidos ${state.estadoSeleccionado.etiquetaPlural().lowercase()}",
                modifier = Modifier.fillMaxWidth().weight(1f)
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.pedidos, key = { it.id }) { pedido ->
                    PedidoCard(
                        pedido = pedido,
                        procesando = state.procesandoId == pedido.id,
                        botonesHabilitados = state.procesandoId == null,
                        onAvanzar = { viewModel.avanzar(pedido) },
                        onCancelar = { viewModel.pedirCancelacion(pedido) }
                    )
                }
            }
        }
    }

    // Confirmación de cancelación
    state.pedidoPorCancelar?.let { pedido ->
        AlertDialog(
            onDismissRequest = viewModel::descartarCancelacion,
            containerColor = CardBg,
            icon = { Icon(Icons.Outlined.Cancel, contentDescription = null, tint = ErrorRed) },
            title = {
                Text(
                    "¿Cancelar pedido ${pedido.folio}?",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Black,
                    color = DarkBrown
                )
            },
            text = {
                Text(
                    "Se regresará al inventario lo que llevaba este pedido. Esta acción no se puede deshacer.",
                    color = TextGray
                )
            },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmarCancelacion,
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Sí, cancelar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::descartarCancelacion) {
                    Text("No", color = DarkBrown)
                }
            }
        )
    }
}

// ---------- Tarjeta de pedido ----------

@Composable
private fun PedidoCard(
    pedido: PedidoUi,
    procesando: Boolean,
    botonesHabilitados: Boolean,
    onAvanzar: () -> Unit,
    onCancelar: () -> Unit
) {
    val (fondoEstado, textoEstado) = coloresEstado(pedido.estado)
    val (fondoOrigen, textoOrigen) = coloresOrigen(pedido.origen)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Encabezado: ícono, folio, plataforma y estado
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(BgCream),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (pedido.origen == OrigenPedido.MOSTRADOR) Icons.Outlined.Coffee
                        else Icons.Outlined.LocalShipping,
                        contentDescription = null,
                        tint = DarkBrown,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(pedido.folio, fontSize = 16.sp, fontWeight = FontWeight.Black, color = DarkBrown)
                        Spacer(modifier = Modifier.width(6.dp))
                        Insignia(
                            texto = pedido.origen.etiqueta() +
                                    (pedido.folioExterno?.let { " · $it" } ?: ""),
                            fondo = fondoOrigen,
                            color = textoOrigen
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(pedido.hora, fontSize = 12.sp, color = TextGray)
                        pedido.espera?.let {
                            Text(
                                text = " · $it",
                                fontSize = 12.sp,
                                fontWeight = if (pedido.esperaLarga) FontWeight.Bold else FontWeight.Normal,
                                color = if (pedido.esperaLarga) OrangeBadgeText else TextGray
                            )
                        }
                    }
                }
                Insignia(texto = pedido.estado.etiqueta(), fondo = fondoEstado, color = textoEstado)
            }

            HorizontalDivider(color = LightBorder, modifier = Modifier.padding(vertical = 12.dp))

            // Productos
            pedido.lineas.forEach { linea ->
                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text(
                        text = "${linea.cantidad}x",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Terracotta,
                        modifier = Modifier.width(32.dp)
                    )
                    Column {
                        Text(linea.nombre, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = DarkBrown)
                        if (!linea.notas.isNullOrBlank()) {
                            Text(
                                text = "“${linea.notas}”",
                                fontSize = 12.sp,
                                fontStyle = FontStyle.Italic,
                                color = OrangeBadgeText
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = LightBorder, modifier = Modifier.padding(vertical = 12.dp))

            // Pago y total
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pedido.metodoPago.etiqueta(),
                    fontSize = 12.sp,
                    color = TextGray,
                    modifier = Modifier.weight(1f)
                )
                Text(pedido.total, fontSize = 18.sp, fontWeight = FontWeight.Black, color = DarkBrown)
            }

            // Botones (solo pedidos activos)
            val textoAvanzar = pedido.estado.textoBotonAvanzar()
            if (textoAvanzar != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onCancelar,
                        enabled = botonesHabilitados,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                    ) {
                        Text("Cancelar", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onAvanzar,
                        enabled = botonesHabilitados,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Terracotta,
                            disabledContainerColor = Terracotta.copy(alpha = 0.5f)
                        )
                    ) {
                        if (procesando) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Text(textoAvanzar, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

// ---------- Textos y colores ----------

private fun EstadoPedido.etiquetaPlural(): String = when (this) {
    EstadoPedido.PENDIENTE -> "Pendientes"
    EstadoPedido.EN_PREPARACION -> "En preparación"
    EstadoPedido.LISTO -> "Listos"
    EstadoPedido.COMPLETADO -> "Completados"
    EstadoPedido.CANCELADO -> "Cancelados"
}

// Texto del botón según lo que sigue. null = ya no hay botón.
private fun EstadoPedido.textoBotonAvanzar(): String? = when (this) {
    EstadoPedido.PENDIENTE -> "Preparar"
    EstadoPedido.EN_PREPARACION -> "Marcar listo"
    EstadoPedido.LISTO -> "Entregar"
    EstadoPedido.COMPLETADO, EstadoPedido.CANCELADO -> null
}

private fun coloresOrigen(origen: OrigenPedido): Pair<Color, Color> = when (origen) {
    OrigenPedido.MOSTRADOR -> BgCream to DarkBrown
    OrigenPedido.DIDI_FOOD -> Color(0xFFFFF3E0) to Color(0xFFE65100)
    OrigenPedido.UBER_EATS -> Color(0xFFE8F5E9) to Color(0xFF1B5E20)
    OrigenPedido.RAPPI -> Color(0xFFFCE4EC) to Color(0xFFC2185B)
}

// ---------- Componentes pequeños ----------

@Composable
private fun ChipEstado(texto: String, conteo: Int, seleccionado: Boolean, onClick: () -> Unit) {
    val forma = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(forma)
            .background(if (seleccionado) Terracotta else CardBg)
            .border(1.dp, if (seleccionado) Terracotta else LightBorder, forma)
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = texto,
            fontSize = 13.sp,
            fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Medium,
            color = if (seleccionado) Color.White else DarkBrown
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(if (seleccionado) Color.White.copy(alpha = 0.25f) else BgCream)
                .padding(horizontal = 7.dp, vertical = 1.dp)
        ) {
            Text(
                text = "$conteo",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (seleccionado) Color.White else TextGray
            )
        }
    }
}

@Composable
private fun ChipOrigen(texto: String, seleccionado: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (seleccionado) DarkBrown else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = texto,
            fontSize = 12.sp,
            fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal,
            color = if (seleccionado) Color.White else TextGray
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
        Text(texto, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
    }
}

@Composable
private fun BannerMensaje(
    mensaje: MensajePedidos,
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
        Text(
            text = mensaje.texto,
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Icon(Icons.Outlined.Close, contentDescription = "Cerrar", tint = color, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun EstadoVacio(texto: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFFF3EBE1)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.ReceiptLong, contentDescription = null, tint = Terracotta, modifier = Modifier.size(34.dp))
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(texto, fontSize = 15.sp, color = TextGray, textAlign = TextAlign.Center)
    }
}
