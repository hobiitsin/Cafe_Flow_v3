package com.example.cafe_flow_v3.data

import androidx.room.withTransaction
import com.example.cafe_flow_v3.data.entity.Categoria
import com.example.cafe_flow_v3.data.entity.DetallePedido
import com.example.cafe_flow_v3.data.entity.EstadoPedido
import com.example.cafe_flow_v3.data.entity.MetodoPago
import com.example.cafe_flow_v3.data.entity.OrigenPedido
import com.example.cafe_flow_v3.data.entity.Pedido
import com.example.cafe_flow_v3.data.entity.Producto
import com.example.cafe_flow_v3.data.entity.Rol
import com.example.cafe_flow_v3.data.entity.Usuario
import com.example.cafe_flow_v3.util.Fechas
import com.example.cafe_flow_v3.util.SeguridadPassword

/**
 * Llena la base con datos de demostración la PRIMERA vez que se abre la app.
 *
 * Usuarios:
 *   barista@cafeflow.com / 12345678
 *   admin@cafeflow.com   / admin1234
 *
 * Para volver a empezar: desinstala la app o borra sus datos desde Ajustes.
 */
object DatosPrueba {

    private data class Item(
        val producto: Producto,
        val cantidad: Int = 1,
        val notas: String? = null
    )

