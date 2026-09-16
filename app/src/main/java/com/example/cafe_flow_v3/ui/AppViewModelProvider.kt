package com.example.cafe_flow_v3.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cafe_flow_v3.CafeFlowApplication
import com.example.cafe_flow_v3.ui.dashboard.DashboardViewModel
import com.example.cafe_flow_v3.ui.login.LoginViewModel
import com.example.cafe_flow_v3.ui.pedidos.PedidosViewModel
import com.example.cafe_flow_v3.ui.ventas.VentasViewModel

/**
 * "Fábrica" de ViewModels.
 * Un ViewModel no puede recibir parámetros en su constructor a menos que
 * le digas a Android cómo crearlo. Aquí le decimos: toma los repositorios
 * del AppContainer y pásaselos.
 *
 * Cada pantalla nueva agrega aquí su initializer { ... }.
 */
object AppViewModelProvider {

    val Factory = viewModelFactory {

        initializer {
            LoginViewModel(
                authRepository = cafeFlowApp().container.authRepository
            )
        }

        initializer {
            DashboardViewModel(
                reporteRepository = cafeFlowApp().container.reporteRepository,
                authRepository = cafeFlowApp().container.authRepository
            )
        }

        initializer {
            VentasViewModel(
                productoRepository = cafeFlowApp().container.productoRepository,
                pedidoRepository = cafeFlowApp().container.pedidoRepository,
                authRepository = cafeFlowApp().container.authRepository
            )
        }

        initializer {
            PedidosViewModel(
                pedidoRepository = cafeFlowApp().container.pedidoRepository
            )
        }
    }
}

// Obtiene nuestra CafeFlowApplication (la que tiene el container)
fun CreationExtras.cafeFlowApp(): CafeFlowApplication =
    this[APPLICATION_KEY] as CafeFlowApplication
