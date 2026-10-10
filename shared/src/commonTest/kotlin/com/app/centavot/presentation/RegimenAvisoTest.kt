package com.app.centavot.presentation

import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.PeriodoTope
import com.app.centavot.domain.model.RegimenTributario
import com.app.centavot.domain.model.TipoRegimen
import com.app.centavot.presentation.screens.regimen.mensajeRegimenGuardado
import kotlin.test.Test
import kotlin.test.assertEquals

class RegimenAvisoTest {
    @Test
    fun avisoDeRegimenMensual() {
        val rus = RegimenTributario(TipoRegimen.RUS, Monto.soles(8_000), PeriodoTope.MENSUAL, "RUS · Categoría 2")
        assertEquals("Ahora estás en RUS · Categoría 2. Tu tope es S/ 8,000.00 al mes.", mensajeRegimenGuardado(rus))
    }

    @Test
    fun avisoDeRegimenAnual() {
        val rer = RegimenTributario(TipoRegimen.RER, Monto.soles(525_000), PeriodoTope.ANUAL, "RER")
        assertEquals("Ahora estás en RER. Tu tope es S/ 525,000.00 al año.", mensajeRegimenGuardado(rer))
    }
}
