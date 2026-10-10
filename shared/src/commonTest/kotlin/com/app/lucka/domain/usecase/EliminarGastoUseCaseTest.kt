package com.app.lucka.domain.usecase

import com.app.lucka.domain.model.Categoria
import com.app.lucka.domain.model.EstadoGasto
import com.app.lucka.domain.model.Gasto
import com.app.lucka.domain.model.Monto
import com.app.lucka.domain.model.OrigenGasto
import com.app.lucka.fakes.FakeActividadRepository
import com.app.lucka.fakes.FakeGastoRepository
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
