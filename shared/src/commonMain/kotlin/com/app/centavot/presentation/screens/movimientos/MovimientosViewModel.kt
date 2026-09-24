package com.app.centavot.presentation.screens.movimientos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.centavot.core.util.Reloj
import com.app.centavot.domain.model.Categoria
import com.app.centavot.domain.model.Gasto
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.sumar
import com.app.centavot.domain.usecase.ObservarGastosUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate

enum class FiltroMovimientos(val etiqueta: String, val categoria: Categoria?) {
    TODOS("Todos", null),
    NEGOCIO("Negocio", Categoria.NEGOCIO),
    PERSONAL("Personal", Categoria.PERSONAL),
}

data class GrupoDia(val fecha: LocalDate, val gastos: List<Gasto>, val total: Monto)

data class MovimientosUiState(
    val hoy: LocalDate,
    val filtro: FiltroMovimientos = FiltroMovimientos.TODOS,
    val grupos: List<GrupoDia> = emptyList(),
    val hayGastos: Boolean = false,
    val cargando: Boolean = true,
)

class MovimientosViewModel(
    observarGastos: ObservarGastosUseCase,
    reloj: Reloj,
) : ViewModel() {

    private val filtro = MutableStateFlow(FiltroMovimientos.TODOS)

    val estado: StateFlow<MovimientosUiState> = combine(observarGastos(), filtro) { gastos, filtroActual ->
        val filtrados = filtroActual.categoria?.let { categoria -> gastos.filter { it.categoria == categoria } } ?: gastos
        MovimientosUiState(
            hoy = reloj.hoy(),
            filtro = filtroActual,
            grupos = filtrados.groupBy { it.fecha }.map { (fecha, delDia) ->
                GrupoDia(fecha, delDia, delDia.map { it.monto }.sumar())
            },
            hayGastos = gastos.isNotEmpty(),
            cargando = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MovimientosUiState(hoy = reloj.hoy()))

    fun onFiltro(nuevo: FiltroMovimientos) {
        filtro.value = nuevo
    }
}
