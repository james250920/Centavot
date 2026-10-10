package com.app.centavot.presentation

import com.app.centavot.domain.model.Categoria
import com.app.centavot.domain.model.EstadoGasto
import com.app.centavot.domain.model.Gasto
import com.app.centavot.domain.model.Ingreso
import com.app.centavot.domain.model.Modo
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.OrigenGasto
import com.app.centavot.presentation.components.Movimiento
import com.app.centavot.presentation.screens.movimientos.FiltroMovimientos
import com.app.centavot.presentation.screens.movimientos.GrupoDia
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FiltroMovimientosTest {

    private val hoy = LocalDate(2026, 10, 6)
    private val venta = Movimiento.Entrada(Ingreso("v", Monto.soles(40), hoy))
    private val retiro = Movimiento.Entrada(Ingreso("r", Monto.soles(20), hoy, Categoria.PERSONAL, retiroDelNegocio = true))
    private val compra = Movimiento.Salida(Gasto("g", Monto.soles(15), hoy, OrigenGasto.TEXTO, EstadoGasto.CONFIRMADO, Categoria.NEGOCIO))
    private val negocio = listOf(venta, retiro, compra)

    @Test
    fun enElNegocioElRetiroNoEsUnaVenta() {
        assertEquals(listOf(venta), negocio.filter { FiltroMovimientos.ENTRADAS.incluye(it, Modo.NEGOCIO) })
        assertEquals(listOf(retiro), negocio.filter { FiltroMovimientos.PARA_LA_CASA.incluye(it, Modo.NEGOCIO) })
        assertEquals(listOf(compra), negocio.filter { FiltroMovimientos.GASTOS.incluye(it, Modo.NEGOCIO) })
    }

    @Test
    fun enLoPersonalElRetiroEsUnIngresoYNoHayFiltroParaLaCasa() {
        assertTrue(FiltroMovimientos.ENTRADAS.incluye(retiro, Modo.PERSONAL))
        assertFalse(FiltroMovimientos.PARA_LA_CASA in FiltroMovimientos.para(Modo.PERSONAL))
        assertEquals("Ingresos", FiltroMovimientos.ENTRADAS.etiqueta(Modo.PERSONAL))
        assertEquals("Ventas", FiltroMovimientos.ENTRADAS.etiqueta(Modo.NEGOCIO))
    }

    @Test
    fun loQueEntroYSalioDelDiaDependeDelModo() {
        val enElNegocio = GrupoDia(hoy, negocio, Modo.NEGOCIO)
        assertEquals(Monto.soles(40), enElNegocio.entro)
        assertEquals(Monto.soles(35), enElNegocio.salio)

        val enLoPersonal = GrupoDia(hoy, listOf(retiro), Modo.PERSONAL)
        assertEquals(Monto.soles(20), enLoPersonal.entro)
        assertEquals(Monto.CERO, enLoPersonal.salio)
    }
}
