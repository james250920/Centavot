package com.app.centavot.domain.usecase

import com.app.centavot.core.util.Reloj
import com.app.centavot.domain.model.ItemPlantilla
import com.app.centavot.domain.model.Modo
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.MontoPresupuesto
import com.app.centavot.domain.model.Partida
import com.app.centavot.domain.model.PresupuestoMes
import com.app.centavot.domain.model.SubcategoriaGasto
import com.app.centavot.domain.model.TipoGasto
import com.app.centavot.domain.model.TipoPartida
import com.app.centavot.domain.model.delModo
import com.app.centavot.domain.model.enSoles
import com.app.centavot.domain.repository.ActividadRepository
import com.app.centavot.domain.repository.GastoRepository
import com.app.centavot.domain.repository.IngresoRepository
import com.app.centavot.domain.repository.MetaRepository
import com.app.centavot.domain.repository.PresupuestoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.datetime.YearMonth
import kotlinx.datetime.plusMonth
import kotlinx.datetime.yearMonth

private const val MAX_NOMBRE_PARTIDA = 40

/** Las partidas de un modo, en orden. */
class ObservarPartidasUseCase(private val repositorio: PresupuestoRepository) {
    operator fun invoke(modo: Modo): Flow<List<Partida>> =
        repositorio.observarPartidas().map { lista -> lista.filter { it.categoria == modo.categoria } }
}

/**
 * El presupuesto de [mes] frente a lo que de verdad entró, se gastó y se separó para metas, solo del modo.
 * En el negocio, las ventas van a la fuente "Ventas"; en lo personal también cuenta lo que vino del negocio.
 */
class ObservarPresupuestoMesUseCase(
    private val presupuesto: PresupuestoRepository,
    private val gastos: GastoRepository,
    private val ingresos: IngresoRepository,
    private val metas: MetaRepository,
    private val reloj: Reloj,
) {
    operator fun invoke(mes: YearMonth, modo: Modo): Flow<PresupuestoMes> = combine(
        combine(presupuesto.observarPartidas(), presupuesto.observarMontos(), ::Pair),
        gastos.observarGastosEntre(mes.firstDay, mes.lastDay),
        ingresos.observarIngresosEntre(mes.firstDay, mes.lastDay),
        combine(metas.observarMetas(), metas.observarAbonos(), ::Pair),
        reloj.dias(),
    ) { (partidas, montos), listaGastos, listaIngresos, (listaMetas, abonos), hoy ->
        val metasDelModo = listaMetas.filter { it.categoria == modo.categoria }.associateBy { it.id }
        PresupuestoMes.de(
            mes = mes,
            partidas = partidas.filter { it.categoria == modo.categoria },
            montos = montos,
            gastos = listaGastos.delModo(modo),
            ingresos = listaIngresos.delModo(modo).filter { modo == Modo.PERSONAL || it.esDeNegocio },
            separado = abonos
                .filter { it.fecha.yearMonth == mes }
                .mapNotNull { abono -> metasDelModo[abono.metaId]?.let { it.nombre to abono.monto } },
            hoy = hoy,
            ventasALaPrimeraFuente = modo == Modo.NEGOCIO,
        )
    }
}

/**
 * Arma el presupuesto desde la plantilla: crea una partida por cada ítem marcado y le fija su monto desde
 * el mes actual. Los nombres repetidos (sin distinguir mayúsculas) se crean una sola vez.
 */
