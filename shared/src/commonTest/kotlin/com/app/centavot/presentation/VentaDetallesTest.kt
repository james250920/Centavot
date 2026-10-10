package com.app.centavot.presentation

import com.app.centavot.presentation.screens.venta.VentaUiState
import com.app.centavot.presentation.screens.venta.detallesAbiertosAlInicio
import com.app.centavot.presentation.screens.venta.resumenDetallesVenta
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VentaDetallesTest {
    private val hoy = LocalDate(2026, 10, 10)

    @Test
    fun resumenDeLoHabitual() {
        assertEquals("Hoy", resumenDetallesVenta(hoy, hoy, ""))
    }

    @Test
    fun resumenConCambios() {
        assertEquals(
            "Ayer · Pan",
            resumenDetallesVenta(LocalDate(2026, 10, 9), hoy, "  Pan "),
        )
    }

    @Test
    fun detallesPlegadosEnUnaVentaNueva() {
        assertFalse(VentaUiState(esEdicion = false, hoy = hoy, fecha = hoy).detallesAbiertosAlInicio())
    }

    @Test
    fun detallesAbiertosSiHayAlgoDistinto() {
        val base = VentaUiState(esEdicion = false, hoy = hoy, fecha = hoy)
        assertTrue(base.copy(esEdicion = true).detallesAbiertosAlInicio())
        assertTrue(base.copy(descripcion = "Pan").detallesAbiertosAlInicio())
        assertTrue(base.copy(fecha = LocalDate(2026, 10, 8)).detallesAbiertosAlInicio())
        assertTrue(base.copy(errorFecha = "La fecha no puede ser futura").detallesAbiertosAlInicio())
    }
}
