package com.app.centavot.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PerfilYReporteTest {

    @Test
    fun metaDeAhorroSegunLaTasa() {
        val perfil = Perfil("Mari", Rubro.COMERCIO, Monto.soles(2_500), TasaAhorro(125))

        assertEquals(Monto(31_250), perfil.metaAhorro)
    }

    @Test
    fun laTasaNoPasaDe100PorCiento() {
        assertFailsWith<IllegalArgumentException> { TasaAhorro(1_001) }
    }

    @Test
    fun reporteEnCsv() {
        val reporte = ReporteSunat(
            periodo = YearMonth(2026, 9),
            regimen = RegimenTributario(TipoRegimen.RUS, Monto.soles(5_000), PeriodoTope.MENSUAL, "RUS · Categoría 1"),
            gastos = listOf(
                Gasto("2", Monto(1_234_50), LocalDate(2026, 9, 20), OrigenGasto.TEXTO, EstadoGasto.CONFIRMADO, Categoria.NEGOCIO),
                Gasto(
                    "1", Monto(2_500), LocalDate(2026, 9, 3), OrigenGasto.TEXTO, EstadoGasto.CONFIRMADO, Categoria.NEGOCIO,
                    subcategoria = SubcategoriaGasto.SERVICIOS, descripcion = "Recibo \"luz\"",
                ),
            ),
        )

        val lineas = reporte.aCsv().removePrefix("﻿").lines()

        assertEquals("Reporte de gastos de negocio,2026-09", lineas[0])
        assertEquals("Fecha,Descripción,Categoría,Proveedor,Monto (S/)", lineas[3])
        assertEquals("2026-09-03,\"Recibo \"\"luz\"\"\",\"Luz, agua, internet\",,25.00", lineas[4])
        assertEquals("2026-09-20,,,,1234.50", lineas[5])
        assertEquals(",,,Total,1259.50", lineas[6])
    }
}
