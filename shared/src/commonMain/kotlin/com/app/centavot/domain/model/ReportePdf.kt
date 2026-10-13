package com.app.centavot.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

/**
 * Lo que va en el PDF para el contador (P5), sin decir cómo se dibuja: cada plataforma lo pinta.
 * Así el contenido se prueba en los tests comunes y el dibujo queda en Android.
 */
data class DocumentoReporte(
    val titulo: String,
    val subtitulo: String,
    val bloques: List<BloqueReporte>,
    val pie: String,
)

sealed interface BloqueReporte {
    val titulo: String

    /** Pocas filas de "etiqueta ... monto", como el resumen. */
    data class Totales(override val titulo: String, val filas: List<FilaTotal>) : BloqueReporte

    /** Una tabla con columnas; [total] es la última fila, en negrita. */
    data class Tabla(
        override val titulo: String,
        val columnas: List<Columna>,
        val filas: List<List<String>>,
        val total: List<String>? = null,
    ) : BloqueReporte
}

data class FilaTotal(val etiqueta: String, val valor: String, val destacada: Boolean = false)

/** [peso] reparte el ancho de la página entre las columnas. */
data class Columna(val titulo: String, val peso: Float, val alDerecha: Boolean = false)

/** "13/10/2026": el formato que usa el contador. */
fun LocalDate.enNumeros(): String =
    "${day.toString().padStart(2, '0')}/${(month.ordinal + 1).toString().padStart(2, '0')}/$year"

/** "Octubre 2026" */
fun YearMonth.nombre(): String = "${NOMBRES_MES[month.ordinal].replaceFirstChar { it.uppercase() }} $year"

val NOMBRES_MES = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
)

/**
 * El resumen del mes para el contador. Las ventas van **por día**: una bodega anota cientos al mes y
 * el contador necesita el total diario; el detalle venta por venta sigue en el Excel.
 */
fun ReporteSunat.aDocumento(generado: LocalDate): DocumentoReporte {
    val bloques = mutableListOf<BloqueReporte>(
        BloqueReporte.Totales(
            titulo = "Resumen del mes",
            filas = listOf(
                FilaTotal("Ventas", totalVentas.enSoles()),
                FilaTotal("Compras y gastos", total.enSoles()),
                FilaTotal(if (ganancia < Monto.CERO) "Pérdida" else "Ganancia", Monto(kotlin.math.abs(ganancia.centimos)).enSoles(), destacada = true),
            ),
        ),
    )
    if (gastos.isNotEmpty()) {
        bloques += BloqueReporte.Tabla(
            titulo = "Compras y gastos por categoría",
            columnas = listOf(Columna("Categoría", 3f), Columna("Monto", 1f, alDerecha = true)),
            filas = porSubcategoria.map { (subcategoria, monto) ->
                listOf(subcategoria?.etiqueta ?: ETIQUETA_SIN_CATEGORIA, monto.enSoles())
            },
            total = listOf("Total", total.enSoles()),
        )
    }
    if (ventas.isNotEmpty()) {
        bloques += BloqueReporte.Tabla(
            titulo = "Ventas por día",
            columnas = listOf(Columna("Fecha", 1.2f), Columna("Ventas anotadas", 1.2f, alDerecha = true), Columna("Monto", 1.2f, alDerecha = true)),
            filas = ventas.groupBy { it.fecha }.entries.sortedBy { it.key }.map { (fecha, delDia) ->
                listOf(fecha.enNumeros(), delDia.size.toString(), delDia.map { it.monto }.sumar().enSoles())
            },
            total = listOf("Total", ventas.size.toString(), totalVentas.enSoles()),
        )
    }
    if (gastos.isNotEmpty()) {
        bloques += BloqueReporte.Tabla(
            titulo = "Detalle de compras y gastos",
            columnas = listOf(
                Columna("Fecha", 1.1f),
                Columna("Descripción", 2.3f),
                Columna("Categoría", 2f),
                Columna("Proveedor", 1.4f),
                Columna("Monto", 1.3f, alDerecha = true),
            ),
            filas = gastos.sortedBy { it.fecha }.map { gasto ->
                listOf(
                    gasto.fecha.enNumeros(),
                    gasto.descripcion.orEmpty(),
                    gasto.subcategoria?.etiqueta ?: ETIQUETA_SIN_CATEGORIA,
                    gasto.proveedor.orEmpty(),
                    gasto.monto.enSoles(),
                )
            },
            total = listOf("", "", "", "Total", total.enSoles()),
        )
    }
    return DocumentoReporte(
        titulo = "Resumen para el contador",
        subtitulo = "${periodo.nombre()} · ${regimen.nombre}",
        bloques = bloques,
        pie = "Hecho con Centavot el ${generado.enNumeros()}. Centavot no envía nada a SUNAT: revisa este " +
            "resumen con tu contador antes de declarar.",
    )
}
