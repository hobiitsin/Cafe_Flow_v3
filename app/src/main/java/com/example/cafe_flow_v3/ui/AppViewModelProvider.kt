package com.example.cafe_flow_v3.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cafe_flow_v3.CafeFlowApplication
import com.example.cafe_flow_v3.ui.dashboard.DashboardViewModel
import com.example.cafe_flow_v3.ui.inventario.InventarioViewModel
import com.example.cafe_flow_v3.ui.login.LoginViewModel
import com.example.cafe_flow_v3.ui.pedidos.PedidosViewModel
import com.example.cafe_flow_v3.ui.productos.ProductosViewModel
import com.example.cafe_flow_v3.ui.reportes.ReportesViewModel
import com.example.cafe_flow_v3.ui.ventas.VentasViewModel

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

        initializer {
            InventarioViewModel(
                productoRepository = cafeFlowApp().container.productoRepository
            )
        }

        initializer {
            ProductosViewModel(
                productoRepository = cafeFlowApp().container.productoRepository
            )
        }

        initializer {
            ReportesViewModel(
                reporteRepository = cafeFlowApp().container.reporteRepository
            )
        }
    }
}

// Obtiene CafeFlowApplication (
fun CreationExtras.cafeFlowApp(): CafeFlowApplication =
    this[APPLICATION_KEY] as CafeFlowApplication