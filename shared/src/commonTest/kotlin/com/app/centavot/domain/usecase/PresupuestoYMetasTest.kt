package com.app.centavot.domain.usecase

import com.app.centavot.core.util.Reloj
import com.app.centavot.domain.model.Categoria
import com.app.centavot.domain.model.EstadoGasto
import com.app.centavot.domain.model.EstadoLinea
import com.app.centavot.domain.model.Gasto
import com.app.centavot.domain.model.Ingreso
import com.app.centavot.domain.model.ItemPlantilla
import com.app.centavot.domain.model.Meta
import com.app.centavot.domain.model.Modo
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.MontoPresupuesto
import com.app.centavot.domain.model.OrigenGasto
import com.app.centavot.domain.model.Partida
import com.app.centavot.domain.model.PresupuestoMes
import com.app.centavot.domain.model.ProgresoMeta
import com.app.centavot.domain.model.SubcategoriaGasto
import com.app.centavot.domain.model.TipoPartida
import com.app.centavot.domain.model.montoPara
import com.app.centavot.domain.model.plantillaPresupuesto
import com.app.centavot.fakes.FakeActividadRepository
import com.app.centavot.fakes.FakeGastoRepository
import com.app.centavot.fakes.FakeIngresoRepository
import com.app.centavot.fakes.FakeMetaRepository
import com.app.centavot.fakes.FakeNotificacionRepository
import com.app.centavot.fakes.FakePresupuestoRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Lo pedido en la reunión con usuarios del 10 de octubre: presupuesto con plantilla, fuentes de ingreso y metas. */
class PresupuestoYMetasTest {

    private val hoy = LocalDate(2026, 10, 13)
    private val octubre = YearMonth(2026, 10)
    private val reloj = Reloj { hoy }
    private var siguiente = 0
    private val generarId = { "id${siguiente++}" }

    private val comida = Partida("comida", "Alimentación", TipoPartida.GASTO, Categoria.PERSONAL, SubcategoriaGasto.ALIMENTACION, 0)
    private val colegio = Partida("colegio", "Pensión del colegio", TipoPartida.GASTO, Categoria.PERSONAL, SubcategoriaGasto.EDUCACION, 1)
    private val clases = Partida("clases", "Clases de matemáticas", TipoPartida.INGRESO, Categoria.PERSONAL, null, 2)

    private fun gasto(id: String, soles: Long, sub: SubcategoriaGasto?, partidaId: String? = null) = Gasto(
        id, Monto.soles(soles), hoy, OrigenGasto.TEXTO, EstadoGasto.CONFIRMADO, Categoria.PERSONAL,
        subcategoria = sub, partidaId = partidaId,
    )

    @Test
    fun elMontoDeUnMesValeParaLosSiguientesHastaQueSeCambie() {
        val montos = listOf(
            MontoPresupuesto("colegio", YearMonth(2026, 3), Monto.soles(400)),
            MontoPresupuesto("colegio", YearMonth(2027, 1), Monto.CERO),
        )

        assertEquals(Monto.CERO, montos.montoPara("colegio", YearMonth(2026, 2)))
        assertEquals(Monto.soles(400), montos.montoPara("colegio", octubre))
        // En enero ya no hay pensión: el presupuesto de enero es distinto del de octubre.
        assertEquals(Monto.CERO, montos.montoPara("colegio", YearMonth(2027, 1)))
    }

