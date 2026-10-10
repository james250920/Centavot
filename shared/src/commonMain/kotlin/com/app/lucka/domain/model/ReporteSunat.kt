package com.app.lucka.domain.model

import kotlinx.datetime.YearMonth

/**
 * Resumen del negocio de un mes (ventas y gastos de negocio), listo para revisar con el
 * contador. Lucka no declara ni envía nada a SUNAT.
 */
data class ReporteSunat(
    val periodo: YearMonth,
    val regimen: RegimenTributario,
    val gastos: List<Gasto>,
    val ventas: List<Ingreso> = emptyList(),
) {
    init {
        require(gastos.all { it.esDeNegocio }) { "El reporte solo incluye gastos de negocio" }
        require(ventas.all { it.esDeNegocio }) { "El reporte solo incluye ventas del negocio" }
    }

    /** Total de compras y gastos del negocio. */
    val total: Monto get() = gastos.map { it.monto }.sumar()

    val totalVentas: Monto get() = ventas.map { it.monto }.sumar()

    val ganancia: Monto get() = totalVentas - total

    val estaVacio: Boolean get() = gastos.isEmpty() && ventas.isEmpty()
}
