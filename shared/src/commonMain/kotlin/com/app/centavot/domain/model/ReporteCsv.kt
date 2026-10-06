package com.app.centavot.domain.model

import kotlinx.datetime.YearMonth

/**
 * Reportes en CSV para abrirlos en Excel o enviarlos al contador (RF-08 / RF-10).
 * Llevan BOM para que Excel lea bien las tildes.
 */
fun ReporteSunat.aCsv(): String = csv {
    appendLine(fila("Resumen del negocio", periodo.toString()))
    appendLine(fila("Régimen", regimen.nombre))
    appendLine(fila("Ventas", totalVentas.enDecimal()))
    appendLine(fila("Compras y gastos", total.enDecimal()))
    appendLine(fila("Ganancia", ganancia.enDecimal()))
    appendLine()
    appendLine(fila("COMPRAS Y GASTOS DEL NEGOCIO"))
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
    appendLine()
    appendLine(fila("VENTAS"))
    appendLine(fila("Fecha", "Descripción", "Monto (S/)"))
    ventas.sortedBy { it.fecha }.forEach { venta ->
        appendLine(fila(venta.fecha.toString(), venta.descripcion.orEmpty(), venta.monto.enDecimal()))
    }
    appendLine(fila("", "Total", totalVentas.enDecimal()))
}

/** Gastos personales del mes: es solo para el usuario, no para SUNAT. */
fun reporteGastosPersonalesCsv(periodo: YearMonth, gastos: List<Gasto>): String = csv {
    val personales = gastos.filter { it.categoria == Categoria.PERSONAL }.sortedBy { it.fecha }
    appendLine(fila("Gastos personales", periodo.toString()))
    appendLine()
    appendLine(fila("Fecha", "Descripción", "Categoría", "Monto (S/)"))
    personales.forEach { gasto ->
        appendLine(
            fila(gasto.fecha.toString(), gasto.descripcion.orEmpty(), gasto.subcategoria?.etiqueta.orEmpty(), gasto.monto.enDecimal()),
        )
    }
    appendLine(fila("", "", "Total", personales.map { it.monto }.sumar().enDecimal()))
}

/** Lo que te deben: solo cobros pendientes, con lo que falta pagar. */
fun reporteMeDebenCsv(cobros: List<Cobro>): String = csv {
    val pendientes = cobros.filter { it.estaPendiente }.sortedBy { it.fecha }
    appendLine(fila("Me deben"))
    appendLine()
    appendLine(fila("Desde", "Quién", "Tipo", "Motivo", "Monto (S/)", "Adelanto (S/)", "Abonos (S/)", "Falta (S/)"))
    pendientes.forEach { cobro ->
        appendLine(
            fila(
                cobro.fecha.toString(),
                cobro.contacto.nombre,
                cobro.tipo.etiqueta,
                cobro.motivo,
                cobro.monto.enDecimal(),
                cobro.adelanto.enDecimal(),
                cobro.abonado.enDecimal(),
                cobro.saldo.enDecimal(),
            ),
        )
    }
    appendLine(fila("", "", "", "", "", "", "Total", pendientes.map { it.saldo }.sumar().enDecimal()))
}

private fun csv(contenido: StringBuilder.() -> Unit): String = buildString {
    append('﻿')
    contenido()
}

/** "1234.50", sin separador de miles para que Excel lo lea como número. */
private fun Monto.enDecimal(): String {
    val signo = if (centimos < 0) "-" else ""
    val valor = kotlin.math.abs(centimos)
    return "$signo${valor / 100}.${(valor % 100).toString().padStart(2, '0')}"
}

private fun fila(vararg celdas: String): String = celdas.joinToString(",") { celda ->
    if (celda.any { it == ',' || it == '"' || it == '\n' }) "\"${celda.replace("\"", "\"\"")}\"" else celda
}
