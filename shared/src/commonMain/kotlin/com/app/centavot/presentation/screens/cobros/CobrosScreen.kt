package com.app.centavot.presentation.screens.cobros

import androidx.compose.foundation.layout.height
import androidx.compose.material3.FloatingActionButton
import androidx.compose.animation.scaleOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.app.centavot.domain.model.Cobro
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.ResumenCobros
import com.app.centavot.presentation.components.DialogoConfirmar
import com.app.centavot.presentation.components.EstadoVacio
import com.app.centavot.presentation.components.Iconos
import com.app.centavot.presentation.components.formatear
import com.app.centavot.presentation.components.formatearRelativo
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel

/** Relleno lateral más corto para que Cobrar, Abonar y WhatsApp quepan en una fila. */
private val RellenoAccion = PaddingValues(horizontal = 16.dp, vertical = 8.dp)

/** Posición del botón "Registrar cobro" en la lista (después de "Te deben"). */
private const val INDICE_BOTON_REGISTRAR = 1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CobrosScreen(
    onRegistrarCobro: () -> Unit,
    onEditarCobro: (String) -> Unit,
    onAbrirContactos: () -> Unit,
    viewModel: CobrosViewModel = koinViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val lista = rememberLazyListState()
    // "Registrar cobro" vive en la lista, con su texto; el "+" flotante solo aparece cuando ese botón ya no se ve.
    val mostrarBotonFlotante by remember {
        derivedStateOf {
            val info = lista.layoutInfo
            val boton = info.visibleItemsInfo.firstOrNull { it.index == INDICE_BOTON_REGISTRAR }
            // Cuenta como oculto si ya salió de la vista o le queda menos de la mitad a la vista.
            info.visibleItemsInfo.isNotEmpty() &&
                (boton == null || boton.offset + boton.size / 2 < info.viewportStartOffset)
        }
    }

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
            AnimatedVisibility(visible = estado.cobros.isNotEmpty() && mostrarBotonFlotante, enter = scaleIn(), exit = scaleOut()) {
                FloatingActionButton(onClick = onRegistrarCobro) {
                    Icon(Iconos.Agregar, contentDescription = "Registrar cobro")
                }
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
            state = lista,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { estado.resumen?.let { resumen -> TarjetaTeDeben(resumen) } }
            item {
                FilledTonalButton(onClick = onRegistrarCobro, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Icon(Iconos.Agregar, contentDescription = null)
                    Text("Registrar cobro", modifier = Modifier.padding(start = 8.dp))
                }
            }

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
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
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
 * Un cobro con sus datos arriba y, debajo, solo las acciones del día a día: Cobrar, Abonar y
 * recordar por WhatsApp. Editar y Eliminar van en el menú ⋮, lejos de las otras para no tocarlas sin querer.
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
                modifier = Modifier.padding(start = 12.dp, top = 12.dp),
            )
            MenuCobro(
                nombre = cobro.contacto.nombre,
                onEditar = onEditar.takeIf { cobro.estaPendiente },
                onEliminar = onEliminar,
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
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (cobro.estaPendiente) {
                FilledTonalButton(onClick = onCobrar, contentPadding = RellenoAccion) { Text("Cobrar") }
                OutlinedButton(onClick = onAbonar, contentPadding = RellenoAccion) { Text("Abonar") }
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
        }
    }
}

/** Acciones que se usan poco o no tienen vuelta atrás, con su nombre escrito. */
@Composable
private fun MenuCobro(nombre: String, onEditar: (() -> Unit)?, onEliminar: () -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { abierto = true }) {
            Icon(Iconos.MasOpciones, contentDescription = "Más opciones del cobro de $nombre")
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            if (onEditar != null) {
                DropdownMenuItem(
                    text = { Text("Editar") },
                    leadingIcon = { Icon(Iconos.Editar, contentDescription = null) },
                    onClick = { abierto = false; onEditar() },
                )
            }
            DropdownMenuItem(
                text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Iconos.Eliminar, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                onClick = { abierto = false; onEliminar() },
            )
        }
    }
}
