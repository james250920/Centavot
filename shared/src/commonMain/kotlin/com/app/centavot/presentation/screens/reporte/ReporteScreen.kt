package com.app.centavot.presentation.screens.reporte

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.centavot.presentation.components.EstadoVacio
import com.app.centavot.presentation.components.FilaGasto
import com.app.centavot.presentation.components.Iconos
import com.app.centavot.presentation.components.formatear
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReporteScreen(
    onAbrirGasto: (String) -> Unit,
    viewModel: ReporteViewModel = koinViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("Reporte SUNAT") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = viewModel::mesAnterior) {
                        Icon(Iconos.Anterior, contentDescription = "Mes anterior")
                    }
                    Text(
                        text = estado.periodo.formatear(),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = viewModel::mesSiguiente, enabled = estado.puedeAvanzar) {
                        Icon(Iconos.Siguiente, contentDescription = "Mes siguiente")
                    }
                }
            }

            val reporte = estado.reporte
            if (estado.cargando || reporte == null) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                return@LazyColumn
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Gastos de negocio", style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = reporte.total.formatear(),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                        )
                        val cantidad = reporte.gastos.size
                        Text(
                            text = "$cantidad ${if (cantidad == 1) "gasto" else "gastos"} · ${reporte.regimen.nombre}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Iconos.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "Este resumen te ayuda a preparar tu declaración. Revísalo con tu contador antes de presentarla.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (reporte.gastos.isEmpty()) {
                item {
                    EstadoVacio(
                        titulo = "Sin gastos de negocio",
                        mensaje = "No registraste gastos de negocio en ${estado.periodo.formatear().lowercase()}.",
                    )
                }
            } else {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column {
                            reporte.gastos.forEach { gasto ->
                                FilaGasto(gasto, estado.hoy, onClick = { onAbrirGasto(gasto.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}
