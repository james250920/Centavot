package com.app.lucka.presentation

import com.app.lucka.domain.model.Categoria
import com.app.lucka.domain.model.EstadoGasto
import com.app.lucka.domain.model.Gasto
import com.app.lucka.domain.model.Ingreso
import com.app.lucka.domain.model.Monto
import com.app.lucka.domain.model.OrigenGasto
import com.app.lucka.presentation.components.Movimiento
import com.app.lucka.presentation.screens.movimientos.FiltroMovimientos
import com.app.lucka.presentation.screens.movimientos.GrupoDia
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class FiltroMovimientosTest {

    private val hoy = LocalDate(2026, 10, 6)
    private val venta = Movimiento.Entrada(Ingreso("v", Monto.soles(40), hoy))
    private val sueldo = Movimiento.Entrada(Ingreso("s", Monto.soles(1_000), hoy, Categoria.PERSONAL))
    private val compra = Movimiento.Salida(Gasto("g", Monto.soles(15), hoy, OrigenGasto.TEXTO, EstadoGasto.CONFIRMADO, Categoria.NEGOCIO))
    private val todos = listOf(venta, sueldo, compra)

    @Test
    fun elFiltroVentasNoIncluyeIngresosPersonales() {
        assertEquals(listOf(venta), todos.filter { FiltroMovimientos.VENTAS.incluye(it) })
        assertEquals(listOf(sueldo), todos.filter { FiltroMovimientos.INGRESOS_PERSONALES.incluye(it) })
        assertEquals(listOf(compra), todos.filter { FiltroMovimientos.NEGOCIO.incluye(it) })
    }

    @Test
    fun loVendidoDelDiaNoSumaElSueldo() {
        val dia = GrupoDia(hoy, todos)

        assertEquals(Monto.soles(40), dia.vendido)
        assertEquals(Monto.soles(1_000), dia.ingresosPersonales)
        assertEquals(Monto.soles(15), dia.gastado)
    }
}