class CrearPresupuestoUseCase(
    private val repositorio: PresupuestoRepository,
    private val actividades: ActividadRepository,
    private val reloj: Reloj,
    private val generarId: () -> String,
) {
    data class Eleccion(val item: ItemPlantilla, val monto: Monto)

    suspend operator fun invoke(modo: Modo, elecciones: List<Eleccion>) {
        val existentes = repositorio.observarPartidas().first().filter { it.categoria == modo.categoria }
        val nombres = existentes.map { it.nombre.lowercase() }.toMutableSet()
        var orden = (existentes.maxOfOrNull { it.orden } ?: -1) + 1
        val mes = YearMonth(reloj.hoy().year, reloj.hoy().month)
        elecciones.forEach { (item, monto) ->
            val nombre = item.nombre.trim().take(MAX_NOMBRE_PARTIDA)
            if (nombre.isEmpty() || !nombres.add(nombre.lowercase())) return@forEach
            val partida = Partida(
                id = generarId(),
                nombre = nombre,
                tipo = item.tipo,
                categoria = modo.categoria,
                subcategoria = item.subcategoria,
                orden = orden++,
                tipoGasto = item.tipoGasto,
                semanal = item.semanal,
                deuda = item.deuda,
            )
            repositorio.guardarPartida(partida)
            if (monto > Monto.CERO) repositorio.fijarMonto(MontoPresupuesto(partida.id, mes, monto))
        }
        actividades.registrar("Armaste tu presupuesto de ${modo.etiqueta.lowercase()}.", reloj.ahora())
    }
}

/** Agrega una partida suelta ("Pensión de inglés") al presupuesto del modo. */
class AgregarPartidaUseCase(
    private val repositorio: PresupuestoRepository,
    private val reloj: Reloj,
    private val generarId: () -> String,
) {
    sealed interface Resultado {
        data class Agregada(val partida: Partida) : Resultado
        data object NombreVacio : Resultado
        data object Repetida : Resultado
    }

    suspend operator fun invoke(
        modo: Modo,
        nombre: String,
        tipo: TipoPartida,
        monto: Monto,
        desde: YearMonth,
        tipoGasto: TipoGasto? = null,
        semanal: Boolean = false,
        deuda: Boolean = false,
    ): Resultado {
        val limpio = nombre.trim().take(MAX_NOMBRE_PARTIDA)
        if (limpio.isEmpty()) return Resultado.NombreVacio
        val existentes = repositorio.observarPartidas().first().filter { it.categoria == modo.categoria }
        if (existentes.any { it.nombre.equals(limpio, ignoreCase = true) }) return Resultado.Repetida
        val partida = Partida(
            id = generarId(),
            nombre = limpio,
            tipo = tipo,
            categoria = modo.categoria,
            subcategoria = if (tipo == TipoPartida.GASTO && tipoGasto != TipoGasto.AHORRO) SubcategoriaGasto.OTROS else null,
            orden = (existentes.maxOfOrNull { it.orden } ?: -1) + 1,
            tipoGasto = if (tipo == TipoPartida.GASTO) tipoGasto ?: TipoGasto.PRESCINDIBLE else null,
            semanal = semanal && tipo == TipoPartida.GASTO,
            deuda = deuda && tipo == TipoPartida.GASTO,
        )
        repositorio.guardarPartida(partida)
        if (monto > Monto.CERO) repositorio.fijarMonto(MontoPresupuesto(partida.id, desde, monto))
        return Resultado.Agregada(partida)
    }
}

/** Cambia cuánto se presupuesta en una partida desde [mes]: los meses anteriores no cambian. */
class FijarMontoPartidaUseCase(
    private val repositorio: PresupuestoRepository,
    private val actividades: ActividadRepository,
    private val reloj: Reloj,
) {
    suspend operator fun invoke(partida: Partida, mes: YearMonth, monto: Monto) {
        repositorio.fijarMonto(MontoPresupuesto(partida.id, mes, if (monto < Monto.CERO) Monto.CERO else monto))
        actividades.registrar("Presupuestaste ${monto.enSoles()} para ${partida.nombre} desde $mes.", reloj.ahora())
    }
}

