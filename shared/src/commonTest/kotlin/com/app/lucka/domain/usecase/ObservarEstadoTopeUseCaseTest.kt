package com.app.lucka.domain.usecase

import com.app.lucka.domain.model.Categoria
import com.app.lucka.domain.model.EstadoGasto
import com.app.lucka.domain.model.Gasto
import com.app.lucka.domain.model.Ingreso
import com.app.lucka.domain.model.MedidaTope
import com.app.lucka.domain.model.Monto
import com.app.lucka.domain.model.OrigenGasto
import com.app.lucka.domain.model.PeriodoTope
import com.app.lucka.domain.model.RegimenTributario
import com.app.lucka.domain.model.TipoRegimen
import com.app.lucka.fakes.FakeGastoRepository
import com.app.lucka.fakes.FakeIngresoRepository
import com.app.lucka.fakes.FakeRegimenRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ObservarEstadoTopeUseCaseTest {

    private val hoy = LocalDate(2026, 9, 23)

    private fun gasto(id: String, soles: Long, fecha: LocalDate, categoria: Categoria) = Gasto(
        id = id,
        monto = Monto.soles(soles),
        fecha = fecha,
        origen = OrigenGasto.TEXTO,
        estado = EstadoGasto.CONFIRMADO,
        categoria = categoria,
    )

    private val gastos = FakeGastoRepository(
        listOf(
            gasto("negocio-sep", 3_000, LocalDate(2026, 9, 10), Categoria.NEGOCIO),
            gasto("negocio-sep-2", 1_000, LocalDate(2026, 9, 23), Categoria.NEGOCIO),
            gasto("personal-sep", 2_000, LocalDate(2026, 9, 15), Categoria.PERSONAL),
            gasto("negocio-ago", 4_000, LocalDate(2026, 8, 31), Categoria.NEGOCIO),
        ),
    )

    private val ingresos = FakeIngresoRepository(
        listOf(
            Ingreso("venta-sep", Monto.soles(2_000), LocalDate(2026, 9, 5)),
            Ingreso("sueldo-sep", Monto.soles(1_500), LocalDate(2026, 9, 1), Categoria.PERSONAL),
            Ingreso("venta-ene", Monto.soles(6_000), LocalDate(2026, 1, 20)),
        ),
    )

    private fun observar(regimen: RegimenTributario?) =
        ObservarEstadoTopeUseCase(ingresos, gastos, FakeRegimenRepository(regimen), reloj = { hoy })

    @Test
    fun sinRegimenNoHayEstado() = runTest {
        assertNull(observar(null)().first())
    }

    @Test
    fun nuevoRusMideVentasYComprasDelMes() = runTest {
        val estado = observar(RegimenTributario(TipoRegimen.RUS, Monto.soles(8_000), PeriodoTope.MENSUAL))().first()!!

        // Solo ventas del negocio de septiembre: el sueldo personal no cuenta.
        assertEquals(Monto.soles(2_000), estado.ventas.acumulado)
        // Compras = gastos de negocio de septiembre.
        assertEquals(Monto.soles(4_000), estado.compras!!.acumulado)
        assertEquals(MedidaTope.COMPRAS, estado.principal.first)
        assertEquals(50, estado.principal.second.porcentaje)
    }

    @Test
    fun rerMideSoloLasVentasDelAnio() = runTest {
        val estado = observar(RegimenTributario(TipoRegimen.RER, Monto.soles(525_000), PeriodoTope.ANUAL))().first()!!

        assertEquals(Monto.soles(8_000), estado.ventas.acumulado)
        assertNull(estado.compras)
        assertEquals(MedidaTope.VENTAS, estado.principal.first)
    }
}
