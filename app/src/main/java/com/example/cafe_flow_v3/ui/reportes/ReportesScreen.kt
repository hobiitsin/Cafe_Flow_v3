package com.example.cafe_flow_v3.ui.reportes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cafe_flow_v3.ui.AppViewModelProvider
import com.example.cafe_flow_v3.ui.theme.*

@Composable
fun ReportesScreen(
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReportesViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsState()

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
            IconButton(onClick = onVolver) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Regresar", tint = DarkBrown)
            }
            Column {
                Text("Reportes", fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Serif, color = DarkBrown)
                Text("Solo cuentan los pedidos completados", fontSize = 13.sp, color = TextGray)
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {

            Spacer(modifier = Modifier.height(12.dp))

            // Por día ultimos 7 dias
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChipOpcion("Por día", state.periodo == PeriodoReporte.DIA) {
                    viewModel.seleccionarPeriodo(PeriodoReporte.DIA)
                }
                ChipOpcion("Últimos 7 días", state.periodo == PeriodoReporte.SEMANA) {
                    viewModel.seleccionarPeriodo(PeriodoReporte.SEMANA)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Selector de fecha
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBg)
                    .border(1.dp, LightBorder, RoundedCornerShape(12.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.periodo == PeriodoReporte.DIA) {
                    IconButton(onClick = viewModel::diaAnterior) {
                        Icon(Icons.Outlined.ChevronLeft, contentDescription = "Día anterior", tint = DarkBrown)
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }
                Text(
                    text = state.etiquetaPeriodo,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkBrown,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 12.dp)
                )
                if (state.periodo == PeriodoReporte.DIA) {
                    IconButton(onClick = viewModel::diaSiguiente, enabled = state.puedeAvanzar) {
                        Icon(
                            Icons.Outlined.ChevronRight,
                            contentDescription = "Día siguiente",
                            tint = if (state.puedeAvanzar) DarkBrown else TextGray.copy(alpha = 0.3f)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }

            if (state.cargando) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Terracotta)
                }
                return@Column
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tarjetas de resumen
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TarjetaMetrica(
                    icono = Icons.Outlined.Payments,
                    titulo = "Ventas",
                    valor = state.ventasTotales,
                    modifier = Modifier.weight(1f)
                )
                TarjetaMetrica(
                    icono = Icons.Outlined.Receipt,
                    titulo = "Pedidos",
                    valor = "${state.pedidos}",
                    modifier = Modifier.weight(1f)
                )
                TarjetaMetrica(
                    icono = Icons.Outlined.ConfirmationNumber,
                    titulo = "Ticket prom.",
                    valor = state.ticketPromedio,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Grafica
            Seccion(titulo = state.tituloGrafica) {
                if (state.pedidos == 0) {
                    SinDatos()
                } else {
                    GraficaBarras(state.grafica)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Por plataforma
            Seccion(titulo = "Por plataforma") {
                if (state.plataformas.isEmpty()) {
                    SinDatos()
                } else {
                    state.plataformas.forEachIndexed { index, fila ->
                        if (index > 0) HorizontalDivider(color = LightBorder)
                        FilaConBarra(fila = fila, posicion = null)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Más vendidos
            Seccion(titulo = "Productos más vendidos") {
                if (state.productos.isEmpty()) {
                    SinDatos()
                } else {
                    state.productos.forEachIndexed { index, fila ->
                        if (index > 0) HorizontalDivider(color = LightBorder)
                        FilaConBarra(fila = fila, posicion = index + 1)
                    }
                }
            }
        }
    }
}

// Componentes

@Composable
private fun Seccion(titulo: String, contenido: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(titulo, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, color = DarkBrown)
            Spacer(modifier = Modifier.height(12.dp))
            contenido()
        }
    }
}

@Composable
private fun TarjetaMetrica(
    icono: ImageVector,
    titulo: String,
    valor: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icono, contentDescription = null, tint = Terracotta, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(valor, fontSize = 17.sp, fontWeight = FontWeight.Black, color = DarkBrown, maxLines = 1)
            Text(titulo, fontSize = 11.sp, color = TextGray, maxLines = 1)
        }
    }
}

@Composable
private fun GraficaBarras(barras: List<BarraReporte>) {
    Column {
        // Barras
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            barras.forEach { barra ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .fillMaxHeight(barra.altura)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (barra.destacada) Terracotta else Color(0xFFEADBCE))
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        // Etiquetas y montos
        Row(modifier = Modifier.fillMaxWidth()) {
            barras.forEach { barra ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(barra.etiqueta, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = DarkBrown)
                    Text(barra.valor, fontSize = 9.sp, color = TextGray, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun FilaConBarra(fila: FilaReporte, posicion: Int?) {
    Column(modifier = Modifier.padding(vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (posicion != null) {
                Text(
                    text = "$posicion",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = Terracotta,
                    modifier = Modifier.width(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(fila.titulo, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkBrown)
                Text(fila.detalle, fontSize = 12.sp, color = TextGray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(fila.total, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkBrown)
                Text("${fila.porcentaje}%", fontSize = 11.sp, color = TextGray)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(LightBorder)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fila.proporcion.coerceAtLeast(0.02f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(Terracotta)
            )
        }
    }
}

@Composable
private fun SinDatos() {
    Text(
        text = "Sin ventas completadas en este periodo",
        fontSize = 13.sp,
        color = TextGray,
        modifier = Modifier.padding(vertical = 8.dp)
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