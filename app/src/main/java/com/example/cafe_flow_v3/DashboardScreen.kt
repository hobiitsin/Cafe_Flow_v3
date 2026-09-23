package com.example.cafe_flow_v3

import com.example.cafe_flow_v3.ui.mas.MasScreen
import com.example.cafe_flow_v3.ui.inventario.InventarioScreen
import com.example.cafe_flow_v3.ui.pedidos.PedidosScreen
import com.example.cafe_flow_v3.ui.ventas.VentasScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.cafe_flow_v3.data.entity.EstadoPedido
import com.example.cafe_flow_v3.ui.AppViewModelProvider
import com.example.cafe_flow_v3.ui.common.etiqueta
import com.example.cafe_flow_v3.ui.dashboard.DashboardUiState
import com.example.cafe_flow_v3.ui.dashboard.DashboardViewModel
import com.example.cafe_flow_v3.ui.theme.*

// Colores extra para los estados que no estaban en el tema
private val RedBadgeBg = Color(0xFFFFEBEE)
private val RedBadgeText = Color(0xFFC62828)
private val BlueBadgeBg = Color(0xFFE3F2FD)
private val BlueBadgeText = Color(0xFF1565C0)
private val TealBadgeBg = Color(0xFFE0F2F1)
private val TealBadgeText = Color(0xFF00695C)

@Composable
fun DashboardScreen(
    onCerrarSesion: () -> Unit,
    viewModel: DashboardViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    // rememberSaveable: si giras el teléfono, no se pierde la pestaña
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        containerColor = BgCream,
        bottomBar = {
            NavigationBar(
                containerColor = CardBg,
                tonalElevation = 8.dp
            ) {
                val items = listOf(
                    Triple("Dashboard", Icons.Outlined.BarChart, 0),
                    Triple("Ventas", Icons.Outlined.ShoppingCart, 1),
                    Triple("Pedidos", Icons.Outlined.ListAlt, 2),
                    Triple("Inventario", Icons.Outlined.Inventory2, 3),
                    Triple("Más", Icons.Outlined.MoreHoriz, 4)
                )

                items.forEach { (title, icon, index) ->
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = title) },
                        label = { Text(title, fontSize = 10.sp) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Terracotta,
                            selectedTextColor = Terracotta,
                            unselectedIconColor = TextGray,
                            unselectedTextColor = TextGray,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            // El botón solo aparece en el Dashboard y te manda a la pestaña Ventas
            if (selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { selectedTab = 1 },
                    containerColor = Terracotta,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(24.dp),
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Nueva venta", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    ) { innerPadding ->
        val contenidoModifier = Modifier.padding(innerPadding)

        // Cada pestaña muestra su propio contenido
        when (selectedTab) {
            0 -> DashboardContent(
                state = uiState,
                onVerPedidos = { selectedTab = 2 },
                onVerReporte = { selectedTab = 4 },
                modifier = contenidoModifier
            )
            1 -> VentasScreen(
                onVerPedidos = { selectedTab = 2 },
                modifier = contenidoModifier
            )

            2 -> PedidosScreen(modifier = contenidoModifier)

            3 -> InventarioScreen(modifier = contenidoModifier)

            4 -> MasScreen(
                nombreUsuario = uiState.nombreUsuario,
                onVerPedidos = { selectedTab = 2 },
                onCerrarSesion = {
                    viewModel.cerrarSesion()
                    onCerrarSesion()
                },
                modifier = contenidoModifier
            )
        }
    }
}

// ---------- Pestaña Dashboard: ahora con datos reales ----------

@Composable
private fun DashboardContent(
    state: DashboardUiState,
    onVerPedidos: () -> Unit,
    onVerReporte: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Mientras llega la primera respuesta de la base
    if (state.cargando) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Terracotta)
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Header Top
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Terracotta),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Coffee,
                            contentDescription = null,
                            tint = BgCream,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CafeFlow",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Serif,
                        color = DarkBrown
                    )
                }

                // Avatar con la inicial del usuario
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF8D6E63)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.nombreUsuario.take(1).uppercase().ifEmpty { "?" },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }

        // Título Saludo
        item {
            Text(
                text = "${state.saludo}, ${state.nombreUsuario.ifBlank { "Barista" }}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Serif,
                color = DarkBrown
            )
            Text(
                text = "Aquí está el resumen del café para hoy.",
                fontSize = 13.sp,
                color = TextGray
            )
        }

        // Grid de Métricas (2x2)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.ShoppingCart,
                        tag = state.variacion,
                        tagBg = if (state.variacionPositiva) GreenBadgeBg else RedBadgeBg,
                        tagColor = if (state.variacionPositiva) GreenBadgeText else RedBadgeText,
                        label = "Ventas del día",
                        value = state.ventasHoy
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Schedule,
                        tag = if (state.pendientes > 0) "Urgente" else null,
                        tagBg = OrangeBadgeBg,
                        tagColor = OrangeBadgeText,
                        label = "Pedidos pendientes",
                        value = state.pendientes.toString()
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.CheckCircleOutline,
                        label = "Pedidos completados",
                        value = state.completados.toString()
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.LocalShipping,
                        label = "Pedidos externos",
                        value = state.externos.toString()
                    )
                }
            }
        }

        // Gráfico Ventas por hora
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    EncabezadoTarjeta(
                        titulo = "Ventas por hora",
                        accion = "Ver reporte",
                        onAccion = onVerReporte
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        state.barras.forEach { barra ->
                            BarItem(
                                label = barra.etiqueta,
                                heightWeight = barra.altura,
                                isHighlight = barra.destacada
                            )
                        }
                    }
                }
            }
        }

        // Pedidos Recientes
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    EncabezadoTarjeta(
                        titulo = "Pedidos recientes",
                        accion = "Ver todos",
                        onAccion = onVerPedidos
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (state.recientes.isEmpty()) {
                        Text(
                            text = "Todavía no hay pedidos. ¡Registra la primera venta!",
                            fontSize = 13.sp,
                            color = TextGray
                        )
                    }

                    state.recientes.forEachIndexed { index, pedido ->
                        if (index > 0) {
                            HorizontalDivider(
                                color = LightBorder,
                                thickness = 0.8.dp,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                        val (fondo, texto) = coloresEstado(pedido.estado)
                        OrderItem(
                            icon = if (pedido.esExterno) Icons.Outlined.LocalShipping else Icons.Outlined.Coffee,
                            id = pedido.folio,
                            status = pedido.estado.etiqueta(),
                            statusBg = fondo,
                            statusColor = texto,
                            description = pedido.descripcion,
                            price = pedido.total
                        )
                    }
                }
            }
        }

        // Espacio para que el botón flotante no tape el último pedido
        item { Spacer(modifier = Modifier.height(70.dp)) }
    }
}

