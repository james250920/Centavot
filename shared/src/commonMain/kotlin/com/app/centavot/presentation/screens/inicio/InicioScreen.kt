package com.app.centavot.presentation.screens.inicio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.centavot.domain.model.Monto
import com.app.centavot.presentation.components.EstadoVacio
import com.app.centavot.presentation.components.FilaGasto
import com.app.centavot.presentation.components.GraficoDistribucion
import com.app.centavot.presentation.components.Iconos
import com.app.centavot.presentation.components.ParteGrafico
import com.app.centavot.presentation.components.TarjetaTope
import com.app.centavot.presentation.components.formatear
import kotlinx.datetime.yearMonth
import org.koin.compose.viewmodel.koinViewModel

private const val MAX_DEUDORES = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InicioScreen(
    onRegistrarGasto: () -> Unit,
    onAbrirGasto: (String) -> Unit,
    onVerMovimientos: () -> Unit,
    onVerCobros: () -> Unit,
    onAbrirNotificaciones: () -> Unit,
    onAbrirActividad: () -> Unit,
    onAbrirAjustes: () -> Unit,
    viewModel: InicioViewModel = koinViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = estado.perfil?.nombre?.let { "Hola, ${it.substringBefore(' ')}" } ?: "Centavot",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                actions = {
                    IconButton(onClick = onAbrirNotificaciones) {
                        BadgedBox(
                            badge = {
                                if (estado.noLeidas > 0) Badge { Text(if (estado.noLeidas > 9) "9+" else "${estado.noLeidas}") }
                            },
                        ) {
                            val descripcion = if (estado.noLeidas > 0) {
                                "Notificaciones, ${estado.noLeidas} sin leer"
                            } else {
                                "Notificaciones"
                            }
                            Icon(Iconos.Notificaciones, contentDescription = descripcion)
                        }
                    }
                    IconButton(onClick = onAbrirActividad) { Icon(Iconos.Historial, contentDescription = "Actividad") }
                    IconButton(onClick = onAbrirAjustes) { Icon(Iconos.Ajustes, contentDescription = "Ajustes") }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onRegistrarGasto,
                icon = { Icon(Iconos.Agregar, contentDescription = null) },
                text = { Text("Registrar gasto") },
            )
        },
    ) { padding ->
        if (estado.cargando) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = estado.hoy.yearMonth.formatear(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val proximidad = estado.proximidad
            val regimen = estado.regimen
            if (proximidad != null && regimen != null) {
                item { TarjetaTope(proximidad, regimen) }
            }

            item { Indicadores(estado, onAbrirAjustes, onVerCobros) }

            estado.resumen?.let { resumen ->
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TarjetaTotal("Negocio este mes", resumen.totalNegocio, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                        TarjetaTotal("Personal este mes", resumen.totalPersonal, MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                    }
                }
                if (resumen.porSubcategoria.isNotEmpty()) {
                    item {
                        Seccion("Distribución de gastos") {
                            GraficoDistribucion(
                                partes = resumen.porSubcategoria.map { (subcategoria, monto) ->
                                    ParteGrafico(subcategoria?.etiqueta ?: "Sin categoría", monto)
                                },
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                }
            }

            estado.cobros?.takeIf { it.porContacto.isNotEmpty() }?.let { cobros ->
                item {
                    Seccion("Te deben", accion = "Ver cobros", onAccion = onVerCobros) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            cobros.porContacto.take(MAX_DEUDORES).forEach { deuda ->
                                Row(Modifier.fillMaxWidth()) {
                                    Text(deuda.contacto.nombre, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(deuda.total.formatear(), fontWeight = FontWeight.SemiBold)
                                }
                            }
                            val otros = cobros.porContacto.size - MAX_DEUDORES
                            if (otros > 0) {
                                Text(
                                    text = "y $otros ${if (otros == 1) "persona más" else "personas más"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Últimos gastos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    if (estado.recientes.isNotEmpty()) {
                        TextButton(onClick = onVerMovimientos) { Text("Ver todos") }
                    }
                }
            }
            if (estado.recientes.isEmpty()) {
                item {
                    EstadoVacio(
                        titulo = "Aún no registras gastos",
                        mensaje = "Anota tu primer gasto: solo toma unos segundos y así sabrás cuánto te queda antes del tope.",
                    ) {
                        Button(onClick = onRegistrarGasto, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Registrar mi primer gasto")
                        }
                    }
                }
            } else {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column {
                            estado.recientes.forEach { gasto ->
                                FilaGasto(gasto, estado.hoy, onClick = { onAbrirGasto(gasto.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Ingreso, gastos, lo que te deben y meta de ahorro del mes (como el panel del prototipo). */
@Composable
private fun Indicadores(estado: InicioUiState, onAbrirAjustes: () -> Unit, onVerCobros: () -> Unit) {
    val perfil = estado.perfil
    val sinIngreso = perfil == null || perfil.ingresoMensual <= Monto.CERO
    val tasa = perfil?.tasaAhorro?.formatear() ?: "0 %"
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TarjetaTotal(
                titulo = "Ingreso mensual",
                monto = perfil?.ingresoMensual ?: Monto.CERO,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).clickable(onClick = onAbrirAjustes),
            )
            TarjetaTotal(
                titulo = "Gastos del mes",
                monto = estado.resumen?.total ?: Monto.CERO,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TarjetaTotal(
                titulo = "Te deben",
                monto = estado.cobros?.totalPendiente ?: Monto.CERO,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f).clickable(onClick = onVerCobros),
            )
            TarjetaTotal(
                titulo = "Meta de ahorro ($tasa)",
                monto = perfil?.metaAhorro ?: Monto.CERO,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f).clickable(onClick = onAbrirAjustes),
            )
        }
        if (sinIngreso) {
            TextButton(onClick = onAbrirAjustes) { Text("Define tu ingreso y tu meta de ahorro en Ajustes") }
        }
    }
}

@Composable
private fun Seccion(
    titulo: String,
    accion: String? = null,
    onAccion: () -> Unit = {},
    contenido: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (accion != null) TextButton(onClick = onAccion) { Text(accion) }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) { contenido() }
    }
}

@Composable
private fun TarjetaTotal(titulo: String, monto: Monto, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(titulo, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(monto.formatear(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = color)
        }
    }
}
