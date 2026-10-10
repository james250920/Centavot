package com.app.centavot.domain.usecase

import com.app.centavot.domain.model.Categoria
import com.app.centavot.domain.model.EstadoGasto
import com.app.centavot.domain.model.Gasto
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.OrigenGasto
import com.app.centavot.fakes.FakeActividadRepository
import com.app.centavot.fakes.FakeGastoRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EliminarGastoUseCaseTest {

    @Test
    fun eliminaYLoAnotaEnActividad() = runTest {
        val hoy = LocalDate(2026, 9, 29)
        val gastos = FakeGastoRepository(
            listOf(Gasto("g1", Monto.soles(25), hoy, OrigenGasto.TEXTO, EstadoGasto.CONFIRMADO, Categoria.PERSONAL, descripcion = "Cena")),
        )
        val actividades = FakeActividadRepository()

        EliminarGastoUseCase(gastos, actividades) { hoy }("g1")

        assertTrue(gastos.gastos.value.isEmpty())
        assertEquals("Eliminaste el gasto \"Cena\" de S/ 25.00.", actividades.actividades.value.single().descripcion)
    }
}