    @Test
    fun cadaGastoVaASuPartidaOALaDeSuSubcategoria() {
        val mes = PresupuestoMes.de(
            mes = octubre,
            partidas = listOf(comida, colegio, clases),
            montos = listOf(MontoPresupuesto("comida", octubre, Monto.soles(500)), MontoPresupuesto("colegio", octubre, Monto.soles(400))),
            gastos = listOf(
                gasto("1", 300, SubcategoriaGasto.ALIMENTACION, partidaId = "comida"),
                // Anotado antes de armar el presupuesto: cae por su subcategoría.
                gasto("2", 160, SubcategoriaGasto.ALIMENTACION),
                gasto("3", 400, SubcategoriaGasto.EDUCACION),
                gasto("4", 50, SubcategoriaGasto.SALUD),
            ),
            ingresos = listOf(
                Ingreso("a", Monto.soles(100), hoy, Categoria.PERSONAL, partidaId = "clases"),
                Ingreso("b", Monto.soles(30), hoy, Categoria.PERSONAL),
                Ingreso("c", Monto.soles(200), hoy, Categoria.PERSONAL, retiroDelNegocio = true),
            ),
        )

        assertEquals(listOf(Monto.soles(460), Monto.soles(400)), mes.gastos.map { it.real })
        assertEquals(listOf(EstadoLinea.CERCA, EstadoLinea.CERCA), mes.gastos.map { it.estado })
        assertEquals(Monto.soles(50), mes.gastoSinPartida)
        assertEquals(Monto.soles(100), mes.ingresos.single().real)
        assertEquals(Monto.soles(30), mes.ingresoSinFuente)
        assertEquals(Monto.soles(200), mes.deTuNegocio)
    }

    @Test
    fun laLecturaDiceDondeTePasaste() {
        val mes = PresupuestoMes.de(
            mes = octubre,
            partidas = listOf(comida, colegio),
            montos = listOf(MontoPresupuesto("comida", octubre, Monto.soles(500)), MontoPresupuesto("colegio", octubre, Monto.soles(400))),
            gastos = listOf(gasto("1", 700, SubcategoriaGasto.ALIMENTACION, "comida"), gasto("2", 100, SubcategoriaGasto.EDUCACION, "colegio")),
            ingresos = emptyList(),
        )

        assertEquals(EstadoLinea.PASADO, mes.gastos.first().estado)
        assertEquals("Vas dentro del presupuesto, pero en Alimentación ya te pasaste.", mes.lectura)
    }

    @Test
    fun armarDesdeLaPlantillaCreaLasPartidasMarcadasConSuMonto() = runTest {
        val repositorio = FakePresupuestoRepository()
        val crear = CrearPresupuestoUseCase(repositorio, FakeActividadRepository(), reloj, generarId)
        val plantilla = plantillaPresupuesto(Modo.PERSONAL)

        crear(
            Modo.PERSONAL,
            listOf(
                CrearPresupuestoUseCase.Eleccion(plantilla.first { it.nombre == "Alimentación" }, Monto.soles(600)),
                CrearPresupuestoUseCase.Eleccion(plantilla.first { it.nombre == "Sueldo" }, Monto.CERO),
                CrearPresupuestoUseCase.Eleccion(ItemPlantilla("alimentación", TipoPartida.GASTO, null, true), Monto.soles(1)),
                CrearPresupuestoUseCase.Eleccion(ItemPlantilla("Clases de inglés", TipoPartida.INGRESO, null, true), Monto.soles(300)),
            ),
        )

        val partidas = repositorio.partidas.value
        assertEquals(listOf("Alimentación", "Sueldo", "Clases de inglés"), partidas.map { it.nombre })
        assertEquals(SubcategoriaGasto.ALIMENTACION, partidas.first().subcategoria)
        assertTrue(partidas.all { it.categoria == Categoria.PERSONAL })
        assertEquals(listOf(Monto.soles(600), Monto.soles(300)), repositorio.montos.value.map { it.monto })
    }

    @Test
    fun noSeRepiteUnaPartidaYAlQuitarlaSeVanSusMontos() = runTest {
        val repositorio = FakePresupuestoRepository()
        val agregar = AgregarPartidaUseCase(repositorio, reloj, generarId)

        val primera = agregar(Modo.NEGOCIO, " Bolsas ", TipoPartida.GASTO, Monto.soles(40), octubre)
        assertIs<AgregarPartidaUseCase.Resultado.Agregada>(primera)
        assertEquals(AgregarPartidaUseCase.Resultado.Repetida, agregar(Modo.NEGOCIO, "bolsas", TipoPartida.GASTO, Monto.CERO, octubre))
        assertEquals(AgregarPartidaUseCase.Resultado.NombreVacio, agregar(Modo.NEGOCIO, "  ", TipoPartida.GASTO, Monto.CERO, octubre))

        EliminarPartidaUseCase(repositorio, FakeActividadRepository(), reloj)(primera.partida)
        assertEquals(emptyList(), repositorio.partidas.value)
        assertEquals(emptyList(), repositorio.montos.value)
    }

