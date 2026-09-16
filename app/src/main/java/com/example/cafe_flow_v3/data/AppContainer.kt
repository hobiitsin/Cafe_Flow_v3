package com.example.cafe_flow_v3.data

import android.content.Context
import com.example.cafe_flow_v3.data.repository.AuthRepository
import com.example.cafe_flow_v3.data.repository.PedidoRepository
import com.example.cafe_flow_v3.data.repository.ProductoRepository
import com.example.cafe_flow_v3.data.repository.ReporteRepository

/**
 * "Caja de herramientas" de la app: crea la base y los repositorios UNA sola vez.
 * Los ViewModels los piden de aquí en lugar de crearlos cada uno por su lado.
 */
class AppContainer(context: Context) {

    val database: AppDatabase = AppDatabase.obtener(context)

    val authRepository: AuthRepository by lazy {
        AuthRepository(database.usuarioDao())
    }

    val productoRepository: ProductoRepository by lazy {
        ProductoRepository(database.categoriaDao(), database.productoDao())
    }

    val pedidoRepository: PedidoRepository by lazy {
        PedidoRepository(database)
    }

    val reporteRepository: ReporteRepository by lazy {
        ReporteRepository(database.reporteDao(), database.pedidoDao())
    }
}
