package com.app.centavot.domain.usecase

import com.app.centavot.core.util.Reloj
import com.app.centavot.domain.model.Contacto
import com.app.centavot.domain.model.EstadoCobro
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.PeriodoTope
import com.app.centavot.domain.model.RegimenTributario
import com.app.centavot.domain.model.TipoCobro
import com.app.centavot.domain.model.TipoRegimen
import com.app.centavot.fakes.FakeActividadRepository
import com.app.centavot.fakes.FakeCobroRepository
import com.app.centavot.fakes.FakeGastoRepository
import com.app.centavot.fakes.FakeIngresoRepository
import com.app.centavot.fakes.FakeNotificacionRepository
import com.app.centavot.fakes.FakeRegimenRepository
import com.app.centavot.fakes.FakeUsoRepository
import com.app.centavot.fakes.revisarAlertaTope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** "Deshacer" tras registrar un cobro, cobrarlo o abonarlo. */
class DeshacerTest {

    private val hoy = LocalDate(2026, 10, 10)
    private val rosa = Contacto("rosa", "Rosa")
    private val cobros = FakeCobroRepository()
    private val ingresos = FakeIngresoRepository()
    private val actividades = FakeActividadRepository()
    private var siguiente = 0
    private val generarId = { "id-${++siguiente}" }
    private val reloj = Reloj { hoy }
    private val guardarIngreso = GuardarIngresoUseCase(
        ingresos, reloj, generarId, actividades, FakeUsoRepository(),
        revisarAlertaTope(
            FakeGastoRepository(),
            FakeRegimenRepository(RegimenTributario(TipoRegimen.RUS, Monto.soles(5_000), PeriodoTope.MENSUAL)),
            FakeNotificacionRepository(), reloj, ingresos,
        ),
    )
    private val registrar = RegistrarCobroYVentaUseCase(RegistrarCobroUseCase(cobros, actividades, reloj, generarId), guardarIngreso)
    private val restaurar = RestaurarCobroUseCase(cobros, actividades, reloj)

    @Test
    fun alRegistrarConVentaDevuelveLaVentaParaPoderDeshacerla() = runTest {
        val registro = registrar(rosa, "Arroz", Monto.soles(30), hoy, TipoCobro.FIADO, Monto.CERO, null, contarComoVenta = true)
        val cobro = assertIs<RegistrarCobroUseCase.Resultado.Registrado>(registro.resultado).cobro
        assertEquals(ingresos.ingresos.value.single().id, registro.ventaId)

        EliminarCobroUseCase(cobros, actividades, reloj)(cobro.id)
        EliminarIngresoUseCase(ingresos, actividades, reloj)(assertNotNull(registro.ventaId))
        assertTrue(cobros.cobros.value.isEmpty())
        assertTrue(ingresos.ingresos.value.isEmpty())
    }

    @Test
    fun sinVentaNoDevuelveVenta() = runTest {
        val registro = registrar(rosa, "Préstamo", Monto.soles(50), hoy, TipoCobro.FIADO, Monto.CERO, null, contarComoVenta = false)
        assertNull(registro.ventaId)
    }

    @Test
    fun deshacerUnAbonoQueCerroElCobroLoDejaComoEstaba() = runTest {
        val registro = registrar(rosa, "Arroz", Monto.soles(30), hoy, TipoCobro.FIADO, Monto.CERO, null, contarComoVenta = false)
        val antes = assertIs<RegistrarCobroUseCase.Resultado.Registrado>(registro.resultado).cobro

        RegistrarAbonoUseCase(cobros, actividades, reloj)(antes.id, Monto.soles(30))
        assertEquals(EstadoCobro.COBRADO, cobros.cobros.value.single().estado)

        restaurar(antes, "el abono de S/ 30.00")
        val despues = cobros.cobros.value.single()
        assertEquals(antes, despues)
        assertEquals(Monto.soles(30), despues.saldo)
    }

    @Test
    fun deshacerElPagoQuedaAnotadoEnActividad() = runTest {
        val registro = registrar(rosa, "Arroz", Monto.soles(30), hoy, TipoCobro.FIADO, Monto.CERO, null, contarComoVenta = false)
        val antes = assertIs<RegistrarCobroUseCase.Resultado.Registrado>(registro.resultado).cobro
        MarcarCobradoUseCase(cobros, actividades, reloj)(antes.id)

        restaurar(antes, "el pago de S/ 30.00")
        assertEquals(EstadoCobro.PENDIENTE, cobros.cobros.value.single().estado)
        assertTrue(actividades.actividades.value.any { it.descripcion.startsWith("Deshiciste el pago de S/ 30.00.") })
    }
}