// Título de tarjeta + enlace a la derecha ("Ver todos", "Ver reporte")
@Composable
private fun EncabezadoTarjeta(titulo: String, accion: String, onAccion: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = titulo,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            color = DarkBrown
        )
        Text(
            text = accion,
            fontSize = 12.sp,
            color = Terracotta,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable(onClick = onAccion)
        )
    }
}

// Color de la etiqueta según el estado del pedido: (fondo, texto)
fun coloresEstado(estado: EstadoPedido): Pair<Color, Color> = when (estado) {
    EstadoPedido.PENDIENTE -> OrangeBadgeBg to OrangeBadgeText
    EstadoPedido.EN_PREPARACION -> BlueBadgeBg to BlueBadgeText
    EstadoPedido.LISTO -> TealBadgeBg to TealBadgeText
    EstadoPedido.COMPLETADO -> GreenBadgeBg to GreenBadgeText
    EstadoPedido.CANCELADO -> RedBadgeBg to RedBadgeText
}

// ---------- Componentes (sin cambios de diseño) ----------

@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    tag: String? = null,
    tagBg: Color = Color.Transparent,
    tagColor: Color = Color.Transparent,
    label: String,
    value: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BgCream),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = DarkBrown, modifier = Modifier.size(18.dp))
                }
                if (tag != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(tagBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = tag, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = tagColor)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = label, fontSize = 11.sp, color = TextGray)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = DarkBrown)
        }
    }
}

@Composable
fun BarItem(label: String, heightWeight: Float, isHighlight: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.fillMaxHeight()
    ) {
        Box(
            modifier = Modifier
                .width(16.dp)
                .fillMaxHeight(heightWeight)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isHighlight) Terracotta else Color(0xFFEADBCE))
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, fontSize = 11.sp, color = TextGray)
    }
}

@Composable
fun OrderItem(
    icon: ImageVector,
    id: String,
    status: String,
    statusBg: Color,
    statusColor: Color,
    description: String,
    price: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BgCream),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = DarkBrown, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = id, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkBrown)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(statusBg)
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(text = status, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = description, fontSize = 11.sp, color = TextGray, maxLines = 1)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = price, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkBrown)
    }
}

// Pantalla temporal para las pestañas que todavía no construimos (paso 8)
@Composable
fun PantallaEnConstruccion(
    titulo: String,
    descripcion: String,
    icono: ImageVector,
    modifier: Modifier = Modifier,
    accion: (@Composable () -> Unit)? = null // botón opcional debajo (ej. Cerrar sesión)
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(Color(0xFFF3EBE1)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, contentDescription = null, tint = Terracotta, modifier = Modifier.size(40.dp))
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = titulo,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Serif,
            color = DarkBrown
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = descripcion,
            fontSize = 14.sp,
            color = TextGray,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(OrangeBadgeBg)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text("En construcción", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OrangeBadgeText)
        }
        if (accion != null) {
            Spacer(modifier = Modifier.height(32.dp))
            accion()
        }
    }
}
