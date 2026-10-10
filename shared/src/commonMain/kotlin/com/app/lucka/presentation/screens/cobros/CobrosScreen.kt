package com.app.lucka.presentation.screens.cobros

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.lucka.domain.model.Cobro
import com.app.lucka.domain.model.Monto
import com.app.lucka.domain.model.ResumenCobros
import com.app.lucka.presentation.components.DialogoConfirmar
import com.app.lucka.presentation.components.EstadoVacio
import com.app.lucka.presentation.components.Iconos
import com.app.lucka.presentation.components.formatear
import com.app.lucka.presentation.components.formatearRelativo
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CobrosScreen(
    onRegistrarCobro: () -> Unit,
    onEditarCobro: (String) -> Unit,
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
                mensaje = "Anota aquí lo que fías o prestas y los pedidos por cobrar. Marca \"Cobrar\" cuando te paguen, " +
                    "o recuérdaselo por WhatsApp. Así no se te olvida nadie.",
                modifier = Modifier.padding(padding),
            ) {
                Button(onClick = onRegistrarCobro, modifier = Modifier.padding(top = 8.dp)) { Text("Registrar un cobro") }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            estado.resumen?.let { resumen -> item { TarjetaTeDeben(resumen) } }

            item { Text("Fiados, préstamos y pedidos", style = MaterialTheme.typography.titleMedium) }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column {
                        estado.cobros.forEachIndexed { i, cobro ->
                            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            FilaCobro(
                                cobro = cobro,
                                hoy = estado.hoy,
                                onCobrar = { viewModel.cobrar(cobro) },
                                onRecordar = { viewModel.recordar(cobro) },
                                onAbonar = { viewModel.pedirAbono(cobro) },
                                onEditar = { onEditarCobro(cobro.id) },
                                onEliminar = { viewModel.pedirEliminar(cobro) },
                            )
                        }
                    }
                }
            }
        }
    }

    estado.abono?.let { dialogo ->
        AlertDialog(
            onDismissRequest = viewModel::cancelarAbono,
            title = { Text("Abono de ${dialogo.cobro.contacto.nombre}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Le falta pagar ${dialogo.cobro.saldo.formatear()} por \"${dialogo.cobro.motivo}\".")
                    OutlinedTextField(
                        value = dialogo.montoTexto,
                        onValueChange = viewModel::onMontoAbono,
                        label = { Text("¿Cuánto te pagó?") },
                        prefix = { Text("S/ ") },
                        isError = dialogo.error != null,
                        supportingText = dialogo.error?.let { { Text(it) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = { TextButton(onClick = viewModel::confirmarAbono) { Text("Registrar abono") } },
            dismissButton = { TextButton(onClick = viewModel::cancelarAbono) { Text("Cancelar") } },
        )
    }

    estado.porEliminar?.let { cobro ->
        DialogoConfirmar(
            titulo = "¿Eliminar este cobro?",
            mensaje = "Úsalo solo si lo registraste por error. \"${cobro.motivo}\" a ${cobro.contacto.nombre} " +
                "por ${cobro.saldo.formatear()} quedará anotado en tu actividad.",
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

/**
 * Un cobro con sus datos arriba y todas sus acciones en una fila de ancho completo debajo,
 * para que no se apilen ni se corte el motivo con texto grande.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilaCobro(
    cobro: Cobro,
    hoy: LocalDate,
    onCobrar: () -> Unit,
    onRecordar: () -> Unit,
    onAbonar: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
) {
    val colores = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "${cobro.contacto.nombre} · ${cobro.tipo.etiqueta}",
                    style = MaterialTheme.typography.labelMedium,
                    color = colores.onSurfaceVariant,
                )
                Text(cobro.motivo, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            // Pendiente: lo que falta. Cobrado: el total que se pagó.
            Text(
                text = (if (cobro.estaPendiente) cobro.saldo else cobro.monto).formatear(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (cobro.estaPendiente) colores.onSurface else colores.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        if (cobro.pagado > Monto.CERO) {
            Text(
                text = buildString {
                    append("Total ${cobro.monto.formatear()}")
                    if (cobro.adelanto > Monto.CERO) append(" · adelantó ${cobro.adelanto.formatear()}")
                    if (cobro.abonado > Monto.CERO) append(" · abonó ${cobro.abonado.formatear()}")
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            text = if (cobro.estaPendiente) {
                "Desde ${cobro.fecha.formatearRelativo(hoy).lowercase()}"
            } else {
                "Cobrado ${cobro.fechaCobrado?.formatearRelativo(hoy)?.lowercase().orEmpty()}"
            },
            style = MaterialTheme.typography.bodySmall,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            if (cobro.estaPendiente) {
                FilledTonalButton(onClick = onCobrar) { Text("Cobrar") }
                OutlinedButton(onClick = onAbonar) { Text("Abonar") }
                TextButton(onClick = onEditar) {
                    Icon(Iconos.Editar, contentDescription = null)
                    Text("Editar", modifier = Modifier.padding(start = 6.dp))
                }
                if (cobro.contacto.telefono != null) {
                    TextButton(onClick = onRecordar) {
                        Icon(Iconos.Mensaje, contentDescription = null)
                        Text("WhatsApp", modifier = Modifier.padding(start = 6.dp))
                    }
                }
            } else {
                Surface(
                    color = colores.primaryContainer,
                    contentColor = colores.onPrimaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.align(Alignment.CenterVertically),
                ) {
                    Text("Cobrado", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                }
            }
            IconButton(onClick = onEliminar) {
                Icon(Iconos.Eliminar, contentDescription = "Eliminar cobro de ${cobro.contacto.nombre}", tint = colores.onSurfaceVariant)
            }
        }
    }
}
