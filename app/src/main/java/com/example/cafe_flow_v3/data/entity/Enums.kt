package com.example.cafe_flow_v3.data.entity

// Room guarda los enums como texto (su nombre) automáticamente,
// así que no necesitamos TypeConverters para estos.
// Ojo: si renombras una constante, los registros viejos ya no la reconocen.

enum class Rol {
    ADMIN,
    GERENTE,
    CAJERO,
    BARISTA
}

enum class EstadoPedido {
    PENDIENTE,
    EN_PREPARACION,
    LISTO,
    COMPLETADO,
    CANCELADO
}

enum class OrigenPedido {
    MOSTRADOR,
    DIDI_FOOD,
    UBER_EATS,
    RAPPI
}

enum class MetodoPago {
    EFECTIVO,
    TARJETA,
    TRANSFERENCIA,
    PLATAFORMA // pagado directamente en DiDi / Uber / Rappi
}
