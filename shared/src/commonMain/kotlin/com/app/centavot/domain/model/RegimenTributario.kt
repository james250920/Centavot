package com.app.centavot.domain.model

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
)
