package com.app.centavot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.centavot.presentation.AppViewModel
import com.app.centavot.presentation.EstadoApp
import com.app.centavot.presentation.navigation.NavegacionPrincipal
import com.app.centavot.presentation.screens.ajustes.AjustesScreen
import com.app.centavot.presentation.screens.regimen.RegimenScreen
import com.app.centavot.presentation.theme.CentavotTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    CentavotTheme {
        val viewModel = koinViewModel<AppViewModel>()
        val estado by viewModel.estado.collectAsStateWithLifecycle()
        LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.alVolverALaApp() }
        when (estado) {
            EstadoApp.CARGANDO -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
            // Al guardar el perfil y luego el régimen, el estado avanza solo hasta LISTA.
            EstadoApp.SIN_PERFIL -> AjustesScreen(esPrimeraVez = true, onCerrar = {}, onCambiarRegimen = {})
            EstadoApp.SIN_REGIMEN -> RegimenScreen(esPrimeraVez = true, onCerrar = {})
            EstadoApp.LISTA -> NavegacionPrincipal()
        }
    }
}
