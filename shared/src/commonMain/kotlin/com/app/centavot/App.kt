package com.app.centavot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.centavot.presentation.AppViewModel
import com.app.centavot.presentation.EstadoApp
import com.app.centavot.presentation.navigation.NavegacionPrincipal
import com.app.centavot.presentation.screens.regimen.RegimenScreen
import com.app.centavot.presentation.theme.CentavotTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    CentavotTheme {
        val viewModel = koinViewModel<AppViewModel>()
        val estado by viewModel.estado.collectAsStateWithLifecycle()
        when (estado) {
            EstadoApp.CARGANDO -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
            // Al guardar el régimen, el estado pasa a LISTA solo y se muestra la app.
            EstadoApp.SIN_REGIMEN -> RegimenScreen(esPrimeraVez = true, onCerrar = {})
            EstadoApp.LISTA -> NavegacionPrincipal()
        }
    }
}
