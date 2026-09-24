package com.app.centavot.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.yearMonth

enum class TipoRegimen { RUS, RER }

enum class PeriodoTope { MENSUAL, ANUAL }

/**
 * Régimen del usuario con el tope vigente. El tope viene del backend porque
 * cambia por norma SUNAT; nunca se escribe fijo en la app.
 */
data class RegimenTributario(
    val tipo: TipoRegimen,
    val tope: Monto,
    val periodo: PeriodoTope,
    /** Nombre para mostrar, p. ej. "RUS · Categoría 1". */
    val nombre: String = tipo.name,
)

/** Rango de fechas que cuenta para el tope, según el periodo del régimen. */
fun PeriodoTope.rangoQueContiene(fecha: LocalDate): ClosedRange<LocalDate> = when (this) {
    PeriodoTope.MENSUAL -> fecha.yearMonth.let { it.firstDay..it.lastDay }
    PeriodoTope.ANUAL -> LocalDate(fecha.year, 1, 1)..LocalDate(fecha.year, 12, 31)
}
