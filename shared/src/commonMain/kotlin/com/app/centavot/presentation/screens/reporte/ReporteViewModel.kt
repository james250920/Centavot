package com.app.centavot.presentation.screens.reporte

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.centavot.core.util.Reloj
import com.app.centavot.domain.model.ReporteSunat
import com.app.centavot.domain.usecase.ObservarReporteUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minusMonth
import kotlinx.datetime.plusMonth
import kotlinx.datetime.yearMonth

data class ReporteUiState(
    val hoy: LocalDate,
    val periodo: YearMonth,
    val reporte: ReporteSunat? = null,
    val cargando: Boolean = true,
) {
    /** No se puede avanzar a meses futuros. */
    val puedeAvanzar: Boolean get() = periodo < hoy.yearMonth
}

class ReporteViewModel(
    observarReporte: ObservarReporteUseCase,
    private val reloj: Reloj,
) : ViewModel() {

    private val periodo = MutableStateFlow(reloj.hoy().yearMonth)

    @OptIn(ExperimentalCoroutinesApi::class)
    val estado: StateFlow<ReporteUiState> = periodo.flatMapLatest { mes ->
        observarReporte(mes).map { reporte ->
            ReporteUiState(hoy = reloj.hoy(), periodo = mes, reporte = reporte, cargando = false)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ReporteUiState(hoy = reloj.hoy(), periodo = reloj.hoy().yearMonth),
    )

    fun mesAnterior() = periodo.update { it.minusMonth() }

    fun mesSiguiente() = periodo.update { mes -> if (mes < reloj.hoy().yearMonth) mes.plusMonth() else mes }
}
