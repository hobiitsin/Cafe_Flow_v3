package com.example.cafe_flow_v3.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.cafe_flow_v3.data.dao.CategoriaDao
import com.example.cafe_flow_v3.data.dao.PedidoDao
import com.example.cafe_flow_v3.data.dao.ProductoDao
import com.example.cafe_flow_v3.data.dao.ReporteDao
import com.example.cafe_flow_v3.data.dao.UsuarioDao
import com.example.cafe_flow_v3.data.entity.Categoria
import com.example.cafe_flow_v3.data.entity.DetallePedido
import com.example.cafe_flow_v3.data.entity.Pedido
import com.example.cafe_flow_v3.data.entity.Producto
import com.example.cafe_flow_v3.data.entity.Usuario

@Database(
    entities = [
        Usuario::class,
        Categoria::class,
        Producto::class,
        Pedido::class,
        DetallePedido::class
    ],
    // Súbele +1 cada vez que cambies una entidad (agregar/quitar columnas, etc.)
    version = 1,
    // No guardamos el historial del esquema (solo sirve para migraciones avanzadas)
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun categoriaDao(): CategoriaDao
    abstract fun productoDao(): ProductoDao
    abstract fun pedidoDao(): PedidoDao
    abstract fun reporteDao(): ReporteDao

    companion object {
        private const val NOMBRE_BD = "cafeflow.db"

        // Singleton: toda la app comparte UNA sola conexión a la base
        @Volatile
        private var instancia: AppDatabase? = null

        fun obtener(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    NOMBRE_BD
                )
                    // SOLO PARA DESARROLLO: si cambia la versión, borra todo y crea las tablas de nuevo.
                    // Los datos de prueba se vuelven a cargar solos.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instancia = it }
            }
    }
}
