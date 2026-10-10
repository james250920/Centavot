package com.app.centavot.domain.model

import kotlinx.datetime.LocalDate

/**
 * Dinero que entra. En un negocio es cada venta suelta ("una gaseosa a S/ 3.50"), porque
 * el ingreso del comerciante se arma venta por venta, no llega una vez al mes.
 * [Categoria.PERSONAL] sirve para un sueldo u otro ingreso que no es del negocio.
 * Un [retiroDelNegocio] es plata que se sacó de la caja para la casa: es personal, pero en el
 * modo negocio se resta de la caja para que cuadre.
 */
data class Ingreso(
    val id: String,
    val monto: Monto,
    val fecha: LocalDate,
    val categoria: Categoria = Categoria.NEGOCIO,
    val descripcion: String? = null,
    val retiroDelNegocio: Boolean = false,
) {
    val esDeNegocio: Boolean get() = categoria == Categoria.NEGOCIO
}

/** Lo que se anota en la pantalla de entrada de plata. */
enum class TipoEntrada { VENTA, INGRESO, RETIRO }

val Ingreso.tipo: TipoEntrada
    get() = when {
        retiroDelNegocio -> TipoEntrada.RETIRO
        esDeNegocio -> TipoEntrada.VENTA
        else -> TipoEntrada.INGRESO
    }

/** Venta que el usuario repite seguido: se vuelve a registrar con un solo toque. */
data class VentaFrecuente(val descripcion: String, val monto: Monto)

/**
 * Las ventas (o, en lo personal, los ingresos) con descripción que más se repiten, de la más repetida a la menos. "Gaseosa" y
 * " gaseosa " cuentan como la misma venta; se muestra como la escribió la vez más reciente.
 * Espera la lista de la más reciente a la más antigua, como la entrega el repositorio.
 */
fun List<Ingreso>.ventasFrecuentes(
    categoria: Categoria = Categoria.NEGOCIO,
    maximo: Int = MAX_VENTAS_FRECUENTES,
): List<VentaFrecuente> =
    mapNotNull { ingreso ->
        val cuenta = ingreso.categoria == categoria && !ingreso.retiroDelNegocio
        ingreso.descripcion?.trim()?.takeIf { cuenta && it.isNotEmpty() }?.let { VentaFrecuente(it, ingreso.monto) }
    }
        .groupBy { it.descripcion.lowercase() to it.monto }
        .values
        .sortedByDescending { it.size }
        .take(maximo)
        .map { it.first() }

const val MAX_VENTAS_FRECUENTES = 6
