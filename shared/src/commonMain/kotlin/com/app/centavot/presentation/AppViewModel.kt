package com.app.centavot.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.centavot.domain.usecase.ObservarRegimenUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class EstadoApp { CARGANDO, SIN_REGIMEN, LISTA }

/** Decide si mostrar el onboarding (elegir régimen) o la app. */
class AppViewModel(observarRegimen: ObservarRegimenUseCase) : ViewModel() {
    val estado: StateFlow<EstadoApp> = observarRegimen()
        .map { if (it == null) EstadoApp.SIN_REGIMEN else EstadoApp.LISTA }
        .stateIn(viewModelScope, SharingStarted.Eagerly, EstadoApp.CARGANDO)
}
