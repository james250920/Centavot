package com.app.centavot.domain.model

import kotlin.jvm.JvmInline

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
