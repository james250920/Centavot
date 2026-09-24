package com.app.centavot.domain.usecase

import com.app.centavot.domain.model.Categoria
import com.app.centavot.domain.model.EstadoGasto
import com.app.centavot.domain.model.Gasto
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.OrigenGasto
import com.app.centavot.domain.model.PeriodoTope
import com.app.centavot.domain.model.RegimenTributario
import com.app.centavot.domain.model.TipoRegimen
import com.app.centavot.fakes.FakeGastoRepository
import com.app.centavot.fakes.FakeRegimenRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ObservarProximidadTopeUseCaseTest {

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

    @Test
    fun sinRegimenNoHayProximidad() = runTest {
        val observar = ObservarProximidadTopeUseCase(gastos, FakeRegimenRepository(), reloj = { hoy })

        assertNull(observar().first())
    }

    @Test
    fun regimenMensualSumaSoloNegocioDelMes() = runTest {
        val regimen = RegimenTributario(TipoRegimen.RUS, Monto.soles(8_000), PeriodoTope.MENSUAL)
        val observar = ObservarProximidadTopeUseCase(gastos, FakeRegimenRepository(regimen), reloj = { hoy })

        val proximidad = observar().first()!!

        assertEquals(Monto.soles(4_000), proximidad.acumulado)
        assertEquals(50, proximidad.porcentaje)
    }

    @Test
    fun regimenAnualSumaNegocioDelAnio() = runTest {
        val regimen = RegimenTributario(TipoRegimen.RER, Monto.soles(525_000), PeriodoTope.ANUAL)
        val observar = ObservarProximidadTopeUseCase(gastos, FakeRegimenRepository(regimen), reloj = { hoy })

        assertEquals(Monto.soles(8_000), observar().first()!!.acumulado)
    }
}
