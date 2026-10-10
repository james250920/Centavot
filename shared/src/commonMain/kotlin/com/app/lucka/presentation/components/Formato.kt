package com.app.lucka.presentation.components

import com.app.lucka.domain.model.Monto
import com.app.lucka.domain.model.TasaAhorro
import com.app.lucka.domain.model.enSoles
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minus

private val MESES = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
)

private val REGEX_MONTO = Regex("""\d{1,9}(\.\d{0,2})?""")
private val REGEX_TASA = Regex("""\d{1,3}(\.\d?)?""")
private val REGEX_ENTRADA_TASA = Regex("""\d{0,3}([.,]\d?)?""")
private val REGEX_ENTRADA_MONTO = Regex("""\d{0,9}([.,]\d{0,2})?""")

/** "S/ 1,234.50" */
fun Monto.formatear(): String = enSoles()

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

/** "Hoy, 16:13" o "12 de septiembre, 09:05". */
fun LocalDateTime.formatear(hoy: LocalDate): String {
    val hora = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
    return "${date.formatearRelativo(hoy)}, $hora"
}

/** "12.5 %" */
fun TasaAhorro.formatear(): String = "${comoTextoEditable()} %"

/** "12.5", "10" */
fun TasaAhorro.comoTextoEditable(): String =
    if (decimas % 10 == 0) (decimas / 10).toString() else "${decimas / 10}.${decimas % 10}"

/** "12,5" → 12.5 %; null si no es un número entre 0 y 100 con un decimal como máximo. */
fun parsearTasa(texto: String): TasaAhorro? {
    val limpio = texto.trim().replace(',', '.').ifEmpty { "0" }
    if (!REGEX_TASA.matches(limpio)) return null
    val partes = limpio.split('.')
    val decimas = partes[0].toInt() * 10 + (partes.getOrNull(1)?.takeIf { it.isNotEmpty() }?.toInt() ?: 0)
    return if (decimas in 0..TasaAhorro.MAXIMA) TasaAhorro(decimas) else null
}

/** Deja escribir solo un porcentaje con hasta un decimal. */
fun esEntradaDeTasaValida(texto: String): Boolean = REGEX_ENTRADA_TASA.matches(texto)
