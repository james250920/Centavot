package com.app.lucka.presentation.screens.movimientos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.lucka.core.util.Reloj
import com.app.lucka.domain.model.Categoria
import com.app.lucka.domain.model.Monto
import com.app.lucka.domain.model.Periodo
import com.app.lucka.domain.model.rango
import com.app.lucka.domain.model.sumar
import com.app.lucka.domain.usecase.ObservarGastosUseCase
import com.app.lucka.domain.usecase.ObservarIngresosUseCase
import com.app.lucka.presentation.components.Movimiento
import com.app.lucka.presentation.components.movimientosDe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate

enum class FiltroMovimientos(val etiqueta: String) {
    TODOS("Todos"),
    VENTAS("Ventas"),
    NEGOCIO("Gastos del negocio"),
    PERSONAL("Gastos personales"),
    INGRESOS_PERSONALES("Ingresos personales"),
    ;

    fun incluye(movimiento: Movimiento): Boolean = when (this) {
        TODOS -> true
        VENTAS -> movimiento.esVenta
        INGRESOS_PERSONALES -> movimiento is Movimiento.Entrada && !movimiento.ingreso.esDeNegocio
        NEGOCIO -> movimiento is Movimiento.Salida && movimiento.gasto.categoria == Categoria.NEGOCIO
        PERSONAL -> movimiento is Movimiento.Salida && movimiento.gasto.categoria == Categoria.PERSONAL
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

/** Solo las ventas del negocio cuentan como "vendido"; un sueldo u otro ingreso personal no. */
private val Movimiento.esVenta: Boolean get() = this is Movimiento.Entrada && ingreso.esDeNegocio

private val Movimiento.esIngresoPersonal: Boolean get() = this is Movimiento.Entrada && !ingreso.esDeNegocio

data class GrupoDia(val fecha: LocalDate, val movimientos: List<Movimiento>) {
    val vendido: Monto get() = movimientos.filter { it.esVenta }.map { it.monto }.sumar()
    val ingresosPersonales: Monto get() = movimientos.filter { it.esIngresoPersonal }.map { it.monto }.sumar()
    val gastado: Monto get() = movimientos.filterIsInstance<Movimiento.Salida>().map { it.monto }.sumar()
}

data class MovimientosUiState(
    val hoy: LocalDate,
    val filtro: FiltroMovimientos = FiltroMovimientos.TODOS,
    val fechas: FiltroFechas = FiltroFechas.Todo,
    val grupos: List<GrupoDia> = emptyList(),
    val totalVendido: Monto = Monto.CERO,
    val totalIngresosPersonales: Monto = Monto.CERO,
    val totalGastado: Monto = Monto.CERO,
    val hayMovimientos: Boolean = false,
    val cargando: Boolean = true,
)

class MovimientosViewModel(
    observarIngresos: ObservarIngresosUseCase,
    observarGastos: ObservarGastosUseCase,
    reloj: Reloj,
) : ViewModel() {

    private val filtro = MutableStateFlow(FiltroMovimientos.TODOS)
    private val fechas = MutableStateFlow<FiltroFechas>(FiltroFechas.Todo)

    val estado: StateFlow<MovimientosUiState> = combine(
        observarIngresos(),
        observarGastos(),
        filtro,
        fechas,
    ) { ingresos, gastos, filtroActual, fechasActuales ->
        val hoy = reloj.hoy()
        val todos = movimientosDe(ingresos, gastos)
        val filtrados = todos.filter { filtroActual.incluye(it) && fechasActuales.incluye(it.fecha, hoy) }
        MovimientosUiState(
            hoy = hoy,
            filtro = filtroActual,
            fechas = fechasActuales,
            grupos = filtrados.groupBy { it.fecha }.map { (fecha, delDia) -> GrupoDia(fecha, delDia) },
            totalVendido = filtrados.filter { it.esVenta }.map { it.monto }.sumar(),
            totalIngresosPersonales = filtrados.filter { it.esIngresoPersonal }.map { it.monto }.sumar(),
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
