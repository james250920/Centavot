package com.app.centavot.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

/**
 * Una meta de ahorro ("Viaje a Cusco", "Moto para el delivery"), en lo personal o en el negocio. Pedida en
 * la reunión con usuarios del 10 de octubre: como los frascos donde se separa la plata, pero con su avance.
 */
data class Meta(
    val id: String,
    val nombre: String,
    val objetivo: Monto,
    val categoria: Categoria,
    val creada: LocalDate,
    /** Para cuándo la quiere; opcional. */
    val fechaLimite: LocalDate? = null,
)

/** Plata que se separa para una meta. No es un gasto: no cambia la caja ni el tope. */
data class AbonoMeta(val id: String, val metaId: String, val monto: Monto, val fecha: LocalDate)

data class ProgresoMeta(val meta: Meta, val ahorrado: Monto) {
    val porcentaje: Int get() = (ahorrado.centimos * 100 / meta.objetivo.centimos).toInt().coerceAtMost(100)
    val falta: Monto get() = if (ahorrado >= meta.objetivo) Monto.CERO else meta.objetivo - ahorrado
    val cumplida: Boolean get() = ahorrado >= meta.objetivo

    /**
     * Cuánto separar cada semana para llegar a tiempo; null si no tiene fecha, ya se cumplió o la fecha ya pasó.
     * Se redondea hacia arriba al sol.
     */
    fun porSemana(hoy: LocalDate): Monto? {
        val fecha = meta.fechaLimite ?: return null
        if (cumplida) return null
        val dias = hoy.daysUntil(fecha)
        if (dias <= 0) return null
        val semanas = (dias + DIAS_SEMANA - 1) / DIAS_SEMANA
        val centimos = (falta.centimos + semanas - 1) / semanas
        return Monto((centimos + 99) / 100 * 100)
    }

    /** El hito (25, 50, 75 o 100 %) que se cruzó al pasar de [antes] a este progreso, si cruzó alguno. */
    fun hitoDesde(antes: ProgresoMeta): Int? = HITOS_META.lastOrNull { it in (antes.porcentaje + 1)..porcentaje }
}

val HITOS_META = listOf(25, 50, 75, 100)

private const val DIAS_SEMANA = 7
