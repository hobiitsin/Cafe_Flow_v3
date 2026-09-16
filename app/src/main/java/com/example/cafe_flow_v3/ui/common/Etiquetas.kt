package com.example.cafe_flow_v3.ui.common

import com.example.cafe_flow_v3.data.entity.EstadoPedido
import com.example.cafe_flow_v3.data.entity.MetodoPago
import com.example.cafe_flow_v3.data.entity.OrigenPedido
import com.example.cafe_flow_v3.data.entity.Rol

// Texto bonito para mostrar los enums en pantalla.
// Así la base guarda "EN_PREPARACION" y el usuario ve "En preparación".

fun EstadoPedido.etiqueta(): String = when (this) {
    EstadoPedido.PENDIENTE -> "Pendiente"
    EstadoPedido.EN_PREPARACION -> "En preparación"
    EstadoPedido.LISTO -> "Listo"
    EstadoPedido.COMPLETADO -> "Completado"
    EstadoPedido.CANCELADO -> "Cancelado"
}

fun OrigenPedido.etiqueta(): String = when (this) {
    OrigenPedido.MOSTRADOR -> "Mostrador"
    OrigenPedido.DIDI_FOOD -> "DiDi Food"
    OrigenPedido.UBER_EATS -> "Uber Eats"
    OrigenPedido.RAPPI -> "Rappi"
}

fun MetodoPago.etiqueta(): String = when (this) {
    MetodoPago.EFECTIVO -> "Efectivo"
    MetodoPago.TARJETA -> "Tarjeta"
    MetodoPago.TRANSFERENCIA -> "Transferencia"
    MetodoPago.PLATAFORMA -> "Pagado en plataforma"
}

fun Rol.etiqueta(): String = when (this) {
    Rol.ADMIN -> "Administrador"
    Rol.GERENTE -> "Gerente"
    Rol.CAJERO -> "Cajero"
    Rol.BARISTA -> "Barista"
}
