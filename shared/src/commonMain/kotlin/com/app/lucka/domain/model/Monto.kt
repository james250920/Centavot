package com.app.lucka.domain.model

import kotlin.jvm.JvmInline
import kotlin.math.abs

/**
 * Monto en soles, guardado en céntimos para evitar errores de redondeo con Double.
 */
@JvmInline
value class Monto(val centimos: Long) : Comparable<Monto> {

    operator fun plus(otro: Monto) = Monto(centimos + otro.centimos)

    operator fun minus(otro: Monto) = Monto(centimos - otro.centimos)

    override fun compareTo(other: Monto) = centimos.compareTo(other.centimos)

    companion object {
        val CERO = Monto(0)

        fun soles(soles: Long) = Monto(soles * 100)
    }
}

fun Iterable<Monto>.sumar(): Monto = fold(Monto.CERO) { total, monto -> total + monto }

/** "S/ 1,234.50". Vive en el dominio porque también se usa en los textos de Actividad. */
fun Monto.enSoles(): String {
    val valor = abs(centimos)
    val soles = (valor / 100).toString().reversed().chunked(3).joinToString(",").reversed()
    val cent = (valor % 100).toString().padStart(2, '0')
    val signo = if (centimos < 0) "-" else ""
    return "${signo}S/ $soles.$cent"
}
