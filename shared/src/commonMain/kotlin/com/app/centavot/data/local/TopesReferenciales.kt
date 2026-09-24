package com.app.centavot.data.local

import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.PeriodoTope
import com.app.centavot.domain.model.RegimenTributario
import com.app.centavot.domain.model.TipoRegimen

/**
 * Topes referenciales de SUNAT mientras no exista backend.
 * Cuando exista, estas opciones vendrán de la API y este archivo se elimina.
 */
internal val TOPES_REFERENCIALES = listOf(
    RegimenTributario(TipoRegimen.RUS, Monto.soles(5_000), PeriodoTope.MENSUAL, nombre = "RUS · Categoría 1"),
    RegimenTributario(TipoRegimen.RUS, Monto.soles(8_000), PeriodoTope.MENSUAL, nombre = "RUS · Categoría 2"),
    RegimenTributario(TipoRegimen.RER, Monto.soles(525_000), PeriodoTope.ANUAL, nombre = "RER"),
)
