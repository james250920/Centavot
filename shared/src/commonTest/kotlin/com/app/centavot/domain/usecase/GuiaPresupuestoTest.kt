package com.app.centavot.domain.usecase

import com.app.centavot.core.util.Reloj
import com.app.centavot.domain.model.AbonoMeta
import com.app.centavot.domain.model.Categoria
import com.app.centavot.domain.model.EstadoGasto
import com.app.centavot.domain.model.EstadoLinea
import com.app.centavot.domain.model.Gasto
import com.app.centavot.domain.model.Ingreso
import com.app.centavot.domain.model.Meta
import com.app.centavot.domain.model.Modo
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.MontoPresupuesto
import com.app.centavot.domain.model.OrigenGasto
import com.app.centavot.domain.model.Partida
import com.app.centavot.domain.model.Perfil
import com.app.centavot.domain.model.PresupuestoMes
import com.app.centavot.domain.model.Reparto503020
import com.app.centavot.domain.model.SubcategoriaGasto
import com.app.centavot.domain.model.TipoGasto
import com.app.centavot.domain.model.TipoPartida
import com.app.centavot.domain.model.fondoSugerido
import com.app.centavot.domain.model.plantillaPresupuesto
import com.app.centavot.domain.model.tipoGastoPorDefecto
import com.app.centavot.domain.repository.PerfilRepository
import com.app.centavot.fakes.FakeActividadRepository
import com.app.centavot.fakes.FakeGastoRepository
import com.app.centavot.fakes.FakeIngresoRepository
import com.app.centavot.fakes.FakeMetaRepository
import com.app.centavot.fakes.FakePresupuestoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Los requerimientos de presupuesto RP-01 a RP-10 (documentos 03 §3 y 04), con el ejemplo personal del
 * documento 04 §10: S/ 3,000 de ingreso repartidos 50/30/20.
 */
class GuiaPresupuestoTest {

    private val hoy = LocalDate(2026, 10, 15) // jueves
    private val octubre = YearMonth(2026, 10)
    private val reloj = Reloj { hoy }

    private fun partida(id: String, tipo: TipoGasto?, sub: SubcategoriaGasto? = null, orden: Int = 0, semanal: Boolean = false, deuda: Boolean = false) =
        Partida(id, id, if (tipo == null) TipoPartida.INGRESO else TipoPartida.GASTO, Categoria.PERSONAL, sub, orden, tipo, semanal, deuda)

    private fun monto(id: String, soles: Long) = MontoPresupuesto(id, octubre, Monto.soles(soles))

    private fun gasto(id: String, soles: Long, partidaId: String?, dia: Int = 10, sub: SubcategoriaGasto? = null) = Gasto(
        id, Monto.soles(soles), LocalDate(2026, 10, dia), OrigenGasto.TEXTO, EstadoGasto.CONFIRMADO, Categoria.PERSONAL,
        subcategoria = sub, partidaId = partidaId,
    )

    /** El presupuesto del ejemplo: S/ 3,000 de ingreso; S/ 600 de ahorro; S/ 1,500 de necesidades; S/ 900 prescindibles. */
    private val partidas = listOf(
        partida("sueldo", null, orden = 0),
        partida("Fondo de emergencia", TipoGasto.AHORRO, orden = 1),
        partida("Viaje", TipoGasto.AHORRO, orden = 2),
        partida("alquiler", TipoGasto.FIJO, SubcategoriaGasto.CASA, orden = 3),
        partida("prestamo", TipoGasto.FIJO, SubcategoriaGasto.OTROS, orden = 4, deuda = true),
        partida("comida", TipoGasto.VARIABLE, SubcategoriaGasto.ALIMENTACION, orden = 5),
        partida("salidas", TipoGasto.PRESCINDIBLE, SubcategoriaGasto.OTROS, orden = 6),
        partida("menores", TipoGasto.PRESCINDIBLE, SubcategoriaGasto.OTROS, orden = 7, semanal = true),
    )
    private val montos = listOf(
        monto("sueldo", 3_000), monto("Fondo de emergencia", 400), monto("Viaje", 200),
        monto("alquiler", 820), monto("prestamo", 180), monto("comida", 500),
        monto("salidas", 740), monto("menores", 40),
    )

    /** Como el repositorio: un monto nuevo del mismo mes reemplaza al anterior. */
    private fun cambiar(id: String, soles: Long) = montos.filterNot { it.partidaId == id } + monto(id, soles)

