package com.example.cafe_flow_v3.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.cafe_flow_v3.data.entity.Producto
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductoDao {

    // ---------- CRUD (pantalla Productos) ----------

    @Insert
    suspend fun insertar(producto: Producto): Long

    @Insert
    suspend fun insertarTodos(productos: List<Producto>): List<Long>

    @Update
    suspend fun actualizar(producto: Producto)

    // Falla si el producto ya aparece en algún pedido (RESTRICT).
    // En ese caso mejor marcarlo como disponible = false.
    @Delete
    suspend fun eliminar(producto: Producto)

    @Query("SELECT * FROM productos WHERE id = :id")
    suspend fun buscarPorId(id: Long): Producto?

    @Query("SELECT * FROM productos ORDER BY nombre")
    fun observarTodos(): Flow<List<Producto>>

    // ---------- Menú del punto de venta ----------

    @Query("SELECT * FROM productos WHERE disponible = 1 ORDER BY nombre")
    fun observarDisponibles(): Flow<List<Producto>>

    @Query(
        """
        SELECT * FROM productos
        WHERE categoria_id = :categoriaId AND disponible = 1
        ORDER BY nombre
        """
    )
    fun observarPorCategoria(categoriaId: Long): Flow<List<Producto>>

    // ---------- Inventario ----------

    @Query("SELECT * FROM productos WHERE stock > 0 AND stock <= stock_minimo ORDER BY stock")
    fun observarBajoInventario(): Flow<List<Producto>>

    @Query("SELECT * FROM productos WHERE stock = 0 ORDER BY nombre")
    fun observarAgotados(): Flow<List<Producto>>

    // Solo descuenta si alcanza el stock.
    // Regresa cuántas filas cambió: 1 = se descontó, 0 = no había suficiente.
    @Query("UPDATE productos SET stock = stock - :cantidad WHERE id = :id AND stock >= :cantidad")
    suspend fun descontarStock(id: Long, cantidad: Int): Int

    // Para cuando se cancela un pedido o llega mercancía
    @Query("UPDATE productos SET stock = stock + :cantidad WHERE id = :id")
    suspend fun reponerStock(id: Long, cantidad: Int)
}
