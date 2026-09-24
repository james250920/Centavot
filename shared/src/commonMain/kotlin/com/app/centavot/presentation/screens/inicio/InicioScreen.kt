package com.app.centavot.presentation.screens.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.centavot.domain.model.Monto
import com.app.centavot.presentation.components.EstadoVacio
import com.app.centavot.presentation.components.FilaGasto
import com.app.centavot.presentation.components.Iconos
import com.app.centavot.presentation.components.TarjetaTope
import com.app.centavot.presentation.components.formatear
import kotlinx.datetime.yearMonth
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InicioScreen(
    onRegistrarGasto: () -> Unit,
    onAbrirGasto: (String) -> Unit,
    onVerMovimientos: () -> Unit,
    onCambiarRegimen: () -> Unit,
    viewModel: InicioViewModel = koinViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Centavot", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onCambiarRegimen) {
                        Icon(Iconos.Editar, contentDescription = "Cambiar régimen tributario")
                    }
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
            estado.resumen?.let { resumen ->
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TarjetaTotal("Negocio este mes", resumen.totalNegocio, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                        TarjetaTotal("Personal este mes", resumen.totalPersonal, MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
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
