package com.app.centavot.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.centavot.domain.usecase.ObservarPerfilUseCase
import com.app.centavot.domain.usecase.ObservarRegimenUseCase
import com.app.centavot.domain.usecase.RegistrarAperturaUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class EstadoApp { CARGANDO, SIN_PERFIL, SIN_REGIMEN, LISTA }

/**
 * Decide qué mostrar: el onboarding (perfil y luego régimen) o la app.
 * Quien ya eligió régimen antes de que existiera el perfil entra directo.
 */
class AppViewModel(
    observarPerfil: ObservarPerfilUseCase,
    observarRegimen: ObservarRegimenUseCase,
    private val registrarApertura: RegistrarAperturaUseCase,
) : ViewModel() {

    /** KPI principal de la primera fase: cuántas veces al día se usa la app. Se llama al volver al primer plano. */
    fun alVolverALaApp() {
        viewModelScope.launch { registrarApertura() }
    }

    val estado: StateFlow<EstadoApp> = combine(observarPerfil(), observarRegimen()) { perfil, regimen ->
        when {
            regimen != null -> EstadoApp.LISTA
            perfil == null -> EstadoApp.SIN_PERFIL
            else -> EstadoApp.SIN_REGIMEN
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, EstadoApp.CARGANDO)
}
