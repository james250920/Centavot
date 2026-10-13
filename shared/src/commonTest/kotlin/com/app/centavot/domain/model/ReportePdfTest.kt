package com.app.centavot.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** P5: el contenido del PDF para el contador. */
class ReportePdfTest {

    private val hoy = LocalDate(2026, 10, 13)

    private fun gasto(id: String, soles: Long, dia: Int, sub: SubcategoriaGasto?, descripcion: String? = null) = Gasto(
        id, Monto.soles(soles), LocalDate(2026, 10, dia), OrigenGasto.TEXTO, EstadoGasto.CONFIRMADO, Categoria.NEGOCIO,
        subcategoria = sub, descripcion = descripcion,
    )

    private val reporte = ReporteSunat(
        periodo = YearMonth(2026, 10),
        regimen = RegimenTributario(TipoRegimen.RUS, Monto.soles(8_000), PeriodoTope.MENSUAL, "RUS · Categoría 2"),
        gastos = listOf(
            gasto("g2", 500, 1, SubcategoriaGasto.ALQUILER, "Alquiler del local"),
            gasto("g1", 700, 6, SubcategoriaGasto.MERCADERIA, "Makro"),
        ),
        ventas = listOf(
            Ingreso("v1", Monto(350), LocalDate(2026, 10, 2), descripcion = "Gaseosa"),
            Ingreso("v2", Monto(4_500), LocalDate(2026, 10, 2), descripcion = "Abarrotes"),
            Ingreso("v3", Monto.soles(1_500), LocalDate(2026, 10, 1)),
        ),
    )

    @Test
    fun llevaElMesElRegimenYElAvisoDeSunat() {
        val documento = reporte.aDocumento(hoy)

        assertEquals("Resumen para el contador", documento.titulo)
        assertEquals("Octubre 2026 · RUS · Categoría 2", documento.subtitulo)
        assertTrue("13/10/2026" in documento.pie && "no envía nada a SUNAT" in documento.pie, documento.pie)
    }

    @Test
    fun elResumenDiceGananciaOPerdida() {
        val resumen = reporte.aDocumento(hoy).bloques.first() as BloqueReporte.Totales

        assertEquals(
            listOf(
                FilaTotal("Ventas", "S/ 1,548.50"),
                FilaTotal("Compras y gastos", "S/ 1,200.00"),
                FilaTotal("Ganancia", "S/ 348.50", destacada = true),
            ),
            resumen.filas,
        )
    }

    @Test
    fun lasVentasVanPorDiaYEnOrden() {
        val ventas = reporte.aDocumento(hoy).bloques.filterIsInstance<BloqueReporte.Tabla>().single { it.titulo == "Ventas por día" }

        assertEquals(
            listOf(listOf("01/10/2026", "1", "S/ 1,500.00"), listOf("02/10/2026", "2", "S/ 48.50")),
            ventas.filas,
        )
        assertEquals(listOf("Total", "3", "S/ 1,548.50"), ventas.total)
    }

    @Test
    fun losGastosVanPorCategoriaYEnDetalle() {
        val tablas = reporte.aDocumento(hoy).bloques.filterIsInstance<BloqueReporte.Tabla>()

        assertEquals(
            listOf(listOf("Mercadería e insumos", "S/ 700.00"), listOf("Alquiler", "S/ 500.00")),
            tablas.single { it.titulo == "Compras y gastos por categoría" }.filas,
        )
        assertEquals(
            listOf("01/10/2026", "Alquiler del local", "Alquiler", "", "S/ 500.00"),
            tablas.single { it.titulo == "Detalle de compras y gastos" }.filas.first(),
        )
    }

    @Test
    fun sinMovimientosSoloVaElResumen() {
        val vacio = ReporteSunat(YearMonth(2026, 10), reporte.regimen, gastos = emptyList())

        assertEquals(1, vacio.aDocumento(hoy).bloques.size)
    }
}
