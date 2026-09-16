package com.example.cafe_flow_v3.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.cafe_flow_v3.data.model.VentaPorHora
import com.example.cafe_flow_v3.data.model.VentaPorOrigen
import com.example.cafe_flow_v3.data.model.VentaPorProducto
import kotlinx.coroutines.flow.Flow

// Consultas de solo lectura para el Dashboard y la pantalla de Reportes.
//
// :inicio y :fin son milisegundos. El repositorio calcula el rango
// (inicio y fin de "hoy", de "ayer", etc.) y aquí solo filtramos.
//
// Los estados van como texto ('COMPLETADO') porque así los guarda Room;
// tienen que coincidir EXACTO con los nombres del enum.

@Dao
interface ReporteDao {

    // ---------- Tarjetas del dashboard ----------

    // "Ventas del día". COALESCE evita null cuando todavía no hay ventas.
    // Llamándola con el rango de ayer sacamos el "+18%".
    @Query(
        """
        SELECT COALESCE(SUM(total_centavos), 0) FROM pedidos
        WHERE estado = 'COMPLETADO'
          AND fecha_hora BETWEEN :inicio AND :fin
        """
    )
    fun observarVentasEntre(inicio: Long, fin: Long): Flow<Long>

    // "Pedidos pendientes": todo lo que no se ha entregado, sin importar el día
    @Query(
        """
        SELECT COUNT(*) FROM pedidos
        WHERE estado IN ('PENDIENTE', 'EN_PREPARACION', 'LISTO')
        """
    )
    fun observarPendientes(): Flow<Int>

    // "Pedidos completados" (también sirve para "Número de pedidos" en Reportes)
    @Query(
        """
        SELECT COUNT(*) FROM pedidos
        WHERE estado = 'COMPLETADO'
          AND fecha_hora BETWEEN :inicio AND :fin
        """
    )
    fun observarCompletadosEntre(inicio: Long, fin: Long): Flow<Int>

    // "Pedidos externos" (sin contar cancelados)
    @Query(
        """
        SELECT COUNT(*) FROM pedidos
        WHERE origen != 'MOSTRADOR'
          AND estado != 'CANCELADO'
          AND fecha_hora BETWEEN :inicio AND :fin
        """
    )
    fun observarExternosEntre(inicio: Long, fin: Long): Flow<Int>

    // ---------- Gráfica y reportes ----------

    // "Ventas por hora": convierte los milisegundos a hora local (0-23) y agrupa
    @Query(
        """
        SELECT CAST(strftime('%H', fecha_hora / 1000, 'unixepoch', 'localtime') AS INTEGER) AS hora,
               SUM(total_centavos) AS totalCentavos
        FROM pedidos
        WHERE estado = 'COMPLETADO'
          AND fecha_hora BETWEEN :inicio AND :fin
        GROUP BY hora
        ORDER BY hora
        """
    )
    fun observarVentasPorHora(inicio: Long, fin: Long): Flow<List<VentaPorHora>>

    // "Ventas por producto": usa el precio guardado en el detalle, no el precio actual
    @Query(
        """
        SELECT p.id AS productoId,
               p.nombre AS nombre,
               SUM(d.cantidad) AS cantidad,
               SUM(d.cantidad * d.precio_unitario_centavos) AS totalCentavos
        FROM detalle_pedido d
        JOIN pedidos pe ON pe.id = d.pedido_id
        JOIN productos p ON p.id = d.producto_id
        WHERE pe.estado = 'COMPLETADO'
          AND pe.fecha_hora BETWEEN :inicio AND :fin
        GROUP BY p.id, p.nombre
        ORDER BY totalCentavos DESC
        """
    )
    fun observarVentasPorProducto(inicio: Long, fin: Long): Flow<List<VentaPorProducto>>

    // "Ventas por plataforma": Mostrador vs DiDi vs Uber vs Rappi
    @Query(
        """
        SELECT origen,
               COUNT(*) AS pedidos,
               SUM(total_centavos) AS totalCentavos
        FROM pedidos
        WHERE estado = 'COMPLETADO'
          AND fecha_hora BETWEEN :inicio AND :fin
        GROUP BY origen
        ORDER BY totalCentavos DESC
        """
    )
    fun observarVentasPorOrigen(inicio: Long, fin: Long): Flow<List<VentaPorOrigen>>
}
