package com.example.cafe_flow_v3

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cafe_flow_v3.ui.AppViewModelProvider
import com.example.cafe_flow_v3.ui.login.LoginViewModel
import com.example.cafe_flow_v3.ui.theme.*

private val ErrorRed = Color(0xFFC62828)

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    // Cada vez que el ViewModel cambia el estado, la pantalla se redibuja
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    // Cuando el login sale bien, navegamos UNA sola vez
    LaunchedEffect(uiState.loginExitoso) {
        if (uiState.loginExitoso) {
            viewModel.onNavegacionHecha()
            onLoginSuccess()
        }
    }

    val colorBorde = if (uiState.error != null) ErrorRed else LightBorder

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCream)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Logo y Nombre
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Terracotta),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Coffee,
                    contentDescription = null,
                    tint = BgCream,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "CafeFlow",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Serif,
                color = DarkBrown
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Ilustración central
        Box(
            modifier = Modifier
                .size(170.dp)
                .clip(CircleShape)
                .background(Color(0xFFF3EBE1)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Coffee,
                contentDescription = null,
                tint = Terracotta.copy(alpha = 0.5f),
                modifier = Modifier.size(72.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Textos de Bienvenida
        Text(
            text = "¡Bienvenido de nuevo!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            color = DarkBrown
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Inicia sesión para gestionar tus pedidos",
            fontSize = 14.sp,
            color = TextGray
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Campo Email
        Text(
            text = "Correo electrónico",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = DarkBrown,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = uiState.email,
            onValueChange = viewModel::onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.cargando,
            shape = RoundedCornerShape(12.dp),
            placeholder = { Text("barista@cafeflow.com", color = TextGray.copy(alpha = 0.6f)) },
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = null, tint = TextGray)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Terracotta,
                unfocusedBorderColor = colorBorde,
                focusedContainerColor = CardBg,
                unfocusedContainerColor = CardBg,
                disabledContainerColor = CardBg
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next // el teclado muestra "Siguiente"
            )
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Campo Contraseña
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Contraseña",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = DarkBrown
            )
            Text(
                text = "¿Olvidé mi contraseña?",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Terracotta,
                modifier = Modifier.clickable { }
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.cargando,
            shape = RoundedCornerShape(12.dp),
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null, tint = TextGray)
            },
            trailingIcon = {
                IconButton(onClick = viewModel::onTogglePasswordVisible) {
                    Icon(
                        imageVector = if (uiState.passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (uiState.passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                        tint = TextGray
                    )
                }
            },
            visualTransformation = if (uiState.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Terracotta,
                unfocusedBorderColor = colorBorde,
                focusedContainerColor = CardBg,
                unfocusedContainerColor = CardBg,
                disabledContainerColor = CardBg
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done // el teclado muestra "Listo"
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    viewModel.iniciarSesion()
                }
            )
        )

        // Mensaje de error
        uiState.error?.let { mensaje ->
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = mensaje,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = ErrorRed,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Botón Iniciar Sesión
        Button(
            onClick = {
                focusManager.clearFocus()
                viewModel.iniciarSesion()
            },
            enabled = !uiState.cargando,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Terracotta,
                disabledContainerColor = Terracotta.copy(alpha = 0.6f)
            )
        ) {
            if (uiState.cargando) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Text(
                    text = "Iniciar sesión",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Registro (lo conectamos en el paso 8)
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "¿No tienes una cuenta? ", fontSize = 13.sp, color = TextGray)
            Text(
                text = "Registrarse",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Terracotta,
                modifier = Modifier.clickable { }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Ayuda para la demo (bórralo cuando entregues)
        Text(
            text = "Demo: barista@cafeflow.com · 12345678",
            fontSize = 11.sp,
            color = TextGray.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}