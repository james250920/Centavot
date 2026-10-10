package com.app.centavot.presentation

import com.app.centavot.presentation.screens.cobros.CobroUiState
import com.app.centavot.presentation.screens.cobros.detallesAbiertosAlInicio
import com.app.centavot.presentation.screens.cobros.resumenDetallesCobro
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CobroDetallesTest {
    private val hoy = LocalDate(2026, 10, 10)

    @Test
    fun resumenDeUnCobroNuevo() {
        assertEquals("Se suma a tus ventas · Desde hoy", resumenDetallesCobro(false, true, hoy, hoy))
        assertEquals("No se suma a tus ventas · Desde ayer", resumenDetallesCobro(false, false, LocalDate(2026, 10, 9), hoy))
    }

    @Test
    fun alEditarSoloMuestraLaFecha() {
        assertEquals("Desde 4 de octubre", resumenDetallesCobro(true, true, LocalDate(2026, 10, 4), hoy))
    }

    @Test
    fun detallesAbiertosSoloSiHayAlgoDistinto() {
        val base = CobroUiState(esEdicion = false, hoy = hoy, fecha = hoy)
        assertFalse(base.detallesAbiertosAlInicio())
        assertTrue(base.copy(contarComoVenta = false).detallesAbiertosAlInicio())
        assertTrue(base.copy(fecha = LocalDate(2026, 10, 1)).detallesAbiertosAlInicio())
    }
}
