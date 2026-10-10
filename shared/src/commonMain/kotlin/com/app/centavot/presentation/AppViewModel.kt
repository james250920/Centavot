package com.app.centavot.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.centavot.domain.model.Modo
import com.app.centavot.domain.model.Perfil
import com.app.centavot.domain.model.RegimenTributario
import com.app.centavot.domain.usecase.AceptarAvisoPrivacidadUseCase
import com.app.centavot.domain.usecase.CambiarModoUseCase
import com.app.centavot.domain.usecase.ObservarConsentimientoUseCase
import com.app.centavot.domain.usecase.ObservarPerfilUseCase
import com.app.centavot.domain.usecase.ObservarRegimenUseCase
import com.app.centavot.domain.usecase.RegistrarAperturaUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class EstadoApp { CARGANDO, SIN_CONSENTIMIENTO, SIN_PERFIL, SIN_REGIMEN, LISTA }

/**
 * Qué mostrar: primero el aviso de privacidad (hasta aceptarlo), luego el onboarding (nombre y
 * modo; el régimen solo en modo negocio) y después la app. Quien ya eligió régimen antes de que
 * existiera el perfil entra directo. Si alguien pasa de personal a negocio sin régimen, se le pide.
 */
fun estadoApp(consentido: Boolean, perfil: Perfil?, regimen: RegimenTributario?): EstadoApp = when {
    !consentido -> EstadoApp.SIN_CONSENTIMIENTO
    regimen != null -> EstadoApp.LISTA
    perfil == null -> EstadoApp.SIN_PERFIL
    perfil.modo == Modo.PERSONAL -> EstadoApp.LISTA
    else -> EstadoApp.SIN_REGIMEN
}

class AppViewModel(
    observarPerfil: ObservarPerfilUseCase,
    observarRegimen: ObservarRegimenUseCase,
    observarConsentimiento: ObservarConsentimientoUseCase,
    private val aceptarAviso: AceptarAvisoPrivacidadUseCase,
    private val registrarApertura: RegistrarAperturaUseCase,
    private val cambiarModo: CambiarModoUseCase,
) : ViewModel() {

    /** KPI principal de la primera fase: cuántas veces al día se usa la app. Se llama al volver al primer plano. */
    fun alVolverALaApp() {
        viewModelScope.launch { registrarApertura() }
    }

    fun aceptarAvisoPrivacidad() {
        viewModelScope.launch { aceptarAviso() }
    }

    /** Desde la pantalla del régimen: quien no quiere darlo puede quedarse con lo personal. */
    fun usarModoPersonal() {
        viewModelScope.launch { cambiarModo(Modo.PERSONAL) }
    }

    val estado: StateFlow<EstadoApp> =
        combine(observarConsentimiento(), observarPerfil(), observarRegimen(), ::estadoApp)
            .stateIn(viewModelScope, SharingStarted.Eagerly, EstadoApp.CARGANDO)
}
