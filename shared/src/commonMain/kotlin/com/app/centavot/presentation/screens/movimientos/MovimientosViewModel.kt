package com.app.centavot.presentation.screens.movimientos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.centavot.core.util.Reloj
import com.app.centavot.domain.model.Modo
import com.app.centavot.domain.model.delModo
import com.app.centavot.domain.usecase.ObservarModoUseCase
import com.app.centavot.presentation.components.esEntradaEn
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.Periodo
import com.app.centavot.domain.model.rango
import com.app.centavot.domain.model.sumar
import com.app.centavot.domain.usecase.ObservarGastosUseCase
import com.app.centavot.domain.usecase.ObservarIngresosUseCase
import com.app.centavot.presentation.components.Movimiento
import com.app.centavot.presentation.components.movimientosDe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate

/** Qué movimientos ver. Las etiquetas y opciones dependen del modo (negocio o personal). */
enum class FiltroMovimientos {
    TODOS,
    ENTRADAS,
    GASTOS,

    /** Solo en el negocio: lo que se sacó de la caja para la casa. */
    PARA_LA_CASA,
    ;

    fun etiqueta(modo: Modo): String = when (this) {
        TODOS -> "Todos"
        ENTRADAS -> if (modo == Modo.NEGOCIO) "Ventas" else "Ingresos"
        GASTOS -> "Gastos"
        PARA_LA_CASA -> "Para la casa"
    }

    fun incluye(movimiento: Movimiento, modo: Modo): Boolean = when (this) {
        TODOS -> true
        ENTRADAS -> movimiento.esEntradaEn(modo)
        GASTOS -> movimiento is Movimiento.Salida
        PARA_LA_CASA -> movimiento is Movimiento.Entrada && movimiento.ingreso.retiroDelNegocio
    }

    companion object {
        fun para(modo: Modo): List<FiltroMovimientos> = if (modo == Modo.NEGOCIO) entries else entries - PARA_LA_CASA
    }
}

/** Rango de fechas del filtro: todo, un periodo rápido o fechas elegidas por el usuario. */
sealed interface FiltroFechas {
    val etiqueta: String

    data object Todo : FiltroFechas {
        override val etiqueta = "Todo"
    }

    data class Rapido(val periodo: Periodo) : FiltroFechas {
        override val etiqueta get() = periodo.etiqueta
    }

    data class Rango(val desde: LocalDate, val hasta: LocalDate) : FiltroFechas {
        override val etiqueta get() = "Otras fechas"
    }

    fun incluye(fecha: LocalDate, hoy: LocalDate): Boolean = when (this) {
        Todo -> true
        is Rapido -> fecha in periodo.rango(hoy)
        is Rango -> fecha in desde..hasta
    }
}

/** Lo que entró y lo que salió en un día, según el modo. */
data class GrupoDia(val fecha: LocalDate, val movimientos: List<Movimiento>, val modo: Modo = Modo.NEGOCIO) {
    val entro: Monto get() = movimientos.filter { it.esEntradaEn(modo) }.map { it.monto }.sumar()
    val salio: Monto get() = movimientos.filterNot { it.esEntradaEn(modo) }.map { it.monto }.sumar()
}

data class MovimientosUiState(
    val hoy: LocalDate,
    val modo: Modo = Modo.NEGOCIO,
    val filtro: FiltroMovimientos = FiltroMovimientos.TODOS,
    val fechas: FiltroFechas = FiltroFechas.Todo,
    val grupos: List<GrupoDia> = emptyList(),
    /** Ventas en el negocio; ingresos (incluido lo que vino del negocio) en lo personal. */
    val totalEntro: Monto = Monto.CERO,
    val totalGastado: Monto = Monto.CERO,
    /** Solo en el negocio: lo que se sacó para la casa. */
    val totalParaLaCasa: Monto = Monto.CERO,
    val hayMovimientos: Boolean = false,
    val cargando: Boolean = true,
)

class MovimientosViewModel(
    observarIngresos: ObservarIngresosUseCase,
    observarGastos: ObservarGastosUseCase,
    observarModo: ObservarModoUseCase,
    reloj: Reloj,
) : ViewModel() {

    private val filtro = MutableStateFlow(FiltroMovimientos.TODOS)
    private val fechas = MutableStateFlow<FiltroFechas>(FiltroFechas.Todo)

    val estado: StateFlow<MovimientosUiState> = combine(
        combine(observarModo(), observarIngresos(), observarGastos(), ::Triple),
        filtro,
        fechas,
    ) { (modo, ingresos, gastos), filtroElegido, fechasActuales ->
        val hoy = reloj.hoy()
        // Un filtro que no existe en este modo (Para la casa en lo personal) vuelve a Todos.
        val filtroActual = filtroElegido.takeIf { it in FiltroMovimientos.para(modo) } ?: FiltroMovimientos.TODOS
        val todos = movimientosDe(ingresos.delModo(modo), gastos.delModo(modo))
        val filtrados = todos.filter { filtroActual.incluye(it, modo) && fechasActuales.incluye(it.fecha, hoy) }
        MovimientosUiState(
            hoy = hoy,
            modo = modo,
            filtro = filtroActual,
            fechas = fechasActuales,
            grupos = filtrados.groupBy { it.fecha }.map { (fecha, delDia) -> GrupoDia(fecha, delDia, modo) },
            totalEntro = filtrados.filter { it.esEntradaEn(modo) }.map { it.monto }.sumar(),
            totalParaLaCasa = filtrados.filterNot { it.esEntradaEn(modo) || it is Movimiento.Salida }.map { it.monto }.sumar(),
            totalGastado = filtrados.filterIsInstance<Movimiento.Salida>().map { it.monto }.sumar(),
            hayMovimientos = todos.isNotEmpty(),
            cargando = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MovimientosUiState(hoy = reloj.hoy()))

    fun onFiltro(nuevo: FiltroMovimientos) {
        filtro.value = nuevo
    }

    fun onFechas(nuevo: FiltroFechas) {
        fechas.value = nuevo
    }
}
