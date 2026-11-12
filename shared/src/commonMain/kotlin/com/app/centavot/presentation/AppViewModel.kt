package com.app.centavot.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.centavot.core.util.ResultadoIngreso
import com.app.centavot.core.util.Sesion
import com.app.centavot.domain.model.Modo
import com.app.centavot.domain.model.Perfil
import com.app.centavot.domain.model.RegimenTributario
import com.app.centavot.domain.usecase.AceptarAvisoPrivacidadUseCase
import com.app.centavot.domain.usecase.CambiarModoUseCase
import com.app.centavot.domain.usecase.ObservarConsentimientoUseCase
import com.app.centavot.domain.usecase.ObservarPerfilUseCase
import com.app.centavot.domain.usecase.ObservarRegimenUseCase
import com.app.centavot.domain.usecase.RegistrarAperturaUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class EstadoApp { CARGANDO, SIN_CONSENTIMIENTO, SIN_SESION, SIN_PERFIL, SIN_REGIMEN, LISTA }

/**
 * Qué mostrar: primero el aviso de privacidad (hasta aceptarlo), luego entrar con Google ([sinSesion]), el onboarding (nombre y
 * modo; el régimen solo en modo negocio) y después la app. Quien ya eligió régimen antes de que
 * existiera el perfil entra directo. Si alguien pasa de personal a negocio sin régimen, se le pide.
 */
fun estadoApp(consentido: Boolean, perfil: Perfil?, regimen: RegimenTributario?, sinSesion: Boolean = false): EstadoApp = when {
    !consentido -> EstadoApp.SIN_CONSENTIMIENTO
    sinSesion -> EstadoApp.SIN_SESION
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
    private val sesion: Sesion,
) : ViewModel() {

    private val _ingresando = MutableStateFlow(false)
    val ingresando: StateFlow<Boolean> = _ingresando.asStateFlow()
    private val _errorIngreso = MutableStateFlow<String?>(null)
    val errorIngreso: StateFlow<String?> = _errorIngreso.asStateFlow()

    fun entrarConGoogle() {
        if (_ingresando.value) return
        viewModelScope.launch {
            _ingresando.value = true
            _errorIngreso.value = null
            val resultado = sesion.entrarConGoogle()
            if (resultado is ResultadoIngreso.Error) _errorIngreso.value = resultado.mensaje
            _ingresando.value = false
        }
    }

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
        combine(observarConsentimiento(), observarPerfil(), observarRegimen(), sesion.usuario) { consentido, perfil, regimen, usuario ->
            estadoApp(consentido, perfil, regimen, sinSesion = sesion.disponible && usuario == null)
        }
            .stateIn(viewModelScope, SharingStarted.Eagerly, EstadoApp.CARGANDO)
}
