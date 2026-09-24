package com.app.centavot.presentation.components

import com.app.centavot.domain.model.Monto
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minus
import kotlin.math.abs

private val MESES = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
)

private val REGEX_MONTO = Regex("""\d{1,9}(\.\d{0,2})?""")
private val REGEX_ENTRADA_MONTO = Regex("""\d{0,9}([.,]\d{0,2})?""")

/** "S/ 1,234.50" */
fun Monto.formatear(): String {
    val valor = abs(centimos)
    val soles = (valor / 100).toString().reversed().chunked(3).joinToString(",").reversed()
    val cent = (valor % 100).toString().padStart(2, '0')
    val signo = if (centimos < 0) "-" else ""
    return "${signo}S/ $soles.$cent"
}

/** Texto para precargar el campo de monto al editar: "12.5" → "12.50", "12.00" → "12". */
fun Monto.comoTextoEditable(): String {
    val soles = centimos / 100
    val cent = centimos % 100
    return if (cent == 0L) soles.toString() else "$soles.${cent.toString().padStart(2, '0')}"
}

/** Convierte lo que escribió el usuario ("12", "12.5", "12,50") a [Monto]; null si no es válido. */
fun parsearMonto(texto: String): Monto? {
    val limpio = texto.trim().replace(',', '.')
    if (!REGEX_MONTO.matches(limpio)) return null
    val partes = limpio.split('.')
    val soles = partes[0].toLong()
    val cent = partes.getOrNull(1).orEmpty().padEnd(2, '0').toLong()
    return Monto(soles * 100 + cent)
}

/** Deja escribir solo dígitos y un separador decimal con hasta 2 decimales. */
fun esEntradaDeMontoValida(texto: String): Boolean = REGEX_ENTRADA_MONTO.matches(texto)

/** "Septiembre 2026" */
fun YearMonth.formatear(): String =
    "${MESES[month.ordinal].replaceFirstChar { it.uppercase() }} $year"

/** "Hoy", "Ayer", "12 de septiembre" o "12 de septiembre de 2025". */
fun LocalDate.formatearRelativo(hoy: LocalDate): String = when (this) {
    hoy -> "Hoy"
    hoy.minus(1, DateTimeUnit.DAY) -> "Ayer"
    else -> buildString {
        append("$day de ${MESES[month.ordinal]}")
        if (year != hoy.year) append(" de $year")
    }
}
