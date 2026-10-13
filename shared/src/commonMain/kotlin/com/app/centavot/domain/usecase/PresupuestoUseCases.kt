package com.app.centavot.domain.usecase

import com.app.centavot.core.util.Reloj
import com.app.centavot.domain.model.ItemPlantilla
import com.app.centavot.domain.model.Modo
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.MontoPresupuesto
import com.app.centavot.domain.model.Partida
import com.app.centavot.domain.model.PresupuestoMes
import com.app.centavot.domain.model.SubcategoriaGasto
import com.app.centavot.domain.model.TipoPartida
import com.app.centavot.domain.model.delModo
import com.app.centavot.domain.model.enSoles
import com.app.centavot.domain.repository.ActividadRepository
import com.app.centavot.domain.repository.GastoRepository
import com.app.centavot.domain.repository.IngresoRepository
import com.app.centavot.domain.repository.PresupuestoRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.YearMonth

private const val MAX_NOMBRE_PARTIDA = 40

/** Las partidas de un modo, en orden. */
class ObservarPartidasUseCase(private val repositorio: PresupuestoRepository) {
    operator fun invoke(modo: Modo): Flow<List<Partida>> =
        repositorio.observarPartidas().map { lista -> lista.filter { it.categoria == modo.categoria } }
}

/** El presupuesto de [mes] frente a lo que de verdad entró y se gastó, solo del modo. */
class ObservarPresupuestoMesUseCase(
    private val presupuesto: PresupuestoRepository,
    private val gastos: GastoRepository,
    private val ingresos: IngresoRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(mes: YearMonth, modo: Modo): Flow<PresupuestoMes> = combine(
        presupuesto.observarPartidas(),
        presupuesto.observarMontos(),
        gastos.observarGastosEntre(mes.firstDay, mes.lastDay),
        ingresos.observarIngresosEntre(mes.firstDay, mes.lastDay),
    ) { partidas, montos, listaGastos, listaIngresos ->
        PresupuestoMes.de(
            mes = mes,
            partidas = partidas.filter { it.categoria == modo.categoria },
            montos = montos,
            gastos = listaGastos.delModo(modo),
            // En lo personal también cuenta lo que vino del negocio; en el negocio, el presupuesto es solo de gastos.
            ingresos = if (modo == Modo.PERSONAL) listaIngresos.delModo(modo) else emptyList(),
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
            val partida = Partida(generarId(), nombre, item.tipo, modo.categoria, item.subcategoria, orden++)
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

    suspend operator fun invoke(modo: Modo, nombre: String, tipo: TipoPartida, monto: Monto, desde: YearMonth): Resultado {
        val limpio = nombre.trim().take(MAX_NOMBRE_PARTIDA)
        if (limpio.isEmpty()) return Resultado.NombreVacio
        val existentes = repositorio.observarPartidas().first().filter { it.categoria == modo.categoria }
        if (existentes.any { it.nombre.equals(limpio, ignoreCase = true) }) return Resultado.Repetida
        val partida = Partida(
            id = generarId(),
            nombre = limpio,
            tipo = tipo,
            categoria = modo.categoria,
            subcategoria = if (tipo == TipoPartida.GASTO) SubcategoriaGasto.OTROS else null,
            orden = (existentes.maxOfOrNull { it.orden } ?: -1) + 1,
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

class RenombrarPartidaUseCase(private val repositorio: PresupuestoRepository) {
    /** false si el nombre está vacío o ya existe en el modo. */
    suspend operator fun invoke(partida: Partida, nombre: String): Boolean {
        val limpio = nombre.trim().take(MAX_NOMBRE_PARTIDA)
        if (limpio.isEmpty()) return false
        val otras = repositorio.observarPartidas().first().filter { it.categoria == partida.categoria && it.id != partida.id }
        if (otras.any { it.nombre.equals(limpio, ignoreCase = true) }) return false
        repositorio.guardarPartida(partida.copy(nombre = limpio))
        return true
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
