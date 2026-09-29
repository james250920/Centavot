package com.app.centavot.domain.model

/**
 * Reporte en CSV para abrirlo en Excel o enviarlo al contador (RF-08 / RF-10).
 * Lleva BOM para que Excel lea bien las tildes.
 */
fun ReporteSunat.aCsv(): String = buildString {
    append('﻿')
    appendLine(fila("Reporte de gastos de negocio", periodo.toString()))
    appendLine(fila("Régimen", regimen.nombre))
    appendLine()
    appendLine(fila("Fecha", "Descripción", "Categoría", "Proveedor", "Monto (S/)"))
    gastos.sortedBy { it.fecha }.forEach { gasto ->
        appendLine(
            fila(
                gasto.fecha.toString(),
                gasto.descripcion.orEmpty(),
                gasto.subcategoria?.etiqueta.orEmpty(),
                gasto.proveedor.orEmpty(),
                gasto.monto.enDecimal(),
            ),
        )
    }
    appendLine(fila("", "", "", "Total", total.enDecimal()))
}

/** "1234.50", sin separador de miles para que Excel lo lea como número. */
private fun Monto.enDecimal(): String = "${centimos / 100}.${(centimos % 100).toString().padStart(2, '0')}"

private fun fila(vararg celdas: String): String = celdas.joinToString(",") { celda ->
    if (celda.any { it == ',' || it == '"' || it == '\n' }) "\"${celda.replace("\"", "\"\"")}\"" else celda
}