    suspend fun poblarSiVacia(db: AppDatabase) {
        if (db.usuarioDao().contar() > 0) return // ya tiene datos, no hacemos nada

        // Hashear es lento a propósito, así que lo hacemos antes de abrir la transacción
        val hashBarista = SeguridadPassword.hashear("12345678")
        val hashAdmin = SeguridadPassword.hashear("admin1234")

        // Todo o nada: si algo falla a la mitad, no queda la base a medias
        db.withTransaction {

            // ---------- Usuarios ----------
            val baristaId = db.usuarioDao().insertar(
                Usuario(
                    nombre = "Barista",
                    email = "barista@cafeflow.com",
                    passwordHash = hashBarista,
                    rol = Rol.BARISTA
                )
            )
            db.usuarioDao().insertar(
                Usuario(
                    nombre = "Administrador",
                    email = "admin@cafeflow.com",
                    passwordHash = hashAdmin,
                    rol = Rol.ADMIN
                )
            )

            // ---------- Categorías ----------
            val nombresCategorias = listOf(
                "Bebidas calientes", "Bebidas frías", "Panadería", "Postres", "Alimentos"
            )
            val idsCategorias = db.categoriaDao()
                .insertarTodas(nombresCategorias.map { Categoria(nombre = it) })
            val cat = nombresCategorias.zip(idsCategorias).toMap()

            // ---------- Productos ----------
            // Precios en centavos: 110_00 = $110.00
            val productosBase = listOf(
                Producto(categoriaId = cat.getValue("Bebidas calientes"), nombre = "Espresso Doble", precioCentavos = 110_00, stock = 100),
                Producto(categoriaId = cat.getValue("Bebidas calientes"), nombre = "Americano", precioCentavos = 75_00, stock = 100),
                Producto(categoriaId = cat.getValue("Bebidas calientes"), nombre = "Capuchino", precioCentavos = 95_00, stock = 100),
                Producto(categoriaId = cat.getValue("Bebidas calientes"), nombre = "Latte", precioCentavos = 95_00, stock = 100),
                Producto(categoriaId = cat.getValue("Bebidas calientes"), nombre = "Flat White", precioCentavos = 115_00, stock = 100),
                Producto(categoriaId = cat.getValue("Bebidas frías"), nombre = "Cold Brew", precioCentavos = 95_00, stock = 40),
                Producto(categoriaId = cat.getValue("Bebidas frías"), nombre = "Frappé Moka", precioCentavos = 120_00, stock = 40),
                Producto(categoriaId = cat.getValue("Panadería"), nombre = "Croissant", precioCentavos = 70_00, stock = 18),
                Producto(categoriaId = cat.getValue("Postres"), nombre = "Tarta de Manzana", precioCentavos = 120_00, stock = 3),  // bajo inventario
                Producto(categoriaId = cat.getValue("Postres"), nombre = "Brownie", precioCentavos = 60_00, stock = 0),            // agotado
                Producto(categoriaId = cat.getValue("Alimentos"), nombre = "Avocado Toast", precioCentavos = 115_00, stock = 12),
                Producto(categoriaId = cat.getValue("Alimentos"), nombre = "Sándwich de Pavo", precioCentavos = 135_00, stock = 4) // bajo inventario
            )
            val idsProductos = db.productoDao().insertarTodos(productosBase)

            // Mapa nombre -> producto (ya con su id real)
            val productos = productosBase
                .zip(idsProductos) { producto, id -> producto.copy(id = id) }
                .associateBy { it.nombre }

            fun item(nombre: String, cantidad: Int = 1, notas: String? = null) =
                Item(productos.getValue(nombre), cantidad, notas)

            // ---------- Pedidos de AYER (para calcular el % vs hoy) ----------
            db.crearPedido(Fechas.aLasHoras(9, 10, diasAtras = 1), listOf(item("Americano", 2)), usuarioId = baristaId)
            db.crearPedido(Fechas.aLasHoras(11, 30, diasAtras = 1), listOf(item("Capuchino"), item("Croissant")), metodoPago = MetodoPago.TARJETA, usuarioId = baristaId)
            db.crearPedido(Fechas.aLasHoras(13, 5, diasAtras = 1), listOf(item("Sándwich de Pavo"), item("Latte")), origen = OrigenPedido.UBER_EATS, folioExterno = "UE-4K9P1")
            db.crearPedido(Fechas.aLasHoras(17, 20, diasAtras = 1), listOf(item("Frappé Moka", 2)), metodoPago = MetodoPago.TARJETA, usuarioId = baristaId)

            // ---------- Pedidos de HOY completados ----------
            db.crearPedido(Fechas.aLasHoras(8, 5), listOf(item("Americano", notas = "Sin azúcar"), item("Croissant")), usuarioId = baristaId)
            db.crearPedido(Fechas.aLasHoras(10, 10), listOf(item("Capuchino", 2, notas = "Leche deslactosada")), metodoPago = MetodoPago.TARJETA, usuarioId = baristaId)
            db.crearPedido(Fechas.aLasHoras(10, 40), listOf(item("Latte"), item("Brownie")), origen = OrigenPedido.DIDI_FOOD, folioExterno = "DD-58213")
            db.crearPedido(Fechas.aLasHoras(12, 15), listOf(item("Sándwich de Pavo"), item("Frappé Moka")), metodoPago = MetodoPago.TARJETA, usuarioId = baristaId)
            db.crearPedido(Fechas.aLasHoras(12, 50), listOf(item("Cold Brew"), item("Avocado Toast")), usuarioId = baristaId)
            db.crearPedido(Fechas.aLasHoras(13, 20), listOf(item("Flat White", 2), item("Tarta de Manzana")), origen = OrigenPedido.UBER_EATS, folioExterno = "UE-7F3K2")
            db.crearPedido(Fechas.aLasHoras(16, 30), listOf(item("Latte", 2), item("Croissant", 2)), metodoPago = MetodoPago.TRANSFERENCIA, usuarioId = baristaId)

            // ---------- Cancelado (no debe contar en ventas) ----------
            db.crearPedido(Fechas.aLasHoras(11, 0), listOf(item("Americano")), estado = EstadoPedido.CANCELADO, usuarioId = baristaId)

            // ---------- Pedidos ACTIVOS ----------
            db.crearPedido(Fechas.aLasHoras(14, 45), listOf(item("Capuchino"), item("Brownie")), estado = EstadoPedido.LISTO, origen = OrigenPedido.RAPPI, folioExterno = "RP-99120")
            db.crearPedido(Fechas.aLasHoras(15, 10), listOf(item("Cold Brew", 2, notas = "Poco hielo")), estado = EstadoPedido.EN_PREPARACION, origen = OrigenPedido.UBER_EATS, folioExterno = "UE-8H2L9")
            db.crearPedido(Fechas.aLasHoras(15, 30), listOf(item("Avocado Toast"), item("Americano")), estado = EstadoPedido.PENDIENTE, origen = OrigenPedido.DIDI_FOOD, folioExterno = "DD-58377")
            db.crearPedido(Fechas.aLasHoras(17, 0), listOf(item("Espresso Doble"), item("Croissant")), estado = EstadoPedido.PENDIENTE, usuarioId = baristaId)
        }
    }

    // Inserta el pedido y sus renglones, calculando el total con los precios del producto
    private suspend fun AppDatabase.crearPedido(
        fecha: Long,
        items: List<Item>,
        estado: EstadoPedido = EstadoPedido.COMPLETADO,
        origen: OrigenPedido = OrigenPedido.MOSTRADOR,
        metodoPago: MetodoPago =
            if (origen == OrigenPedido.MOSTRADOR) MetodoPago.EFECTIVO else MetodoPago.PLATAFORMA,
        usuarioId: Long? = null,
        folioExterno: String? = null
    ) {
        val total = items.sumOf { it.producto.precioCentavos * it.cantidad }

        val pedidoId = pedidoDao().insertarPedido(
            Pedido(
                usuarioId = usuarioId,
                fechaHora = fecha,
                estado = estado,
                origen = origen,
                folioExterno = folioExterno,
                metodoPago = metodoPago,
                totalCentavos = total
            )
        )

        pedidoDao().insertarDetalles(
            items.map {
                DetallePedido(
                    pedidoId = pedidoId,
                    productoId = it.producto.id,
                    cantidad = it.cantidad,
                    precioUnitarioCentavos = it.producto.precioCentavos,
                    notas = it.notas
                )
            }
        )
    }
}
