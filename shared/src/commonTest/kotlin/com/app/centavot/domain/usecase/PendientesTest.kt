package com.app.centavot.domain.usecase

import com.app.centavot.core.util.Reloj
import com.app.centavot.domain.model.Categoria
import com.app.centavot.domain.model.Contacto
import com.app.centavot.domain.model.EventoUso
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.PeriodoTope
import com.app.centavot.domain.model.RegimenTributario
import com.app.centavot.domain.model.SubcategoriaGasto
import com.app.centavot.domain.model.TipoCobro
import com.app.centavot.domain.model.TipoEventoUso
import com.app.centavot.domain.model.TipoRegimen
import com.app.centavot.domain.model.para
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
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** D1, V5 y D13. */
class PendientesTest {

    private val hoy = LocalDate(2026, 10, 6)
    private val rosa = Contacto("rosa", "Rosa")

    // ---------- D1: el fiado o pedido cuenta como venta al registrarlo ----------

    private val cobros = FakeCobroRepository()
    private val ingresos = FakeIngresoRepository()
    private val actividades = FakeActividadRepository()
    private val notificaciones = FakeNotificacionRepository()
    private var siguiente = 0
    private val generarId = { "id-${++siguiente}" }
    private val reloj = Reloj { hoy }
    private val regimenes = FakeRegimenRepository(RegimenTributario(TipoRegimen.RUS, Monto.soles(5_000), PeriodoTope.MENSUAL))
    private val registrarCobro = RegistrarCobroUseCase(cobros, actividades, reloj, generarId)
    private val guardarIngreso = GuardarIngresoUseCase(
        ingresos, reloj, generarId, actividades, FakeUsoRepository(),
        revisarAlertaTope(FakeGastoRepository(), regimenes, notificaciones, reloj, ingresos),
    )
    private val registrar = RegistrarCobroYVentaUseCase(registrarCobro, guardarIngreso)
    private val marcarCobrado = MarcarCobradoUseCase(cobros, actividades, reloj)
    private val abonar = RegistrarAbonoUseCase(cobros, actividades, reloj)

    @Test
    fun alRegistrarUnFiadoSeCuentaLaVentaUnaSolaVez() = runTest {
        val cobro = assertIs<RegistrarCobroUseCase.Resultado.Registrado>(
            registrar(rosa, "Arroz", Monto.soles(30), hoy, TipoCobro.FIADO, Monto.CERO, null, contarComoVenta = true),
        ).cobro

        val venta = ingresos.ingresos.value.single()
        assertEquals(Monto.soles(30), venta.monto)
        assertEquals("Arroz", venta.descripcion)
        assertEquals(Categoria.NEGOCIO, venta.categoria)

        // Cobrar o abonar después no suma más ventas.
        abonar(cobro.id, Monto.soles(10))
        marcarCobrado(cobro.id)
        assertEquals(1, ingresos.ingresos.value.size)
    }

    @Test
    fun sinContarComoVentaNoSeRegistraIngreso() = runTest {
        registrar(rosa, "Préstamo", Monto.soles(50), hoy, TipoCobro.FIADO, Monto.CERO, null, contarComoVenta = false)

        assertTrue(ingresos.ingresos.value.isEmpty())
        assertEquals(1, cobros.cobros.value.size)
    }

    @Test
    fun editarUnCobroNoCreaOtraVenta() = runTest {
        val cobro = assertIs<RegistrarCobroUseCase.Resultado.Registrado>(
            registrar(rosa, "Pedido", Monto.soles(60), hoy, TipoCobro.PEDIDO, Monto.soles(20), null, contarComoVenta = true),
        ).cobro

        registrar(rosa, "Pedido", Monto.soles(80), hoy, TipoCobro.PEDIDO, Monto.soles(20), cobro.id, contarComoVenta = true)

        assertEquals(listOf(Monto.soles(60)), ingresos.ingresos.value.map { it.monto })
    }

    @Test
    fun unCobroInvalidoNoRegistraVenta() = runTest {
        registrar(rosa, " ", Monto.soles(30), hoy, TipoCobro.FIADO, Monto.CERO, null, contarComoVenta = true)

        assertTrue(ingresos.ingresos.value.isEmpty())
    }

    // ---------- V5: subcategorías según negocio o personal ----------

    @Test
    fun lasSubcategoriasDependenDeLaCategoria() {
        val negocio = SubcategoriaGasto.para(Categoria.NEGOCIO)
        val personal = SubcategoriaGasto.para(Categoria.PERSONAL)

        assertTrue(SubcategoriaGasto.PERSONAL_NEGOCIO in negocio)
        assertFalse(SubcategoriaGasto.PERSONAL_NEGOCIO in personal)
        assertTrue(SubcategoriaGasto.ALIMENTACION in personal)
        assertFalse(SubcategoriaGasto.ALIMENTACION in negocio)
        assertTrue(SubcategoriaGasto.TRANSPORTE in negocio && SubcategoriaGasto.TRANSPORTE in personal)
        assertEquals(SubcategoriaGasto.entries, SubcategoriaGasto.para(null))
    }

    @Test
    fun unGastoPersonalNoGuardaUnaSubcategoriaDeNegocio() = runTest {
        val gastos = FakeGastoRepository()
        val guardar = GuardarGastoUseCase(
            gastos, reloj, generarId, actividades, FakeUsoRepository(),
            revisarAlertaTope(gastos, regimenes, notificaciones, reloj),
        )

        val resultado = guardar(null, Monto.soles(20), Categoria.PERSONAL, hoy, null, SubcategoriaGasto.PERSONAL_NEGOCIO)

        assertNull(assertIs<GuardarGastoUseCase.Resultado.Guardado>(resultado).gasto.subcategoria)
    }

    // ---------- D13: un uso al volver a la app, sin contar rotaciones ----------

    @Test
    fun cuentaUnUsoAlVolverSiPasaronAlMenos5Minutos() = runTest {
        val uso = FakeUsoRepository()
        var ahora = LocalDateTime(2026, 10, 6, 9, 0)
        val registrarApertura = RegistrarAperturaUseCase(
            uso,
            object : Reloj {
                override fun hoy() = ahora.date
                override fun ahora() = ahora
            },
        )

        registrarApertura()
        ahora = LocalDateTime(2026, 10, 6, 9, 2) // rotación o vistazo rápido: no cuenta
        registrarApertura()
        ahora = LocalDateTime(2026, 10, 6, 9, 30) // volvió desde WhatsApp: cuenta
        registrarApertura()

        assertEquals(
            listOf(LocalDateTime(2026, 10, 6, 9, 0), LocalDateTime(2026, 10, 6, 9, 30)),
            uso.eventos.value.filter { it.tipo == TipoEventoUso.APERTURA }.map(EventoUso::fechaHora),
        )
    }
}
