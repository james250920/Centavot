package com.app.centavot.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ReporteSunatTest {

    private val regimen = RegimenTributario(TipoRegimen.RUS, Monto.soles(8_000), PeriodoTope.MENSUAL)

    private fun gasto(centimos: Long, categoria: Categoria?) = Gasto(
        id = "g-$centimos",
        monto = Monto(centimos),
        fecha = LocalDate(2026, 9, 15),
        origen = OrigenGasto.FOTO,
        estado = EstadoGasto.CONFIRMADO,
        categoria = categoria,
    )

    @Test
    fun sumaLosGastosDeNegocio() {
        val reporte = ReporteSunat(
            periodo = YearMonth(2026, 9),
            regimen = regimen,
            gastos = listOf(gasto(1_050, Categoria.NEGOCIO), gasto(2_000, Categoria.NEGOCIO)),
        )

        assertEquals(Monto(3_050), reporte.total)
    }

    @Test
    fun reporteVacioSumaCero() {
        assertEquals(Monto.CERO, ReporteSunat(YearMonth(2026, 9), regimen, emptyList()).total)
    }

    @Test
    fun rechazaGastosPersonalesOSinClasificar() {
        assertFailsWith<IllegalArgumentException> {
            ReporteSunat(YearMonth(2026, 9), regimen, listOf(gasto(500, Categoria.PERSONAL)))
        }
        assertFailsWith<IllegalArgumentException> {
            ReporteSunat(YearMonth(2026, 9), regimen, listOf(gasto(500, null)))
        }
    }
}
