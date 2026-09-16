package com.example.cafe_flow_v3.data.repository


import android.database.sqlite.SQLiteConstraintException
import com.example.cafe_flow_v3.data.dao.CategoriaDao
import com.example.cafe_flow_v3.data.dao.ProductoDao
import com.example.cafe_flow_v3.data.entity.Categoria
import com.example.cafe_flow_v3.data.entity.Producto
import kotlinx.coroutines.flow.Flow

class ProductoRepository(
    private val categoriaDao: CategoriaDao,
    private val productoDao: ProductoDao
) {


    fun observarCategorias(): Flow<List<Categoria>> = categoriaDao.observarTodas()

    fun observarProductos(): Flow<List<Producto>> = productoDao.observarTodos()

    // Menu del punto de venta. Sin categoria = todos los que se encuentren disponibles.
    fun observarMenu(categoriaId: Long? = null): Flow<List<Producto>> =
        if (categoriaId == null) productoDao.observarDisponibles()
        else productoDao.observarPorCategoria(categoriaId)

    fun observarBajoInventario(): Flow<List<Producto>> = productoDao.observarBajoInventario()

    fun observarAgotados(): Flow<List<Producto>> = productoDao.observarAgotados()

    // ---------- Productos ----------

    // Si id == 0 lo crea; si no lo actualiza y Regresa el id.
    suspend fun guardarProducto(producto: Producto): Result<Long> {
        val nombre = producto.nombre.trim()
        when {
            nombre.isEmpty() -> return fallo("El producto necesita un nombre")
            producto.precioCentavos <= 0 -> return fallo("El precio debe ser mayor a $0")
            producto.stock < 0 -> return fallo("El stock no puede ser negativo")
            producto.stockMinimo < 0 -> return fallo("El stock mínimo no puede ser negativo")
        }

        val limpio = producto.copy(nombre = nombre)
        return try {
            if (limpio.id == 0L) {
                Result.success(productoDao.insertar(limpio))
            } else {
                productoDao.actualizar(limpio)
                Result.success(limpio.id)
            }
        } catch (e: SQLiteConstraintException) {
            fallo("La categoría seleccionada no existe")
        }
    }

    suspend fun eliminarProducto(producto: Producto): Result<Unit> =
        try {
            productoDao.eliminar(producto)
            Result.success(Unit)
        } catch (e: SQLiteConstraintException) {
            // RESTRICT: ya aparece en pedidos, borrarlo rompería el historial
            fallo("${producto.nombre} ya tiene ventas registradas. Mejor márcalo como no disponible.")
        }

    suspend fun cambiarDisponibilidad(producto: Producto, disponible: Boolean) {
        productoDao.actualizar(producto.copy(disponible = disponible))
    }

    // Llegó mercancía: suma al stock actual
    suspend fun reabastecer(productoId: Long, cantidad: Int): Result<Unit> {
        if (cantidad <= 0) return fallo("La cantidad debe ser mayor a 0")
        productoDao.reponerStock(productoId, cantidad)
        return Result.success(Unit)
    }

    // ---------- Categorías ----------

    suspend fun crearCategoria(nombre: String): Result<Long> {
        val limpio = nombre.trim()
        if (limpio.isEmpty()) return fallo("Escribe el nombre de la categoría")
        return try {
            Result.success(categoriaDao.insertar(Categoria(nombre = limpio)))
        } catch (e: SQLiteConstraintException) {
            fallo("Ya existe una categoría llamada $limpio")
        }
    }

    suspend fun eliminarCategoria(categoria: Categoria): Result<Unit> =
        try {
            categoriaDao.eliminar(categoria)
            Result.success(Unit)
        } catch (e: SQLiteConstraintException) {
            fallo("${categoria.nombre} todavía tiene productos. Muévelos o elimínalos primero.")
        }
}
