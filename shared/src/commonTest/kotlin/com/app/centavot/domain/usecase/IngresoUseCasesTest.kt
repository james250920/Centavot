package com.app.centavot.domain.usecase

import com.app.centavot.domain.model.Categoria
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.PeriodoTope
import com.app.centavot.domain.model.RegimenTributario
import com.app.centavot.domain.model.TipoEventoUso
import com.app.centavot.domain.model.TipoRegimen
import com.app.centavot.domain.model.VentaFrecuente
import com.app.centavot.fakes.FakeActividadRepository
import com.app.centavot.fakes.FakeGastoRepository
import com.app.centavot.fakes.FakeIngresoRepository
import com.app.centavot.fakes.FakeNotificacionRepository
import com.app.centavot.fakes.FakeRegimenRepository
import com.app.centavot.fakes.FakeUsoRepository
import com.app.centavot.fakes.revisarAlertaTope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class IngresoUseCasesTest {

    private val hoy = LocalDate(2026, 10, 5)
    private val ingresos = FakeIngresoRepository()
    private val actividades = FakeActividadRepository()
    private val notificaciones = FakeNotificacionRepository()
    private val uso = FakeUsoRepository()
    private val regimenes = FakeRegimenRepository(RegimenTributario(TipoRegimen.RUS, Monto.soles(5_000), PeriodoTope.MENSUAL))
    private var siguiente = 0
    private val guardar = GuardarIngresoUseCase(
        ingresos,
        reloj = { hoy },
        generarId = { "venta-${++siguiente}" },
        actividades = actividades,
        uso = uso,
        revisarAlertaTope = revisarAlertaTope(FakeGastoRepository(), regimenes, notificaciones, { hoy }, ingresos),
    )
    private val eliminar = EliminarIngresoUseCase(ingresos, actividades) { hoy }

    @Test
    fun registraUnaVentaSueltaYLaCuentaComoUso() = runTest {
        val resultado = guardar(null, Monto(350), Categoria.NEGOCIO, hoy, "  Gaseosa ")

        val venta = assertIs<GuardarIngresoUseCase.Resultado.Guardado>(resultado).ingreso
        assertEquals("Gaseosa", venta.descripcion)
        assertEquals("Registraste una venta \"Gaseosa\" de S/ 3.50.", actividades.actividades.value.single().descripcion)
        assertEquals(TipoEventoUso.REGISTRO, uso.eventos.value.single().tipo)
    }

    @Test
    fun rechazaMontoCeroYFechaFutura() = runTest {
        assertEquals(GuardarIngresoUseCase.Resultado.MontoInvalido, guardar(null, Monto.CERO, Categoria.NEGOCIO, hoy, null))
        assertEquals(
            GuardarIngresoUseCase.Resultado.FechaFutura,
            guardar(null, Monto(100), Categoria.NEGOCIO, LocalDate(2026, 10, 6), null),
        )
        assertTrue(ingresos.ingresos.value.isEmpty())
    }

    @Test
    fun editarNoCuentaComoRegistroNuevo() = runTest {
        val venta = assertIs<GuardarIngresoUseCase.Resultado.Guardado>(guardar(null, Monto(100), Categoria.NEGOCIO, hoy, null)).ingreso
        guardar(venta.id, Monto(250), Categoria.NEGOCIO, hoy, "Pan")

        assertEquals(Monto(250), ingresos.ingresos.value.single().monto)
        assertEquals(1, uso.eventos.value.size)
    }

    @Test
    fun lasVentasDisparanLaAlertaDeTopeDelNuevoRus() = runTest {
        guardar(null, Monto.soles(4_100), Categoria.NEGOCIO, hoy, null)

        val aviso = notificaciones.notificaciones.value.single()
        assertEquals("Pasaste el 80 % de tu tope", aviso.asunto)
        assertTrue("en ventas del negocio" in aviso.mensaje)
    }

    @Test
    fun unIngresoPersonalNoCuentaParaElTope() = runTest {
        guardar(null, Monto.soles(6_000), Categoria.PERSONAL, hoy, "Sueldo")

        assertTrue(notificaciones.notificaciones.value.isEmpty())
    }

    @Test
    fun eliminarQuedaAnotado() = runTest {
        val venta = assertIs<GuardarIngresoUseCase.Resultado.Guardado>(guardar(null, Monto(500), Categoria.NEGOCIO, hoy, "Menú")).ingreso
        eliminar(venta.id)

        assertTrue(ingresos.ingresos.value.isEmpty())
        assertEquals("Eliminaste la venta \"Menú\" de S/ 5.00.", actividades.actividades.value.last().descripcion)
    }

    @Test
    fun ventasFrecuentesOrdenadasPorRepeticion() = runTest {
        repeat(3) { guardar(null, Monto(350), Categoria.NEGOCIO, hoy, "Gaseosa") }
        guardar(null, Monto(1_000), Categoria.NEGOCIO, hoy, "Menú")
        guardar(null, Monto(800), Categoria.NEGOCIO, hoy, null)
        guardar(null, Monto(9_000), Categoria.PERSONAL, hoy, "Sueldo")

        val frecuentes = ObservarVentasFrecuentesUseCase(ingresos)().first()

        assertEquals(listOf(VentaFrecuente("Gaseosa", Monto(350)), VentaFrecuente("Menú", Monto(1_000))), frecuentes)
    }
}