    @Test
    fun rp01CadaPartidaTieneSuTipoYSinElegirSeDeduce() {
        assertEquals(TipoGasto.FIJO, tipoGastoPorDefecto(SubcategoriaGasto.ALQUILER))
        assertEquals(TipoGasto.VARIABLE, tipoGastoPorDefecto(SubcategoriaGasto.ALIMENTACION))
        assertEquals(TipoGasto.PRESCINDIBLE, tipoGastoPorDefecto(null))
        assertEquals(TipoGasto.FIJO, Partida("a", "Alquiler", TipoPartida.GASTO, Categoria.PERSONAL, SubcategoriaGasto.CASA).clase)
    }

    @Test
    fun rp02YRp03LosGastosVanPorGrupoYElBalanceDejaCadaSolConDestino() {
        val mes = PresupuestoMes.de(octubre, partidas, montos, gastos = emptyList(), ingresos = emptyList())

        assertEquals(listOf(TipoGasto.FIJO, TipoGasto.VARIABLE, TipoGasto.PRESCINDIBLE), mes.grupos.map { it.first })
        assertEquals(listOf("alquiler", "prestamo"), mes.grupos.first().second.map { it.partida.id })
        // 3,000 − 600 de ahorro − (820 + 180 + 500 + 740 + 40 × 4) = 0
        assertEquals(Monto.CERO, mes.balance)
    }

    @Test
    fun rp03SinIngresosPresupuestadosNoHayBalanceYSiFaltaEsNegativo() {
        val sinIngreso = PresupuestoMes.de(octubre, partidas.drop(1), montos, emptyList(), emptyList())
        assertNull(sinIngreso.balance)

        val apretado = PresupuestoMes.de(octubre, partidas, cambiar("salidas", 1_000), emptyList(), emptyList())
        assertEquals(Monto.soles(-260), apretado.balance)
    }

    @Test
    fun rp04LoSeparadoParaMetasVaASuPartidaDeAhorro() {
        val mes = PresupuestoMes.de(
            octubre, partidas, montos, emptyList(), emptyList(),
            separado = listOf("Viaje" to Monto.soles(150), "Moto" to Monto.soles(50), "fondo de emergencia" to Monto.soles(100)),
        )

        // "Viaje" a su partida; "Moto" no tiene partida propia y va a la primera de ahorro.
        assertEquals(listOf(Monto.soles(150), Monto.soles(150)), mes.ahorro.map { it.real }.reversed())
        assertEquals(Monto.soles(300), mes.realAhorro)
        // El ahorro no cuenta como gasto.
        assertEquals(Monto.CERO, mes.realGastos)
        // Y la plantilla lo pone primero, marcado.
        assertTrue(plantillaPresupuesto(Modo.PERSONAL).first { it.tipoGasto == TipoGasto.AHORRO }.marcado)
    }

    @Test
    fun rp05ElRepartoSeComparaCon503020() {
        val mes = PresupuestoMes.de(octubre, partidas, montos, emptyList(), emptyList())

        // Necesidades: 820 + 180 + 500 = 1,500 (50 %); estilo de vida: 740 + 160 = 900 (30 %); ahorro: 600 (20 %).
        assertEquals(Reparto503020(necesidades = 50, estiloDeVida = 30, ahorro = 20), mes.reparto)
    }

    @Test
    fun rp07ElRealDelMesQuedaComoPresupuestoDelSiguiente() = runTest {
        val mes = PresupuestoMes.de(
            octubre, partidas, montos,
            gastos = listOf(gasto("1", 700, "comida"), gasto("2", 120, "menores"), gasto("3", 80, "menores")),
            ingresos = emptyList(),
        )
        val repositorio = FakePresupuestoRepository().apply { partidas.value = this@GuiaPresupuestoTest.partidas }

        val cambiadas = UsarRealComoPresupuestoUseCase(repositorio, FakeActividadRepository(), reloj)(mes)

        assertEquals(2, cambiadas)
        val noviembre = YearMonth(2026, 11)
        // Comida: lo realista es lo que se gasta (ejemplo de Jemina). Gastos menores: S/ 200 al mes = S/ 50 por semana.
        assertEquals(
            setOf(MontoPresupuesto("comida", noviembre, Monto.soles(700)), MontoPresupuesto("menores", noviembre, Monto.soles(50))),
            repositorio.montos.value.toSet(),
        )
    }

