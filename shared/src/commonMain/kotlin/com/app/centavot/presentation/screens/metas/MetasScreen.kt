package com.app.centavot.presentation.screens.metas

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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.centavot.domain.model.Meta
import com.app.centavot.domain.model.Modo
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.ProgresoMeta
import com.app.centavot.presentation.components.BarraAvance
import com.app.centavot.presentation.components.EstadoVacio
import com.app.centavot.presentation.components.Iconos
import com.app.centavot.presentation.components.comoTextoEditable
import com.app.centavot.presentation.components.esEntradaDeMontoValida
import com.app.centavot.presentation.components.formatear
import com.app.centavot.presentation.components.formatearRelativo
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel

/** Metas de ahorro: cuánto llevas, cuánto falta y cuánto separar por semana para llegar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetasScreen(onCerrar: () -> Unit, viewModel: MetasViewModel = koinViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val dialogo by viewModel.dialogo.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tus metas") },
                navigationIcon = { IconButton(onClick = onCerrar) { Icon(Iconos.Atras, contentDescription = "Volver") } },
            )
        },
    ) { padding ->
        if (estado.cargando) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (estado.metas.isEmpty()) {
                item {
                    EstadoVacio(
                        titulo = "Ponte una meta",
                        mensaje = if (estado.modo == Modo.PERSONAL) "Un viaje, tus estudios, un fondo para emergencias." else "Una moto para el delivery, una refrigeradora, el local propio.",
                    ) { Button(onClick = viewModel::nueva) { Text("Nueva meta") } }
                }
                return@LazyColumn
            }
            items(estado.metas, key = { it.meta.id }) { progreso ->
                TarjetaMeta(progreso, estado.hoy, onAbonar = { viewModel.abonar(progreso.meta) }, onEditar = { viewModel.editar(progreso.meta) })
            }
            item {
                OutlinedButton(onClick = viewModel::nueva, modifier = Modifier.fillMaxWidth()) {
                    Icon(Iconos.Agregar, contentDescription = null)
                    Text("Nueva meta", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }

    when (val actual = dialogo) {
        is DialogoMeta.Editar -> DialogoEditarMeta(
            meta = actual.meta,
            hoy = estado.hoy,
            error = error,
            onGuardar = { nombre, monto, fecha -> viewModel.guardar(actual.meta, nombre, monto, fecha) },
            onQuitar = actual.meta?.let { meta -> { viewModel.quitar(meta) } },
            onCancelar = viewModel::cerrar,
        )
        is DialogoMeta.Abonar -> DialogoAbonar(actual.meta, error, onAbonar = { viewModel.confirmarAbono(actual.meta, it) }, onCancelar = viewModel::cerrar)
        null -> Unit
    }
}

@Composable
private fun TarjetaMeta(progreso: ProgresoMeta, hoy: LocalDate, onAbonar: () -> Unit, onEditar: () -> Unit) {
    val meta = progreso.meta
    var menu by remember { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(meta.nombre, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Iconos.MasOpciones, contentDescription = "Más opciones de la meta ${meta.nombre}") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Editar") },
                            leadingIcon = { Icon(Iconos.Editar, contentDescription = null) },
                            onClick = { menu = false; onEditar() },
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth()) {
                Text(
                    "${progreso.ahorrado.formatear()} de ${meta.objetivo.formatear()}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text("${progreso.porcentaje} %", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            }
            BarraAvance(progreso.porcentaje, MaterialTheme.colorScheme.primary, "${meta.nombre}: ${progreso.porcentaje} por ciento")
            val nota = when {
                progreso.cumplida -> "¡La cumpliste!"
                progreso.porSemana(hoy) != null && meta.fechaLimite != null ->
                    "${progreso.porSemana(hoy)!!.formatear()} por semana hasta el ${meta.fechaLimite.formatearRelativo(hoy).lowercase()}"
                else -> "Faltan ${progreso.falta.formatear()}"
            }
            Text(nota, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!progreso.cumplida) {
                FilledTonalButton(onClick = onAbonar) { Text("Separar plata") }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DialogoEditarMeta(
    meta: Meta?,
    hoy: LocalDate,
    error: String?,
    onGuardar: (String, String, LocalDate?) -> Unit,
    onQuitar: (() -> Unit)?,
    onCancelar: () -> Unit,
) {
    var nombre by remember { mutableStateOf(meta?.nombre.orEmpty()) }
    var monto by remember { mutableStateOf(meta?.objetivo?.comoTextoEditable().orEmpty()) }
    var fecha by remember { mutableStateOf(meta?.fechaLimite) }
    var confirmarQuitar by remember { mutableStateOf(false) }
    if (confirmarQuitar && onQuitar != null) {
        AlertDialog(
            onDismissRequest = { confirmarQuitar = false },
            title = { Text("¿Quitar esta meta?") },
            text = { Text("Se borra con lo que separaste en ella.") },
            confirmButton = {
                TextButton(onClick = onQuitar, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Quitar") }
            },
            dismissButton = { TextButton(onClick = { confirmarQuitar = false }) { Text("Cancelar") } },
        )
        return
    }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(if (meta == null) "Nueva meta" else "Editar meta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it.take(40) },
                    label = { Text("¿Para qué?") },
                    placeholder = { Text("Ej. viaje a Cusco") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                OutlinedTextField(
                    value = monto,
                    onValueChange = { if (esEntradaDeMontoValida(it)) monto = it },
                    label = { Text("¿Cuánto quieres juntar?") },
                    prefix = { Text("S/ ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Text("¿Para cuándo?", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Plazo.entries.forEach { plazo ->
                        val valor = plazo.fecha(hoy)
                        FilterChip(selected = fecha == valor, onClick = { fecha = valor }, label = { Text(plazo.etiqueta) })
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                if (onQuitar != null) {
                    TextButton(
                        onClick = { confirmarQuitar = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text("Quitar meta") }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onGuardar(nombre, monto, fecha) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DialogoAbonar(meta: Meta, error: String?, onAbonar: (String) -> Unit, onCancelar: () -> Unit) {
    var monto by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Separar para \"${meta.nombre}\"") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = monto,
                    onValueChange = { if (esEntradaDeMontoValida(it)) monto = it },
                    label = { Text("¿Cuánto separaste?") },
                    prefix = { Text("S/ ") },
                    isError = error != null,
                    supportingText = { Text(error ?: "No cuenta como gasto.") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(10L, 20L, 50L, 100L).forEach { soles ->
                        OutlinedButton(onClick = { monto = soles.toString() }) { Text(Monto.soles(soles).formatear().removeSuffix(".00")) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onAbonar(monto) }) { Text("Separar") } },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    )
}
