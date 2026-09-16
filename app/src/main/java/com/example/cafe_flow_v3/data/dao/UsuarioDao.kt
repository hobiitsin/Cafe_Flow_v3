package com.example.cafe_flow_v3.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.cafe_flow_v3.data.entity.Usuario
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {

    // ABORT: si el correo ya existe, lanza una excepción (la atrapamos en el repositorio)
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(usuario: Usuario): Long

    @Update
    suspend fun actualizar(usuario: Usuario)

    // Para el login. El correo lo guardamos siempre en minúsculas desde el repositorio.
    @Query("SELECT * FROM usuarios WHERE email = :email LIMIT 1")
    suspend fun buscarPorEmail(email: String): Usuario?

    @Query("SELECT * FROM usuarios WHERE id = :id")
    suspend fun buscarPorId(id: Long): Usuario?

    @Query("SELECT * FROM usuarios ORDER BY nombre")
    fun observarTodos(): Flow<List<Usuario>>

    // Para saber si la base está vacía y hay que cargar datos de prueba
    @Query("SELECT COUNT(*) FROM usuarios")
    suspend fun contar(): Int
}
