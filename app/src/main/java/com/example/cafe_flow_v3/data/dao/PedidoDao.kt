package com.example.cafe_flow_v3.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.cafe_flow_v3.data.entity.DetallePedido
import com.example.cafe_flow_v3.data.entity.EstadoPedido
import com.example.cafe_flow_v3.data.entity.OrigenPedido
import com.example.cafe_flow_v3.data.entity.Pedido
import com.example.cafe_flow_v3.data.model.PedidoConDetalles
import kotlinx.coroutines.flow.Flow

@Dao
interface PedidoDao {

    // Se crea un pedido con los detalles y se quita stock

    @Insert
    suspend fun insertarPedido(pedido: Pedido): Long

    @Insert
    suspend fun insertarDetalles(detalles: List<DetallePedido>)

    @Query("UPDATE pedidos SET estado = :estado WHERE id = :id")
    suspend fun actualizarEstado(id: Long, estado: EstadoPedido)



    @Transaction
    @Query("SELECT * FROM pedidos WHERE id = :id")
    suspend fun buscarConDetalles(id: Long): PedidoConDetalles?

    // Dashboard: "Pedidos recientes"
    @Transaction
    @Query("SELECT * FROM pedidos ORDER BY fecha_hora DESC LIMIT :limite")
    fun observarRecientes(limite: Int): Flow<List<PedidoConDetalles>>

    // Pantalla Pedidos (pestañas Pendientes / En preparación / Listos...).
    // Más antiguos primero: el que lleva más tiempo esperando va arriba.
    @Transaction
    @Query("SELECT * FROM pedidos WHERE estado = :estado ORDER BY fecha_hora ASC")
    fun observarPorEstado(estado: EstadoPedido): Flow<List<PedidoConDetalles>>

    // Pantalla Pedidos externos, filtrando por DiDi / Uber / Rappi
    @Transaction
    @Query("SELECT * FROM pedidos WHERE origen = :origen ORDER BY fecha_hora DESC")
    fun observarPorOrigen(origen: OrigenPedido): Flow<List<PedidoConDetalles>>

    // Todos los externos juntos
    @Transaction
    @Query("SELECT * FROM pedidos WHERE origen != 'MOSTRADOR' ORDER BY fecha_hora DESC")
    fun observarExternos(): Flow<List<PedidoConDetalles>>
}
