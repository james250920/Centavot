package com.app.lucka.presentation.screens.notificaciones

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.lucka.presentation.components.EstadoVacio
import com.app.lucka.presentation.components.Iconos
import com.app.lucka.presentation.components.formatear
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificacionesScreen(
    onCerrar: () -> Unit,
    viewModel: NotificacionesViewModel = koinViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notificaciones") },
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
            estado.notificaciones.isEmpty() -> EstadoVacio(
                titulo = "No tienes notificaciones",
                mensaje = "Te avisaremos aquí cuando tus gastos de negocio lleguen al 80, 90 o 100 % del tope de tu régimen.",
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(estado.notificaciones, key = { it.id }) { notificacion ->
                    val esNueva = notificacion.id in estado.nuevas
                    ListItem(
                        colors = ListItemDefaults.colors(
                            containerColor = if (esNueva) MaterialTheme.colorScheme.surfaceContainerLow else Color.Transparent,
                        ),
                        leadingContent = {
                            Box(contentAlignment = Alignment.TopEnd) {
                                Icon(Iconos.Notificaciones, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                                if (esNueva) {
                                    Box(
                                        Modifier.size(8.dp)
                                            .background(MaterialTheme.colorScheme.error, CircleShape)
                                            .semantics { contentDescription = "Nueva" },
                                    )
                                }
                            }
                        },
                        overlineContent = { Text(notificacion.fechaHora.formatear(estado.hoy)) },
                        headlineContent = {
                            Text(
                                text = notificacion.asunto ?: "Aviso de Sistema",
                                fontWeight = if (esNueva) FontWeight.SemiBold else null,
                            )
                        },
                        supportingContent = { Text(notificacion.mensaje) },
                    )
                }
            }
        }
    }
}
