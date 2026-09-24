package com.app.centavot.domain.usecase

import com.app.centavot.domain.model.Categoria
import com.app.centavot.domain.model.EstadoGasto
import com.app.centavot.domain.model.Gasto
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.OrigenGasto
import com.app.centavot.fakes.FakeGastoRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GuardarGastoUseCaseTest {

    private val hoy = LocalDate(2026, 9, 23)
    private val repositorio = FakeGastoRepository()
    private val guardar = GuardarGastoUseCase(repositorio, reloj = { hoy }, generarId = { "nuevo-id" })

    @Test
    fun registraUnGastoNuevoConfirmado() = runTest {
        val resultado = guardar(null, Monto(1_250), Categoria.NEGOCIO, hoy, "  Mercadería  ")

        val gasto = assertIs<GuardarGastoUseCase.Resultado.Guardado>(resultado).gasto
        assertEquals("nuevo-id", gasto.id)
        assertEquals("Mercadería", gasto.descripcion)
        assertEquals(OrigenGasto.TEXTO, gasto.origen)
        assertEquals(EstadoGasto.CONFIRMADO, gasto.estado)
        assertTrue(gasto.corregidoManualmente)
        assertEquals(listOf(gasto), repositorio.gastos.value)
    }

    @Test
    fun descripcionEnBlancoSeGuardaComoNull() = runTest {
        val resultado = guardar(null, Monto(100), Categoria.PERSONAL, hoy, "   ")

        assertNull(assertIs<GuardarGastoUseCase.Resultado.Guardado>(resultado).gasto.descripcion)
    }

    @Test
    fun rechazaMontoCeroYFechaFutura() = runTest {
        assertEquals(GuardarGastoUseCase.Resultado.MontoInvalido, guardar(null, Monto.CERO, Categoria.NEGOCIO, hoy, null))
        assertEquals(
            GuardarGastoUseCase.Resultado.FechaFutura,
            guardar(null, Monto(100), Categoria.NEGOCIO, LocalDate(2026, 9, 24), null),
        )
        assertTrue(repositorio.gastos.value.isEmpty())
    }

    @Test
    fun editarConservaIdYOrigen() = runTest {
        val original = Gasto(
            id = "g1",
            monto = Monto(500),
            fecha = LocalDate(2026, 9, 1),
            origen = OrigenGasto.FOTO,
            estado = EstadoGasto.PROCESADO,
            categoria = Categoria.PERSONAL,
        )
        repositorio.gastos.value = listOf(original)

        guardar("g1", Monto(800), Categoria.NEGOCIO, LocalDate(2026, 9, 2), null)

        val editado = repositorio.gastos.value.single()
        assertEquals("g1", editado.id)
        assertEquals(OrigenGasto.FOTO, editado.origen)
        assertEquals(Monto(800), editado.monto)
        assertEquals(Categoria.NEGOCIO, editado.categoria)
        assertEquals(EstadoGasto.CONFIRMADO, editado.estado)
    }

    @Test
    fun editarUnGastoQueNoExiste() = runTest {
        assertEquals(
            GuardarGastoUseCase.Resultado.NoEncontrado,
            guardar("no-existe", Monto(100), Categoria.NEGOCIO, hoy, null),
        )
    }
}
