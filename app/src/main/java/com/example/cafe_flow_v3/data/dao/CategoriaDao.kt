package com.example.cafe_flow_v3.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.cafe_flow_v3.data.entity.Categoria
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoriaDao {

    @Insert
    suspend fun insertar(categoria: Categoria): Long

    @Insert
    suspend fun insertarTodas(categorias: List<Categoria>): List<Long>

    @Update
    suspend fun actualizar(categoria: Categoria)

    // Falla si la categoría todavía tiene productos (por el RESTRICT de la entidad)
    @Delete
    suspend fun eliminar(categoria: Categoria)

    // Flow = la pantalla se actualiza sola cuando cambia la tabla
    @Query("SELECT * FROM categorias ORDER BY nombre")
    fun observarTodas(): Flow<List<Categoria>>
}
