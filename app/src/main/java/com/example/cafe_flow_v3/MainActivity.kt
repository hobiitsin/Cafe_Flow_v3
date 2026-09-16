package com.example.cafe_flow_v3


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CafeFlowApp()
        }
    }
}

@Composable
fun CafeFlowApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {

        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("dashboard") {
                        // Quita el login del historial: "atrás" no regresa al login
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("dashboard") {
            DashboardScreen(
                onCerrarSesion = {
                    navController.navigate("login") {
                        // Quita el dashboard: "atrás" no regresa sin iniciar sesión
                        popUpTo("dashboard") { inclusive = true }
                    }
                }
            )
        }
    }
}
