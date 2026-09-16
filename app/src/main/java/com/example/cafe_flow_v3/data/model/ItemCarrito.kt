package com.example.cafe_flow_v3.data.model

import com.example.cafe_flow_v3.data.entity.Producto

// Un renglón del carrito en la pantalla de Ventas (todavía no está en la base)
data class ItemCarrito(
    val producto: Producto,
    val cantidad: Int = 1,
    val notas: String? = null // personalización: "leche de almendra"
) {
    val subtotalCentavos: Long
        get() = producto.precioCentavos * cantidad
}
