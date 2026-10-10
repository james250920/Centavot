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

/** Todo lo que la app guarda del usuario, en un solo archivo por secciones (derecho de acceso). */
fun exportarTodoCsv(
    perfil: Perfil?,
    regimen: RegimenTributario?,
    ingresos: List<Ingreso>,
    gastos: List<Gasto>,
    contactos: List<Contacto>,
    cobros: List<Cobro>,
    actividades: List<Actividad>,
    generado: kotlinx.datetime.LocalDateTime,
): String = csv {
    appendLine(fila("Mis datos en Centavot", generado.toString()))
    appendLine(fila("Este archivo tiene todo lo que Centavot guarda en tu celular."))
    appendLine()
    appendLine(fila("PERFIL"))
    appendLine(fila("Nombre", "Rubro", "Ingreso mensual (S/)", "Ahorro (%)", "Régimen", "Tope (S/)"))
    appendLine(
        fila(
            perfil?.nombre.orEmpty(),
            perfil?.rubro?.etiqueta.orEmpty(),
            perfil?.ingresoMensual?.enDecimal().orEmpty(),
            perfil?.tasaAhorro?.let { "${it.decimas / 10}.${it.decimas % 10}" }.orEmpty(),
            regimen?.nombre.orEmpty(),
            regimen?.tope?.enDecimal().orEmpty(),
        ),
    )
    appendLine()
    appendLine(fila("VENTAS E INGRESOS (${ingresos.size})"))
    appendLine(fila("Fecha", "Tipo", "Descripción", "Monto (S/)"))
    ingresos.sortedBy { it.fecha }.forEach {
        appendLine(fila(it.fecha.toString(), if (it.esDeNegocio) "Venta" else "Ingreso personal", it.descripcion.orEmpty(), it.monto.enDecimal()))
    }
    appendLine()
    appendLine(fila("GASTOS (${gastos.size})"))
    appendLine(fila("Fecha", "Tipo", "Categoría", "Descripción", "Monto (S/)"))
    gastos.sortedBy { it.fecha }.forEach {
        val tipo = when (it.categoria) {
            Categoria.NEGOCIO -> "Negocio"
            Categoria.PERSONAL -> "Personal"
            null -> "Sin clasificar"
        }
        appendLine(fila(it.fecha.toString(), tipo, it.subcategoria?.etiqueta.orEmpty(), it.descripcion.orEmpty(), it.monto.enDecimal()))
    }
    appendLine()
    appendLine(fila("CONTACTOS (${contactos.size})"))
    appendLine(fila("Nombre", "Celular"))
    contactos.forEach { appendLine(fila(it.nombre, it.telefono.orEmpty())) }
    appendLine()
    appendLine(fila("COBROS (${cobros.size})"))
    appendLine(fila("Desde", "Quién", "Tipo", "Motivo", "Monto (S/)", "Adelanto (S/)", "Abonos (S/)", "Falta (S/)", "Estado", "Cobrado el"))
    cobros.sortedBy { it.fecha }.forEach {
        appendLine(
            fila(
                it.fecha.toString(), it.contacto.nombre, it.tipo.etiqueta, it.motivo, it.monto.enDecimal(),
                it.adelanto.enDecimal(), it.abonado.enDecimal(), it.saldo.enDecimal(),
                if (it.estaPendiente) "Pendiente" else "Cobrado", it.fechaCobrado?.toString().orEmpty(),
            ),
        )
    }
    appendLine()
    appendLine(fila("HISTORIAL DE ACTIVIDAD (${actividades.size})"))
    appendLine(fila("Fecha y hora", "Qué pasó"))
    actividades.sortedBy { it.fechaHora }.forEach { appendLine(fila(it.fechaHora.toString(), it.descripcion)) }
}
