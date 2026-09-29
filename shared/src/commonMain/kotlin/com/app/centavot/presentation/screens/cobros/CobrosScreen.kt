package com.app.centavot.presentation.screens.cobros

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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.centavot.domain.model.Cobro
import com.app.centavot.domain.model.ResumenCobros
import com.app.centavot.presentation.components.DialogoConfirmar
import com.app.centavot.presentation.components.EstadoVacio
import com.app.centavot.presentation.components.Iconos
import com.app.centavot.presentation.components.formatear
import com.app.centavot.presentation.components.formatearRelativo
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CobrosScreen(
    onRegistrarCobro: () -> Unit,
    onAbrirContactos: () -> Unit,
    viewModel: CobrosViewModel = koinViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cobros") },
                actions = {
                    IconButton(onClick = onAbrirContactos) {
                        Icon(Iconos.Contactos, contentDescription = "Mis contactos")
                    }
                },
            )
        },
        floatingActionButton = {
            if (estado.cobros.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onRegistrarCobro,
                    icon = { Icon(Iconos.Agregar, contentDescription = null) },
                    text = { Text("Registrar cobro") },
                )
            }
        },
    ) { padding ->
        if (estado.cargando) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        if (estado.cobros.isEmpty()) {
            EstadoVacio(
                titulo = "Nadie te debe por ahora",
                mensaje = "Anota aquí lo que fías o prestas, y marca \"Cobrar\" cuando te paguen. Así no se te olvida nadie.",
                modifier = Modifier.padding(padding),
            ) {
                Button(onClick = onRegistrarCobro, modifier = Modifier.padding(top = 8.dp)) { Text("Registrar un cobro") }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            estado.resumen?.let { resumen -> item { TarjetaTeDeben(resumen) } }

            item { Text("Préstamos y cuentas por cobrar", style = MaterialTheme.typography.titleMedium) }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column {
                        estado.cobros.forEachIndexed { i, cobro ->
                            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            FilaCobro(
                                cobro = cobro,
                                hoy = estado.hoy,
                                onCobrar = { viewModel.cobrar(cobro) },
                                onEliminar = { viewModel.pedirEliminar(cobro) },
                            )
                        }
                    }
                }
            }
        }
    }

    estado.porEliminar?.let { cobro ->
        DialogoConfirmar(
            titulo = "¿Eliminar este cobro?",
            mensaje = "Úsalo solo si lo registraste por error. \"${cobro.motivo}\" a ${cobro.contacto.nombre} " +
                "por ${cobro.monto.formatear()} quedará anotado en tu actividad.",
            textoConfirmar = "Eliminar",
            onConfirmar = viewModel::confirmarEliminar,
            onCancelar = viewModel::cancelarEliminar,
        )
    }
}

@Composable
private fun TarjetaTeDeben(resumen: ResumenCobros) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Te deben", style = MaterialTheme.typography.labelLarge)
            Text(resumen.totalPendiente.formatear(), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            if (resumen.porContacto.isEmpty()) {
                Text("Todos te pagaron. ¡Bien!", style = MaterialTheme.typography.bodyMedium)
            }
            resumen.porContacto.forEach { deuda ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${deuda.contacto.nombre} · ${deuda.cantidad} ${if (deuda.cantidad == 1) "cobro" else "cobros"}",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(deuda.total.formatear(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun FilaCobro(cobro: Cobro, hoy: LocalDate, onCobrar: () -> Unit, onEliminar: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        overlineContent = { Text(cobro.contacto.nombre) },
        headlineContent = { Text(cobro.motivo, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = cobro.monto.formatear(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (cobro.estaPendiente) colores.onSurface else colores.onSurfaceVariant,
                    textDecoration = if (cobro.estaPendiente) null else TextDecoration.LineThrough,
                )
                Text(
                    text = if (cobro.estaPendiente) {
                        "Desde ${cobro.fecha.formatearRelativo(hoy).lowercase()}"
                    } else {
                        "Cobrado ${cobro.fechaCobrado?.formatearRelativo(hoy)?.lowercase().orEmpty()}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (cobro.estaPendiente) {
                    FilledTonalButton(onClick = onCobrar) { Text("Cobrar") }
                } else {
                    Surface(color = colores.primaryContainer, contentColor = colores.onPrimaryContainer, shape = MaterialTheme.shapes.small) {
                        Text("Cobrado", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                }
                IconButton(onClick = onEliminar) {
                    Icon(Iconos.Eliminar, contentDescription = "Eliminar cobro de ${cobro.contacto.nombre}", tint = colores.onSurfaceVariant)
                }
            }
        },
    )
}