    @Test
    fun elPresupuestoDelMesSoloMiraElModo() = runTest {
        val presupuesto = FakePresupuestoRepository().apply {
            partidas.value = listOf(comida, comida.copy(id = "neg", nombre = "Mercadería", categoria = Categoria.NEGOCIO))
        }
        val gastos = FakeGastoRepository(listOf(gasto("1", 80, SubcategoriaGasto.ALIMENTACION, "comida")))
        val mes = ObservarPresupuestoMesUseCase(presupuesto, gastos, FakeIngresoRepository())(octubre, Modo.PERSONAL).first()

        assertEquals(listOf("Alimentación"), mes.gastos.map { it.partida.nombre })
        assertEquals(Monto.soles(80), mes.realGastos)
    }

    private val viaje = Meta("viaje", "Viaje a Cusco", Monto.soles(1_200), Categoria.PERSONAL, hoy, fechaLimite = LocalDate(2026, 12, 15))

    @Test
    fun elProgresoDiceCuantoSepararPorSemana() {
        val progreso = ProgresoMeta(viaje, Monto.soles(300))

        assertEquals(25, progreso.porcentaje)
        assertEquals(Monto.soles(900), progreso.falta)
        // 63 días = 9 semanas: S/ 900 / 9 = S/ 100.
        assertEquals(Monto.soles(100), progreso.porSemana(hoy))
        assertNull(ProgresoMeta(viaje.copy(fechaLimite = null), Monto.CERO).porSemana(hoy))
        assertNull(ProgresoMeta(viaje, Monto.soles(1_500)).porSemana(hoy))
        assertEquals(100, ProgresoMeta(viaje, Monto.soles(1_500)).porcentaje)
    }

    @Test
    fun abonarAvisaAlCruzarCadaHitoUnaSolaVez() = runTest {
        val metas = FakeMetaRepository().apply { metas.value = listOf(viaje) }
        val notificaciones = FakeNotificacionRepository()
        val abonar = AbonarMetaUseCase(metas, notificaciones, FakeActividadRepository(), reloj, generarId)

        val primero = abonar("viaje", Monto.soles(200))
        assertIs<AbonarMetaUseCase.Resultado.Abonado>(primero)
        assertNull(primero.hito)

        val segundo = abonar("viaje", Monto.soles(450))
        assertIs<AbonarMetaUseCase.Resultado.Abonado>(segundo)
        assertEquals(50, segundo.hito)
        assertEquals(54, segundo.progreso.porcentaje)

        abonar("viaje", Monto.soles(600))
        assertEquals(listOf("meta-viaje-50", "meta-viaje-100"), notificaciones.notificaciones.value.map { it.id })
        assertEquals("¡Cumpliste tu meta \"Viaje a Cusco\"!", notificaciones.notificaciones.value.last().asunto)
        assertEquals(AbonarMetaUseCase.Resultado.MontoInvalido, abonar("viaje", Monto.CERO))
    }

    @Test
    fun unaMetaNecesitaNombreMontoYFechaFutura() = runTest {
        val metas = FakeMetaRepository()
        val guardar = GuardarMetaUseCase(metas, FakeActividadRepository(), reloj, generarId)

        assertEquals(GuardarMetaUseCase.Resultado.NombreVacio, guardar(null, Modo.PERSONAL, " ", Monto.soles(10), null))
        assertEquals(GuardarMetaUseCase.Resultado.MontoInvalido, guardar(null, Modo.PERSONAL, "Moto", Monto.CERO, null))
        assertEquals(GuardarMetaUseCase.Resultado.FechaPasada, guardar(null, Modo.PERSONAL, "Moto", Monto.soles(10), hoy))
        assertIs<GuardarMetaUseCase.Resultado.Guardada>(guardar(null, Modo.NEGOCIO, "Moto para el delivery", Monto.soles(3_000), null))

        val enNegocio = ObservarMetasUseCase(metas)(Modo.NEGOCIO).first()
        assertEquals(listOf("Moto para el delivery"), enNegocio.map { it.meta.nombre })
        assertEquals(emptyList(), ObservarMetasUseCase(metas)(Modo.PERSONAL).first())
    }
}
