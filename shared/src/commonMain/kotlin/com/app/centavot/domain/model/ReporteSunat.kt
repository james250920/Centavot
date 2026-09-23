package com.app.centavot.domain.model

import kotlinx.datetime.YearMonth

/** Resumen de los gastos de negocio de un mes, listo para revisar y exportar. */
data class ReporteSunat(
    val periodo: YearMonth,
    val regimen: RegimenTributario,
    val gastos: List<Gasto>,
) {
    init {
        require(gastos.all { it.esDeNegocio }) { "El reporte solo incluye gastos de negocio" }
    }

    val total: Monto get() = gastos.map { it.monto }.sumar()
}
