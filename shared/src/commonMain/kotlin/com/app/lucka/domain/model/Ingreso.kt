package com.app.lucka.domain.model

import kotlinx.datetime.LocalDate

/**
 * Dinero que entra. En un negocio es cada venta suelta ("una gaseosa a S/ 3.50"), porque
 * el ingreso del comerciante se arma venta por venta, no llega una vez al mes.
 * [Categoria.PERSONAL] sirve para un sueldo u otro ingreso que no es del negocio.
 */
data class Ingreso(
    val id: String,
    val monto: Monto,
    val fecha: LocalDate,
    val categoria: Categoria = Categoria.NEGOCIO,
    val descripcion: String? = null,
) {
    val esDeNegocio: Boolean get() = categoria == Categoria.NEGOCIO
}

/** Venta que el usuario repite seguido: se vuelve a registrar con un solo toque. */
data class VentaFrecuente(val descripcion: String, val monto: Monto)

/**
 * Las ventas con descripción que más se repiten, de la más repetida a la menos. "Gaseosa" y
 * " gaseosa " cuentan como la misma venta; se muestra como la escribió la vez más reciente.
 * Espera la lista de la más reciente a la más antigua, como la entrega el repositorio.
 */
fun List<Ingreso>.ventasFrecuentes(maximo: Int = MAX_VENTAS_FRECUENTES): List<VentaFrecuente> =
    mapNotNull { ingreso ->
        ingreso.descripcion?.trim()?.takeIf { ingreso.esDeNegocio && it.isNotEmpty() }?.let { VentaFrecuente(it, ingreso.monto) }
    }
        .groupBy { it.descripcion.lowercase() to it.monto }
        .values
        .sortedByDescending { it.size }
        .take(maximo)
        .map { it.first() }

const val MAX_VENTAS_FRECUENTES = 6
