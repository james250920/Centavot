package com.app.centavot.presentation.screens.movimientos

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.centavot.presentation.components.EstadoVacio
import com.app.centavot.presentation.components.FilaGasto
import com.app.centavot.presentation.components.Iconos
import com.app.centavot.presentation.components.formatear
import com.app.centavot.presentation.components.formatearRelativo
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovimientosScreen(
    onAbrirGasto: (String) -> Unit,
    onRegistrarGasto: () -> Unit,
    viewModel: MovimientosViewModel = koinViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Movimientos") }) },
        floatingActionButton = {
            if (estado.hayGastos) {
                FloatingActionButton(onClick = onRegistrarGasto) {
                    Icon(Iconos.Agregar, contentDescription = "Registrar gasto")
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FiltroMovimientos.entries.forEach { filtro ->
                    FilterChip(
                        selected = estado.filtro == filtro,
                        onClick = { viewModel.onFiltro(filtro) },
                        label = { Text(filtro.etiqueta) },
                    )
                }
            }

            when {
                estado.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                !estado.hayGastos -> EstadoVacio(
                    titulo = "Aún no hay movimientos",
                    mensaje = "Cuando registres gastos, aparecerán aquí agrupados por día.",
                ) {
                    Button(onClick = onRegistrarGasto, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Registrar gasto")
                    }
                }
                estado.grupos.isEmpty() -> EstadoVacio(
                    titulo = "Sin gastos de ${estado.filtro.etiqueta.lowercase()}",
                    mensaje = "Prueba con otro filtro o registra un gasto nuevo.",
                )
                else -> LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                    estado.grupos.forEach { grupo ->
                        item(key = "dia-${grupo.fecha}") {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = grupo.fecha.formatearRelativo(estado.hoy),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = grupo.total.formatear(),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        items(grupo.gastos, key = { it.id }) { gasto ->
                            FilaGasto(gasto, estado.hoy, onClick = { onAbrirGasto(gasto.id) }, mostrarFecha = false)
                        }
                    }
                }
            }
        }
    }
}
