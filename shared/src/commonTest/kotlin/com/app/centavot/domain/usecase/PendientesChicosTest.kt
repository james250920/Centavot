package com.app.centavot.domain.usecase

import com.app.centavot.core.util.Reloj
import com.app.centavot.domain.model.Categoria
import com.app.centavot.domain.model.EstadoGasto
import com.app.centavot.domain.model.Gasto
import com.app.centavot.domain.model.Ingreso
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.Notificacion
import com.app.centavot.domain.model.OrigenGasto
import com.app.centavot.domain.model.Periodo
import com.app.centavot.domain.model.PeriodoTope
import com.app.centavot.domain.model.RegimenTributario
import com.app.centavot.domain.model.Rubro
import com.app.centavot.domain.model.TipoRegimen
import com.app.centavot.fakes.FakeGastoRepository
import com.app.centavot.fakes.FakeIngresoRepository
import com.app.centavot.fakes.FakeNotificacionRepository
import com.app.centavot.fakes.FakeRegimenRepository
import com.app.centavot.fakes.revisarAlertaTope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** P8, P9 y P10 de los pendientes. */
class PendientesChicosTest {

    private val lunes = LocalDate(2026, 10, 12)
    private val martes = LocalDate(2026, 10, 13)

    /** Un reloj cuyo día se puede adelantar, como pasa con la app abierta a medianoche. */
    private class RelojQueAvanza(inicial: LocalDate) : Reloj {
        val dia = MutableStateFlow(inicial)
        override fun hoy(): LocalDate = dia.value
        override fun dias(): Flow<LocalDate> = dia
    }

    @Test
    fun tuCajaPasaAlDiaNuevoSinCerrarLaApp() = runTest {
        val reloj = RelojQueAvanza(lunes)
        val ingresos = FakeIngresoRepository(
            listOf(
                Ingreso("l", Monto.soles(80), lunes),
                Ingreso("m", Monto.soles(15), martes),
            ),
        )
        val caja = ObservarResumenPeriodoUseCase(ingresos, FakeGastoRepository(), reloj)

        val vistos = mutableListOf<Monto>()
        val trabajo = launch { caja(Periodo.HOY).take(2).toList().forEach { vistos += it.ventas } }
        runCurrent()
        reloj.dia.value = martes
        trabajo.join()

        assertEquals(listOf(Monto.soles(80), Monto.soles(15)), vistos)
    }

    private fun compra(soles: Long) = Gasto(
        id = "c$soles",
        monto = Monto.soles(soles),
        fecha = martes,
        origen = OrigenGasto.TEXTO,
        estado = EstadoGasto.CONFIRMADO,
        categoria = Categoria.NEGOCIO,
    )

    @Test
    fun elAvisoDeComprasNoSeRepiteSiYaLlegoConElIdAnterior() = runTest {
        val rus = RegimenTributario(TipoRegimen.RUS, Monto.soles(8_000), PeriodoTope.MENSUAL)
        val notificaciones = FakeNotificacionRepository()
        // El aviso del 80 % que dejó la primera versión, que solo vigilaba los gastos del negocio.
        notificaciones.agregarSiNoExiste(
            Notificacion("tope-RUS-800000-2026-10-80", "Pasaste el 80 % de tu tope", "…", martes.atTime(9, 0)),
        )
        val revisar = revisarAlertaTope(
            gastos = FakeGastoRepository(listOf(compra(6_500))),
            regimenes = FakeRegimenRepository(rus),
            notificaciones = notificaciones,
            reloj = { martes },
            ingresos = FakeIngresoRepository(listOf(Ingreso("v", Monto.soles(6_600), martes))),
        )

        revisar()

        val ids = notificaciones.notificaciones.value.map { it.id }
        assertFalse("tope-RUS-800000-COMPRAS-2026-10-80" in ids, "El aviso de compras se repitió: $ids")
        assertTrue("tope-RUS-800000-VENTAS-2026-10-80" in ids, "El de ventas es nuevo y sí tiene que llegar: $ids")
    }

    @Test
    fun sinElIdAnteriorElAvisoDeComprasLlega() = runTest {
        val rus = RegimenTributario(TipoRegimen.RUS, Monto.soles(8_000), PeriodoTope.MENSUAL)
        val notificaciones = FakeNotificacionRepository()
        revisarAlertaTope(
            gastos = FakeGastoRepository(listOf(compra(6_500))),
            regimenes = FakeRegimenRepository(rus),
            notificaciones = notificaciones,
            reloj = { martes },
        )()

        assertEquals(listOf("tope-RUS-800000-COMPRAS-2026-10-80"), notificaciones.notificaciones.value.map { it.id })
    }

    @Test
    fun elRubroDeServiciosNoInvitaAProfesionalesIndependientes() {
        assertFalse("profesional" in Rubro.SERVICIOS.etiqueta.lowercase())
    }
}
