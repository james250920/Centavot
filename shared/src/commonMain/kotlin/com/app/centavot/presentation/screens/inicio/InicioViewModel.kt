package com.app.centavot.presentation.screens.inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.centavot.core.util.Reloj
import com.app.centavot.domain.model.Gasto
import com.app.centavot.domain.model.ProximidadTope
import com.app.centavot.domain.model.RegimenTributario
import com.app.centavot.domain.model.ResumenMes
import com.app.centavot.domain.usecase.ObservarGastosUseCase
import com.app.centavot.domain.usecase.ObservarProximidadTopeUseCase
import com.app.centavot.domain.usecase.ObservarRegimenUseCase
import com.app.centavot.domain.usecase.ObservarResumenMesUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate

data class InicioUiState(
    val hoy: LocalDate,
    val cargando: Boolean = true,
    val regimen: RegimenTributario? = null,
    val proximidad: ProximidadTope? = null,
    val resumen: ResumenMes? = null,
    val recientes: List<Gasto> = emptyList(),
)

private const val CANTIDAD_RECIENTES = 5

class InicioViewModel(
    observarRegimen: ObservarRegimenUseCase,
    observarProximidadTope: ObservarProximidadTopeUseCase,
    observarResumenMes: ObservarResumenMesUseCase,
    observarGastos: ObservarGastosUseCase,
    reloj: Reloj,
) : ViewModel() {

    val estado: StateFlow<InicioUiState> = combine(
        observarRegimen(),
        observarProximidadTope(),
        observarResumenMes(),
        observarGastos(),
    ) { regimen, proximidad, resumen, gastos ->
        InicioUiState(
            hoy = reloj.hoy(),
            cargando = false,
            regimen = regimen,
            proximidad = proximidad,
            resumen = resumen,
            recientes = gastos.take(CANTIDAD_RECIENTES),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InicioUiState(hoy = reloj.hoy()))
}
