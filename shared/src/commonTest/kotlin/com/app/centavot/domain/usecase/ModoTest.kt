package com.app.centavot.domain.usecase

import com.app.centavot.core.util.Reloj
import com.app.centavot.domain.model.Categoria
import com.app.centavot.domain.model.Cobro
import com.app.centavot.domain.model.Contacto
import com.app.centavot.domain.model.EstadoGasto
import com.app.centavot.domain.model.Gasto
import com.app.centavot.domain.model.Ingreso
import com.app.centavot.domain.model.Modo
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.OrigenGasto
import com.app.centavot.domain.model.Perfil
import com.app.centavot.domain.model.PeriodoTope
import com.app.centavot.domain.model.RegimenTributario
import com.app.centavot.domain.model.ResumenPeriodo
import com.app.centavot.domain.model.TipoCobro
import com.app.centavot.domain.model.TipoEntrada
import com.app.centavot.domain.model.TipoRegimen
import com.app.centavot.domain.model.VentaFrecuente
import com.app.centavot.domain.model.delModo
import com.app.centavot.domain.model.tipo
import com.app.centavot.domain.model.ventasFrecuentes
import com.app.centavot.domain.repository.PerfilRepository
import com.app.centavot.fakes.FakeActividadRepository
import com.app.centavot.fakes.FakeCobroRepository
import com.app.centavot.fakes.FakeGastoRepository
import com.app.centavot.fakes.FakeIngresoRepository
import com.app.centavot.fakes.FakeNotificacionRepository
import com.app.centavot.fakes.FakeRegimenRepository
import com.app.centavot.fakes.FakeUsoRepository
import com.app.centavot.fakes.revisarAlertaTope
import com.app.centavot.presentation.EstadoApp
import com.app.centavot.presentation.estadoApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Modo personal o negocio: cada uno ve solo lo suyo y cambiar no borra nada. */
class ModoTest {

    private val hoy = LocalDate(2026, 10, 10)
    private val reloj = Reloj { hoy }
    private val regimen = RegimenTributario(TipoRegimen.RUS, Monto.soles(5_000), PeriodoTope.MENSUAL)

    private val venta = Ingreso("v", Monto.soles(40), hoy, Categoria.NEGOCIO, "Gaseosa")
    private val sueldo = Ingreso("s", Monto.soles(1_000), hoy, Categoria.PERSONAL, "Sueldo")
    private val retiro = Ingreso("r", Monto.soles(30), hoy, Categoria.PERSONAL, "Mercado", retiroDelNegocio = true)
    private val compra = Gasto("g1", Monto.soles(15), hoy, OrigenGasto.TEXTO, EstadoGasto.CONFIRMADO, Categoria.NEGOCIO)
    private val pasaje = Gasto("g2", Monto.soles(5), hoy, OrigenGasto.TEXTO, EstadoGasto.CONFIRMADO, Categoria.PERSONAL)
    private val sinClasificar = Gasto("g3", Monto.soles(2), hoy, OrigenGasto.FOTO, EstadoGasto.entries.first())

    @Test
    fun quienEligePersonalEntraSinRegimen() {
        val personal = Perfil("Ana", null, modo = Modo.PERSONAL)
        val negocio = Perfil("Rosa", null, modo = Modo.NEGOCIO)

        assertEquals(EstadoApp.LISTA, estadoApp(consentido = true, perfil = personal, regimen = null))
        assertEquals(EstadoApp.SIN_REGIMEN, estadoApp(consentido = true, perfil = negocio, regimen = null))
        assertEquals(EstadoApp.LISTA, estadoApp(consentido = true, perfil = negocio, regimen = regimen))
    }

    @Test
    fun cadaModoVeSoloLoSuyoYElRetiroEnLosDos() {
        val ingresos = listOf(venta, sueldo, retiro)
        val gastos = listOf(compra, pasaje, sinClasificar)

        assertEquals(listOf(venta, retiro), ingresos.delModo(Modo.NEGOCIO))
        assertEquals(listOf(sueldo, retiro), ingresos.delModo(Modo.PERSONAL))
        assertEquals(listOf(compra, sinClasificar), gastos.delModo(Modo.NEGOCIO))
        assertEquals(listOf(pasaje, sinClasificar), gastos.delModo(Modo.PERSONAL))
    }

    @Test
    fun losCobrosSeSeparanPorModo() {
        val rosa = Contacto("rosa", "Rosa")
        val fiado = Cobro("c1", rosa, "Arroz", Monto.soles(10), hoy)
        val prestamo = Cobro("c2", rosa, "Préstamo", Monto.soles(50), hoy, categoria = Categoria.PERSONAL)

        assertEquals(listOf(fiado), listOf(fiado, prestamo).delModo(Modo.NEGOCIO))
        assertEquals(listOf(prestamo), listOf(fiado, prestamo).delModo(Modo.PERSONAL))
    }

