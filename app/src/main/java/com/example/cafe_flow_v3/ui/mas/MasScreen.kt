package com.example.cafe_flow_v3.ui.mas

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cafe_flow_v3.ui.productos.ProductosScreen
import com.example.cafe_flow_v3.ui.theme.*

private val ErrorRed = Color(0xFFC62828)

// Secciones dentro de la pestaña "Más". Reportes se agrega en el paso 8.5.
private enum class SeccionMas { MENU, PRODUCTOS }

@Composable
fun MasScreen(
    nombreUsuario: String,
    onVerPedidos: () -> Unit,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier
) {
    var seccion by rememberSaveable { mutableStateOf(SeccionMas.MENU) }

    // Dentro de una sección, "atrás" regresa al menú
    BackHandler(enabled = seccion != SeccionMas.MENU) {
        seccion = SeccionMas.MENU
    }

    when (seccion) {
        SeccionMas.MENU -> MenuMas(
            nombreUsuario = nombreUsuario,
            onProductos = { seccion = SeccionMas.PRODUCTOS },
            onVerPedidos = onVerPedidos,
            onCerrarSesion = onCerrarSesion,
            modifier = modifier
        )
        SeccionMas.PRODUCTOS -> ProductosScreen(
            onVolver = { seccion = SeccionMas.MENU },
            modifier = modifier
        )
    }
}

@Composable
private fun MenuMas(
    nombreUsuario: String,
    onProductos: () -> Unit,
    onVerPedidos: () -> Unit,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("Más", fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Serif, color = DarkBrown)

        Spacer(modifier = Modifier.height(16.dp))

        // Tarjeta del usuario
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF8D6E63)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = nombreUsuario.take(1).uppercase().ifEmpty { "?" },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(nombreUsuario.ifBlank { "Usuario" }, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = DarkBrown)
                    Text("Sesión iniciada", fontSize = 12.sp, color = TextGray)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text("Administración", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextGray)
        Spacer(modifier = Modifier.height(8.dp))

        OpcionMenu(
            icono = Icons.Outlined.Coffee,
            titulo = "Productos",
            subtitulo = "Agregar, editar y ocultar del menú",
            onClick = onProductos
        )
        OpcionMenu(
            icono = Icons.Outlined.LocalShipping,
            titulo = "Pedidos externos",
            subtitulo = "En Pedidos, filtra por DiDi, Uber o Rappi",
            onClick = onVerPedidos
        )
        OpcionMenu(
            icono = Icons.Outlined.BarChart,
            titulo = "Reportes",
            subtitulo = "Ventas por día, producto y plataforma",
            proximamente = true,
            onClick = {}
        )

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedButton(
            onClick = onCerrarSesion,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
        ) {
            Icon(Icons.Outlined.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cerrar sesión", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun OpcionMenu(
    icono: ImageVector,
    titulo: String,
    subtitulo: String,
    onClick: () -> Unit,
    proximamente: Boolean = false
) {
    val forma = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(forma)
            .background(CardBg)
            .clickable(enabled = !proximamente, onClick = onClick)
            .padding(14.dp)
            .alpha(if (proximamente) 0.55f else 1f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BgCream),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, contentDescription = null, tint = Terracotta, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DarkBrown)
            Text(subtitulo, fontSize = 12.sp, color = TextGray)
        }
        if (proximamente) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(OrangeBadgeBg)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("Próximamente", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OrangeBadgeText)
            }
        } else {
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TextGray)
        }
    }
}