/** Cambia el nombre de una partida y, en las de gasto, su tipo, si es semanal y si es una deuda (RP-01, RP-08, RP-10). */
class RenombrarPartidaUseCase(private val repositorio: PresupuestoRepository) {
    /** false si el nombre está vacío o ya existe en el modo. */
    suspend operator fun invoke(
        partida: Partida,
        nombre: String,
        tipoGasto: TipoGasto? = partida.tipoGasto,
        semanal: Boolean = partida.semanal,
        deuda: Boolean = partida.deuda,
    ): Boolean {
        val limpio = nombre.trim().take(MAX_NOMBRE_PARTIDA)
        if (limpio.isEmpty()) return false
        val otras = repositorio.observarPartidas().first().filter { it.categoria == partida.categoria && it.id != partida.id }
        if (otras.any { it.nombre.equals(limpio, ignoreCase = true) }) return false
        val esGasto = partida.tipo == TipoPartida.GASTO
        repositorio.guardarPartida(
            partida.copy(
                nombre = limpio,
                tipoGasto = if (esGasto) tipoGasto else null,
                semanal = esGasto && semanal,
                deuda = esGasto && deuda,
            ),
        )
        return true
    }
}

/**
 * RP-07, cierre de mes: el presupuesto del mes siguiente queda con lo que de verdad se gastó en cada partida
 * en [mes]. Devuelve cuántas partidas cambiaron.
 */
class UsarRealComoPresupuestoUseCase(
    private val repositorio: PresupuestoRepository,
    private val actividades: ActividadRepository,
    private val reloj: Reloj,
) {
    suspend operator fun invoke(presupuesto: PresupuestoMes): Int {
        val siguiente = presupuesto.mes.plusMonth()
        val cambios = presupuesto.realComoPresupuesto()
        cambios.forEach { (partida, monto) -> repositorio.fijarMonto(MontoPresupuesto(partida.id, siguiente, monto)) }
        if (cambios.isNotEmpty()) {
            actividades.registrar("Usaste lo real de ${presupuesto.mes} como presupuesto de $siguiente.", reloj.ahora())
        }
        return cambios.size
    }
}

/** Quita una partida: sus gastos e ingresos se quedan, sin partida. Queda en Actividad. */
class EliminarPartidaUseCase(
    private val repositorio: PresupuestoRepository,
    private val actividades: ActividadRepository,
    private val reloj: Reloj,
) {
    suspend operator fun invoke(partida: Partida) {
        repositorio.eliminarPartida(partida.id)
        actividades.registrar("Quitaste ${partida.nombre} de tu presupuesto.", reloj.ahora())
    }
}

/** K6 y K7 (documento 01 §11.2): uso del presupuesto y avance de las metas en el mes, del modo actual. */
data class IndicadoresPlanes(
    val conPresupuesto: Boolean,
    /** Qué parte de lo gastado en el mes cayó en una partida; null si no se gastó nada. */
    val gastosConPartida: Int?,
    val metasConAbono: Int,
    val metas: Int,
)

class ObservarIndicadoresPlanesUseCase(
    private val observarModo: ObservarModoUseCase,
    private val observarPresupuestoMes: ObservarPresupuestoMesUseCase,
    private val metas: MetaRepository,
    private val reloj: Reloj,
) {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<IndicadoresPlanes> = observarModo().flatMapLatest { modo ->
        val mes = reloj.hoy().yearMonth
        combine(observarPresupuestoMes(mes, modo), metas.observarMetas(), metas.observarAbonos()) { presupuesto, listaMetas, abonos ->
            val delModo = listaMetas.filter { it.categoria == modo.categoria }
            val conAbono = abonos.filter { it.fecha.yearMonth == mes }.map { it.metaId }.toSet()
            val gastado = presupuesto.realGastos
            IndicadoresPlanes(
                conPresupuesto = presupuesto.hayPartidas,
                gastosConPartida = if (gastado > Monto.CERO && presupuesto.hayPartidas) {
                    ((gastado - presupuesto.gastoSinPartida).centimos * 100 / gastado.centimos).toInt()
                } else {
                    null
                },
                metasConAbono = delModo.count { it.id in conAbono },
                metas = delModo.size,
            )
        }
    }
}
