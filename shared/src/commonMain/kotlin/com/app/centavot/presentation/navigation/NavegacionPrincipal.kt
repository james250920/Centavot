package com.app.centavot.presentation.navigation

import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import com.app.centavot.presentation.Avisos
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarDuration
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.app.centavot.presentation.components.Movimiento
import com.app.centavot.presentation.screens.ayuda.AyudaScreen
import com.app.centavot.presentation.screens.privacidad.AvisoPrivacidadScreen
import com.app.centavot.presentation.screens.venta.VentaScreen
import com.app.centavot.presentation.screens.actividad.ActividadScreen
import com.app.centavot.presentation.screens.ajustes.AjustesScreen
import com.app.centavot.presentation.screens.cobros.CobroScreen
import com.app.centavot.presentation.screens.cobros.CobrosScreen
import com.app.centavot.presentation.screens.cobros.ContactosScreen
import com.app.centavot.presentation.screens.gasto.GastoScreen
import com.app.centavot.presentation.screens.inicio.InicioScreen
import com.app.centavot.presentation.screens.movimientos.MovimientosScreen
import com.app.centavot.presentation.screens.notificaciones.NotificacionesScreen
import com.app.centavot.presentation.screens.regimen.RegimenScreen
import com.app.centavot.presentation.screens.reporte.ReporteScreen

private enum class Pestana(val ruta: Any, val etiqueta: String, val icono: () -> ImageVector) {
    INICIO(RutaInicio, "Inicio", { Iconos.Inicio }),
    MOVIMIENTOS(RutaMovimientos, "Movimientos", { Iconos.Movimientos }),
    COBROS(RutaCobros, "Cobros", { Iconos.Cobros }),
    REPORTE(RutaReporte, "Reportes", { Iconos.Reporte }),
}

/** Alto del botón principal fijo abajo en los formularios (48 dp más 16 dp de margen arriba y abajo). */
private val ALTO_BOTON_INFERIOR = 80.dp

@Composable
fun NavegacionPrincipal() {
    val nav = rememberNavController()
    val entradaActual by nav.currentBackStackEntryAsState()
    val pestanaActual = Pestana.entries.firstOrNull { pestana ->
        entradaActual?.destination?.hasRoute(pestana.ruta::class) == true
    }

    // Avisos tras guardar ("Gasto de S/ 12.00 guardado · Deshacer"), aunque la pantalla que lo pidió ya se cerró.
    val avisos = koinInject<Avisos>()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(avisos) {
        avisos.flujo.collect { aviso ->
            val resultado = snackbar.showSnackbar(
                message = aviso.mensaje,
                actionLabel = if (aviso.deshacer != null) "Deshacer" else null,
                withDismissAction = aviso.deshacer != null,
                duration = if (aviso.deshacer != null) SnackbarDuration.Long else SnackbarDuration.Short,
            )
            if (resultado == SnackbarResult.ActionPerformed) aviso.deshacer?.invoke()
        }
    }

    Scaffold(
        snackbarHost = {
            // En formularios (sin barra de pestañas) el botón principal va abajo: el aviso se pone encima de él.
            SnackbarHost(snackbar, Modifier.padding(bottom = if (pestanaActual == null) ALTO_BOTON_INFERIOR else 0.dp))
        },
        bottomBar = {
            // La barra solo se muestra en las pestañas principales, no en formularios.
            if (pestanaActual != null) {
                NavigationBar {
                    Pestana.entries.forEach { pestana ->
                        NavigationBarItem(
                            selected = pestana == pestanaActual,
                            onClick = { nav.irAPestana(pestana.ruta) },
                            icon = { Icon(pestana.icono(), contentDescription = null) },
                            label = {
                                // Una sola línea y estilo más pequeño: "Movimientos" no se parte con texto grande.
                                Text(pestana.etiqueta, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                            },
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
                    onRegistrarVenta = { nav.navigate(RutaVenta()) },
                    onRegistrarGasto = { nav.navigate(RutaGasto()) },
                    onRegistrarRetiro = { nav.navigate(RutaVenta(retiro = true)) },
                    onAbrirMovimiento = { nav.abrirMovimiento(it) },
                    onVerMovimientos = { nav.irAPestana(RutaMovimientos) },
                    onVerCobros = { nav.irAPestana(RutaCobros) },
                    onAbrirNotificaciones = { nav.navigate(RutaNotificaciones) },
                    onAbrirAjustes = { nav.navigate(RutaAjustes) },
                    onAbrirAyuda = { nav.navigate(RutaAyuda) },
                )
            }
            composable<RutaMovimientos> {
                MovimientosScreen(
                    onAbrirMovimiento = { nav.abrirMovimiento(it) },
                    onRegistrarVenta = { nav.navigate(RutaVenta()) },
                    onRegistrarGasto = { nav.navigate(RutaGasto()) },
                )
            }
            composable<RutaCobros> {
                CobrosScreen(
                    onRegistrarCobro = { nav.navigate(RutaCobro()) },
                    onEditarCobro = { nav.navigate(RutaCobro(it)) },
                    onAbrirContactos = { nav.navigate(RutaContactos) },
                )
            }
            composable<RutaCobro> { entrada ->
                CobroScreen(id = entrada.toRoute<RutaCobro>().id, onCerrar = { nav.popBackStack() })
            }
            composable<RutaContactos> {
                ContactosScreen(onCerrar = { nav.popBackStack() })
            }
            composable<RutaAjustes> {
                AjustesScreen(
                    esPrimeraVez = false,
                    onCerrar = { nav.popBackStack() },
                    onCambiarRegimen = { nav.navigate(RutaRegimen) },
                    onVerPrivacidad = { nav.navigate(RutaPrivacidad) },
                    onAbrirActividad = { nav.navigate(RutaActividad) },
                )
            }
            composable<RutaActividad> {
                ActividadScreen(onCerrar = { nav.popBackStack() })
            }
            composable<RutaNotificaciones> {
                NotificacionesScreen(onCerrar = { nav.popBackStack() })
            }
            composable<RutaReporte> {
                ReporteScreen(
                    onAbrirGasto = { nav.navigate(RutaGasto(it)) },
                    onAbrirVenta = { nav.navigate(RutaVenta(it)) },
                )
            }
            composable<RutaGasto> { entrada ->
                GastoScreen(id = entrada.toRoute<RutaGasto>().id, onCerrar = { nav.popBackStack() })
            }
            composable<RutaVenta> { entrada ->
                val ruta = entrada.toRoute<RutaVenta>()
                VentaScreen(id = ruta.id, retiro = ruta.retiro, onCerrar = { nav.popBackStack() })
            }
            composable<RutaPrivacidad> {
                AvisoPrivacidadScreen(onCerrar = { nav.popBackStack() })
            }
            composable<RutaAyuda> {
                AyudaScreen(onCerrar = { nav.popBackStack() })
            }
            composable<RutaRegimen> {
                RegimenScreen(esPrimeraVez = false, onCerrar = { nav.popBackStack() })
            }
        }
    }
}

private fun NavHostController.abrirMovimiento(movimiento: Movimiento) = when (movimiento) {
    is Movimiento.Entrada -> navigate(RutaVenta(movimiento.id))
    is Movimiento.Salida -> navigate(RutaGasto(movimiento.id))
}

private fun NavHostController.irAPestana(ruta: Any) = navigate(ruta) {
    popUpTo(RutaInicio) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
