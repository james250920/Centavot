package com.app.centavot.presentation.components

import com.app.centavot.domain.model.Monto
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FormatoTest {

    @Test
    fun formateaMontosEnSoles() {
        assertEquals("S/ 0.05", Monto(5).formatear())
        assertEquals("S/ 12.50", Monto(1_250).formatear())
        assertEquals("S/ 1,234.56", Monto(123_456).formatear())
        assertEquals("S/ 525,000.00", Monto.soles(525_000).formatear())
        assertEquals("-S/ 3.00", Monto(-300).formatear())
    }

    @Test
    fun parseaLoQueEscribeElUsuario() {
        assertEquals(Monto(1_200), parsearMonto("12"))
        assertEquals(Monto(1_250), parsearMonto("12.5"))
        assertEquals(Monto(1_250), parsearMonto("12,50"))
        assertEquals(Monto(1_200), parsearMonto("12."))
        assertNull(parsearMonto(""))
        assertNull(parsearMonto("abc"))
        assertNull(parsearMonto("1.234"))
    }

    @Test
    fun textoEditableEsInversoDelParseo() {
        listOf(Monto(1_200), Monto(1_250), Monto(5)).forEach {
            assertEquals(it, parsearMonto(it.comoTextoEditable()))
        }
    }

    @Test
    fun filtraLaEntradaDelCampoMonto() {
        assertTrue(esEntradaDeMontoValida(""))
        assertTrue(esEntradaDeMontoValida("12,5"))
        assertFalse(esEntradaDeMontoValida("12.345"))
        assertFalse(esEntradaDeMontoValida("1a"))
        assertFalse(esEntradaDeMontoValida("1.2.3"))
    }

    @Test
    fun formateaFechas() {
        val hoy = LocalDate(2026, 9, 23)
        assertEquals("Hoy", hoy.formatearRelativo(hoy))
        assertEquals("Ayer", LocalDate(2026, 9, 22).formatearRelativo(hoy))
        assertEquals("5 de agosto", LocalDate(2026, 8, 5).formatearRelativo(hoy))
        assertEquals("31 de diciembre de 2025", LocalDate(2025, 12, 31).formatearRelativo(hoy))
        assertEquals("Septiembre 2026", YearMonth(2026, 9).formatear())
    }
}