    @Test
    fun elRetiroRestaDeLaCajaPeroNoDeLaGanancia() {
        val caja = ResumenPeriodo.de(listOf(venta, sueldo, retiro), listOf(compra, pasaje))

        assertEquals(Monto.soles(25), caja.ganancia)
        assertEquals(Monto.soles(30), caja.retiros)
        assertEquals(Monto.soles(-5), caja.quedaEnCaja)
        // En lo personal, lo que vino del negocio también entró.
        assertEquals(Monto.soles(1_030), caja.ingresosPersonales)
        assertEquals(Monto.soles(1_025), caja.saldoPersonal)
    }

    @Test
    fun losFrecuentesSonDelModoYNoIncluyenRetiros() {
        val ingresos = listOf(venta, sueldo, retiro)

        assertEquals(listOf(VentaFrecuente("Gaseosa", Monto.soles(40))), ingresos.ventasFrecuentes(Categoria.NEGOCIO))
        assertEquals(listOf(VentaFrecuente("Sueldo", Monto.soles(1_000))), ingresos.ventasFrecuentes(Categoria.PERSONAL))
    }

    @Test
    fun unRetiroSiempreSeGuardaComoPersonal() = runTest {
        val ingresos = FakeIngresoRepository()
        val actividades = FakeActividadRepository()
        val guardar = GuardarIngresoUseCase(
            ingresos, reloj, { "r1" }, actividades, FakeUsoRepository(),
            revisarAlertaTope(FakeGastoRepository(), FakeRegimenRepository(regimen), FakeNotificacionRepository(), reloj, ingresos),
        )

        val resultado = guardar(null, Monto.soles(20), Categoria.NEGOCIO, hoy, null, retiroDelNegocio = true)

        val guardado = assertIs<GuardarIngresoUseCase.Resultado.Guardado>(resultado).ingreso
        assertEquals(Categoria.PERSONAL, guardado.categoria)
        assertEquals(TipoEntrada.RETIRO, guardado.tipo)
        assertEquals("Registraste un retiro para la casa de S/ 20.00.", actividades.actividades.value.single().descripcion)
    }

    @Test
    fun unPrestamoPersonalNoSeSumaALasVentas() = runTest {
        val cobros = FakeCobroRepository()
        val ingresos = FakeIngresoRepository()
        val actividades = FakeActividadRepository()
        var siguiente = 0
        val generarId = { "id-${++siguiente}" }
        val guardarIngreso = GuardarIngresoUseCase(
            ingresos, reloj, generarId, actividades, FakeUsoRepository(),
            revisarAlertaTope(FakeGastoRepository(), FakeRegimenRepository(regimen), FakeNotificacionRepository(), reloj, ingresos),
        )
        val registrar = RegistrarCobroYVentaUseCase(RegistrarCobroUseCase(cobros, actividades, reloj, generarId), guardarIngreso)

        val registro = registrar(
            Contacto("luis", "Luis"), "Pasaje", Monto.soles(50), hoy, TipoCobro.FIADO, Monto.CERO, null,
            contarComoVenta = true, categoria = Categoria.PERSONAL,
        )

        assertIs<RegistrarCobroUseCase.Resultado.Registrado>(registro.resultado)
        assertEquals(Categoria.PERSONAL, cobros.cobros.value.single().categoria)
        assertTrue(ingresos.ingresos.value.isEmpty())
        assertEquals("Registraste un préstamo a Luis por S/ 50.00 (\"Pasaje\").", actividades.actividades.value.single().descripcion)
    }

    @Test
    fun cambiarDeModoNoBorraNadaYQuedaAnotado() = runTest {
        val perfiles = object : PerfilRepository {
            val perfil = MutableStateFlow<Perfil?>(Perfil("Rosa", null, Monto.soles(1_500)))
            override fun observarPerfil(): Flow<Perfil?> = perfil
            override suspend fun guardar(perfil: Perfil) {
                this.perfil.value = perfil
            }
        }
        val actividades = FakeActividadRepository()

        CambiarModoUseCase(perfiles, actividades, reloj)(Modo.PERSONAL)

        val perfil = perfiles.perfil.value!!
        assertEquals(Modo.PERSONAL, perfil.modo)
        assertEquals(Monto.soles(1_500), perfil.ingresoMensual)
        assertEquals(Modo.PERSONAL, ObservarModoUseCase(perfiles)().first())
        assertEquals("Cambiaste al modo \"Mi plata personal\".", actividades.actividades.value.single().descripcion)

        // Volver al mismo modo no anota nada nuevo.
        CambiarModoUseCase(perfiles, actividades, reloj)(Modo.PERSONAL)
        assertEquals(1, actividades.actividades.value.size)
    }
}