    @Test
    fun rp08LosGastosMenoresSeMidenPorSemanaDesdeElLunes() {
        val mes = PresupuestoMes.de(
            octubre, partidas, montos,
            // El jueves 15: lunes 12 S/ 25 y miércoles 14 S/ 20 son de esta semana; el viernes 9, no.
            gastos = listOf(gasto("1", 30, "menores", dia = 9), gasto("2", 25, "menores", dia = 12), gasto("3", 20, "menores", dia = 14)),
            ingresos = emptyList(),
            hoy = hoy,
        )
        val menores = mes.gastos.single { it.partida.id == "menores" }

        assertEquals(Monto.soles(160), menores.presupuesto)
        assertEquals(Monto.soles(75), menores.real)
        assertEquals(Monto.soles(45), menores.realSemana)
        assertEquals(EstadoLinea.PASADO, menores.estado)
        assertEquals(Monto.soles(-5), menores.diferencia)
    }

    @Test
    fun rp09ElFondoDeEmergenciaSaleDeLosGastosFijos() {
        val mes = PresupuestoMes.de(octubre, partidas, montos, emptyList(), emptyList())

        // Fijos: 820 + 180 = 1,000.
        assertEquals(Monto.soles(3_000), mes.fondoSugerido(Modo.PERSONAL, meses = 3))
        assertEquals(Monto.soles(6_000), mes.fondoSugerido(Modo.PERSONAL, meses = 6))
        // Colchón del negocio: 2 semanas de gastos fijos.
        assertEquals(Monto.soles(500), mes.fondoSugerido(Modo.NEGOCIO))
    }

    @Test
    fun rp10AvisaSiLasCuotasPasanEl30PorCiento() {
        val sano = PresupuestoMes.de(octubre, partidas, montos, emptyList(), emptyList())
        assertEquals(6, sano.porcentajeDeudas)
        assertFalse(sano.deudasAltas)

        val cargado = PresupuestoMes.de(octubre, partidas, cambiar("prestamo", 1_000), emptyList(), emptyList())
        assertEquals(33, cargado.porcentajeDeudas)
        assertTrue(cargado.deudasAltas)
    }

    @Test
    fun enElNegocioLasVentasVanALaFuenteVentas() = runTest {
        val ventas = Partida("ventas", "Ventas", TipoPartida.INGRESO, Categoria.NEGOCIO)
        val presupuesto = FakePresupuestoRepository().apply {
            partidas.value = listOf(ventas)
            montos.value = listOf(MontoPresupuesto("ventas", octubre, Monto.soles(7_000)))
        }
        val ingresos = FakeIngresoRepository(
            listOf(
                Ingreso("v1", Monto.soles(300), hoy),
                Ingreso("v2", Monto.soles(200), hoy),
                Ingreso("p", Monto.soles(999), hoy, Categoria.PERSONAL),
            ),
        )

        val mes = ObservarPresupuestoMesUseCase(presupuesto, FakeGastoRepository(), ingresos, FakeMetaRepository(), reloj)(octubre, Modo.NEGOCIO).first()

        assertEquals(Monto.soles(500), mes.ingresos.single().real)
        assertEquals(Monto.CERO, mes.ingresoSinFuente)
        assertEquals("Ventas", plantillaPresupuesto(Modo.NEGOCIO).first().nombre)
    }

    @Test
    fun k6YK7MidenElUsoDelPresupuestoYDeLasMetas() = runTest {
        val presupuesto = FakePresupuestoRepository().apply { partidas.value = this@GuiaPresupuestoTest.partidas }
        val gastos = FakeGastoRepository(listOf(gasto("1", 75, "comida"), gasto("2", 25, null, sub = SubcategoriaGasto.SALUD)))
        val metas = FakeMetaRepository().apply {
            this.metas.value = listOf(
                Meta("m1", "Viaje", Monto.soles(1_000), Categoria.PERSONAL, hoy),
                Meta("m2", "Moto", Monto.soles(3_000), Categoria.PERSONAL, hoy),
            )
            abonos.value = listOf(AbonoMeta("a1", "m1", Monto.soles(50), hoy), AbonoMeta("a2", "m2", Monto.soles(50), LocalDate(2026, 9, 30)))
        }
        val observar = ObservarIndicadoresPlanesUseCase(
            observarModo = ObservarModoUseCase(
                object : PerfilRepository {
                    override fun observarPerfil(): Flow<Perfil?> = flowOf(Perfil("Jemina", null, modo = Modo.PERSONAL))
                    override suspend fun guardar(perfil: Perfil) = Unit
                },
            ),
            observarPresupuestoMes = ObservarPresupuestoMesUseCase(presupuesto, gastos, FakeIngresoRepository(), metas, reloj),
            metas = metas,
            reloj = reloj,
        )

        val indicadores = observar().first()

        assertEquals(75, indicadores.gastosConPartida)
        assertEquals(1, indicadores.metasConAbono)
        assertEquals(2, indicadores.metas)
    }
}
