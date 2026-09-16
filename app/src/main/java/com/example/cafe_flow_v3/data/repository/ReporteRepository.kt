package com.example.cafe_flow_v3.data.repository

import com.example.cafe_flow_v3.data.dao.PedidoDao
import com.example.cafe_flow_v3.data.dao.ReporteDao
import com.example.cafe_flow_v3.data.model.PedidoConDetalles
import com.example.cafe_flow_v3.data.model.VentaPorHora
import com.example.cafe_flow_v3.data.model.VentaPorOrigen
import com.example.cafe_flow_v3.data.model.VentaPorProducto
import com.example.cafe_flow_v3.util.Fechas
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class ResumenDashboard(
    val ventasHoyCentavos: Long = 0,
    val variacionVsAyer: Int? = null, // +18 = 18% más que ayer. null si ayer no hubo ventas.
    val pendientes: Int = 0,
    val completadosHoy: Int = 0,
    val externosHoy: Int = 0,
    val ventasPorHora: List<VentaPorHora> = emptyList(),
    val recientes: List<PedidoConDetalles> = emptyList()
)

class ReporteRepository(
    private val reporteDao: ReporteDao,
    private val pedidoDao: PedidoDao
) {

    /**
     * Junta las 7 consultas del dashboard en un solo Flow.
     * Si cualquiera cambia se emite un resumen nuevo.
     */
    fun resumenDelDia(): Flow<ResumenDashboard> {
        val inicioHoy = Fechas.inicioDelDia()
        val finHoy = Fechas.finDelDia()
        val inicioAyer = Fechas.inicioDelDia(diasAtras = 1)
        val finAyer = Fechas.finDelDia(diasAtras = 1)

        // combine acepta maximo 5 flows con tipos distintos
        val metricas = combine(
            reporteDao.observarVentasEntre(inicioHoy, finHoy),
            reporteDao.observarVentasEntre(inicioAyer, finAyer),
            reporteDao.observarPendientes(),
            reporteDao.observarCompletadosEntre(inicioHoy, finHoy),
            reporteDao.observarExternosEntre(inicioHoy, finHoy)
        ) { ventasHoy, ventasAyer, pendientes, completados, externos ->
            ResumenDashboard(
                ventasHoyCentavos = ventasHoy,
                variacionVsAyer = calcularVariacion(ventasHoy, ventasAyer),
                pendientes = pendientes,
                completadosHoy = completados,
                externosHoy = externos
            )
        }

        return combine(
            metricas,
            reporteDao.observarVentasPorHora(inicioHoy, finHoy),
            pedidoDao.observarRecientes(limite = 3)
        ) { resumen, porHora, recientes ->
            resumen.copy(ventasPorHora = porHora, recientes = recientes)
        }
    }

    // dias Atras = 0 es hoy 1 es ayer

    fun ventasDelDia(diasAtras: Int = 0): Flow<Long> =
        reporteDao.observarVentasEntre(Fechas.inicioDelDia(diasAtras), Fechas.finDelDia(diasAtras))

    fun pedidosDelDia(diasAtras: Int = 0): Flow<Int> =
        reporteDao.observarCompletadosEntre(Fechas.inicioDelDia(diasAtras), Fechas.finDelDia(diasAtras))

    fun ventasPorProducto(diasAtras: Int = 0): Flow<List<VentaPorProducto>> =
        reporteDao.observarVentasPorProducto(Fechas.inicioDelDia(diasAtras), Fechas.finDelDia(diasAtras))

    fun ventasPorPlataforma(diasAtras: Int = 0): Flow<List<VentaPorOrigen>> =
        reporteDao.observarVentasPorOrigen(Fechas.inicioDelDia(diasAtras), Fechas.finDelDia(diasAtras))

    private fun calcularVariacion(hoy: Long, ayer: Long): Int? =
        if (ayer == 0L) null else ((hoy - ayer) * 100 / ayer).toInt()
}
