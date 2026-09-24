package com.app.centavot.presentation.navigation

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.app.centavot.presentation.components.Iconos
import com.app.centavot.presentation.screens.gasto.GastoScreen
import com.app.centavot.presentation.screens.inicio.InicioScreen
import com.app.centavot.presentation.screens.movimientos.MovimientosScreen
import com.app.centavot.presentation.screens.regimen.RegimenScreen
import com.app.centavot.presentation.screens.reporte.ReporteScreen

private enum class Pestana(val ruta: Any, val etiqueta: String, val icono: () -> ImageVector) {
    INICIO(RutaInicio, "Inicio", { Iconos.Inicio }),
    MOVIMIENTOS(RutaMovimientos, "Movimientos", { Iconos.Movimientos }),
    REPORTE(RutaReporte, "Reporte", { Iconos.Reporte }),
}

@Composable
fun NavegacionPrincipal() {
    val nav = rememberNavController()
    val entradaActual by nav.currentBackStackEntryAsState()
    val pestanaActual = Pestana.entries.firstOrNull { pestana ->
        entradaActual?.destination?.hasRoute(pestana.ruta::class) == true
    }

    Scaffold(
        bottomBar = {
            // La barra solo se muestra en las pestañas principales, no en formularios.
            if (pestanaActual != null) {
                NavigationBar {
                    Pestana.entries.forEach { pestana ->
                        NavigationBarItem(
                            selected = pestana == pestanaActual,
                            onClick = { nav.irAPestana(pestana.ruta) },
                            icon = { Icon(pestana.icono(), contentDescription = null) },
                            label = { Text(pestana.etiqueta) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = RutaInicio,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            composable<RutaInicio> {
                InicioScreen(
                    onRegistrarGasto = { nav.navigate(RutaGasto()) },
                    onAbrirGasto = { nav.navigate(RutaGasto(it)) },
                    onVerMovimientos = { nav.irAPestana(RutaMovimientos) },
                    onCambiarRegimen = { nav.navigate(RutaRegimen) },
                )
            }
            composable<RutaMovimientos> {
                MovimientosScreen(
                    onAbrirGasto = { nav.navigate(RutaGasto(it)) },
                    onRegistrarGasto = { nav.navigate(RutaGasto()) },
                )
            }
            composable<RutaReporte> {
                ReporteScreen(onAbrirGasto = { nav.navigate(RutaGasto(it)) })
            }
            composable<RutaGasto> { entrada ->
                GastoScreen(id = entrada.toRoute<RutaGasto>().id, onCerrar = { nav.popBackStack() })
            }
            composable<RutaRegimen> {
                RegimenScreen(esPrimeraVez = false, onCerrar = { nav.popBackStack() })
            }
        }
    }
}

private fun NavHostController.irAPestana(ruta: Any) = navigate(ruta) {
    popUpTo(RutaInicio) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
