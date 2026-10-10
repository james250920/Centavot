package com.app.centavot.presentation.screens.actividad

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.centavot.domain.model.MAX_ACTIVIDADES_VISIBLES
import com.app.centavot.presentation.components.EstadoVacio
import com.app.centavot.presentation.components.Iconos
import com.app.centavot.presentation.components.formatear
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActividadScreen(
    onCerrar: () -> Unit,
    viewModel: ActividadViewModel = koinViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (estado.completo) "Historial completo" else "Actividad") },
                navigationIcon = {
                    IconButton(onClick = onCerrar) { Icon(Iconos.Atras, contentDescription = "Volver") }
                },
            )
        },
    ) { padding ->
        when {
            estado.cargando -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            estado.actividades.isEmpty() -> EstadoVacio(
                titulo = "Sin actividad todavía",
                mensaje = "Aquí verás todo lo que hagas en Lucka: gastos, cobros y cambios en tus ajustes.",
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(estado.actividades, key = { it.id }) { actividad ->
                    ListItem(
                        headlineContent = { Text(actividad.descripcion) },
                        supportingContent = { Text(actividad.fechaHora.formatear(estado.hoy)) },
                    )
                }
                // Solo se muestran las más recientes; el historial completo se conserva siempre.
                if (!estado.completo && estado.actividades.size >= MAX_ACTIVIDADES_VISIBLES) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            OutlinedButton(onClick = viewModel::verHistorialCompleto) { Text("Ver historial completo") }
                        }
                        Text(
                            text = "Mostramos tus $MAX_ACTIVIDADES_VISIBLES actividades más recientes. No se borra nada.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        )
                    }
                }
            }
        }
    }
}
