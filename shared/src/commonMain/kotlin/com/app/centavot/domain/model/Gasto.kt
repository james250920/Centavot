package com.app.centavot.domain.model

import kotlinx.datetime.LocalDate

data class Gasto(
    val id: String,
    val monto: Monto,
    val fecha: LocalDate,
    val origen: OrigenGasto,
    val estado: EstadoGasto,
    /** null mientras el gasto no está clasificado. */
    val categoria: Categoria? = null,
    val proveedor: String? = null,
    val descripcion: String? = null,
    /** Si es true, el backend no puede sobrescribir la categoría: el usuario siempre gana. */
    val corregidoManualmente: Boolean = false,
) {
    val esDeNegocio: Boolean get() = categoria == Categoria.NEGOCIO
}
