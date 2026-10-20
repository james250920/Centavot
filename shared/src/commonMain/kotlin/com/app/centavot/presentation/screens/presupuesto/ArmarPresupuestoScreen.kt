package com.app.centavot.presentation.screens.presupuesto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.centavot.domain.model.Modo
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.TipoGasto
import com.app.centavot.domain.model.TipoPartida
import com.app.centavot.presentation.components.Iconos
import com.app.centavot.presentation.components.formatear
import org.koin.compose.viewmodel.koinViewModel

/** "Arma tu presupuesto": la plantilla con checks, sin tener que escribir. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArmarPresupuestoScreen(
    onCerrar: () -> Unit,
    onListo: () -> Unit,
    viewModel: ArmarPresupuestoViewModel = koinViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(estado.terminado) { if (estado.terminado) onListo() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Arma tu presupuesto") },
                navigationIcon = { IconButton(onClick = onCerrar) { Icon(Iconos.Atras, contentDescription = "Volver") } },
            )
        },
        bottomBar = {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                estado.balance?.let { LineaBalance(it) }
                Button(
                    onClick = viewModel::guardar,
                    enabled = !estado.guardando && estado.marcados > 0,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) { Text(if (estado.marcados == 0) "Marca al menos una" else "Guardar presupuesto (${estado.marcados})") }
            }
        },
    ) { padding ->
        if (estado.cargando) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                Text(
                    "Marca lo que tienes cada mes. El monto es opcional y lo cambias cuando quieras.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            // El orden de la guía (documento 04 §7): lo que entra, el ahorro primero, y luego los gastos.
            grupo(if (estado.modo == Modo.PERSONAL) "Lo que te entra" else "Cuánto esperas vender", estado.renglones.filter { it.grupo == null }, null, viewModel)
            grupo(
                titulo = "Tu ahorro, primero",
                renglones = estado.renglones.filter { it.grupo == TipoGasto.AHORRO },
                sugerencia = estado.ahorroSugerido?.let { "Sugerido: ${it.formatear()} (10 % de lo que entra)" },
                viewModel = viewModel,
            )
            listOf(TipoGasto.FIJO, TipoGasto.VARIABLE, TipoGasto.PRESCINDIBLE).forEach { tipo ->
                grupo(tipo.etiqueta, estado.renglones.filter { it.grupo == tipo }, null, viewModel)
            }
            item {
                TextButton(onClick = viewModel::pedirOtra) {
                    Icon(Iconos.Agregar, contentDescription = null)
                    Text("Agregar otra", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }

    if (estado.agregandoOtra) {
        DialogoOtra(onAgregar = viewModel::agregarOtra, onCancelar = viewModel::cancelarOtra)
    }
}

private fun LazyListScope.grupo(titulo: String, renglones: List<Renglon>, sugerencia: String?, viewModel: ArmarPresupuestoViewModel) {
    if (renglones.isEmpty()) return
    item {
        Column(Modifier.padding(top = 12.dp, bottom = 4.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            sugerencia?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
    items(renglones, key = { it.item.nombre }) { renglon ->
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
            // Toda la fila marca y desmarca; el monto va aparte.
            Row(
                Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .toggleable(value = renglon.marcado, role = Role.Checkbox) { viewModel.onMarcar(renglon.item, it) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = renglon.marcado, onCheckedChange = null)
                Text(
                    if (renglon.item.semanal) "${renglon.item.nombre} (por semana)" else renglon.item.nombre,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            OutlinedTextField(
                value = renglon.montoTexto,
                onValueChange = { viewModel.onMonto(renglon.item, it) },
                prefix = { Text("S/ ") },
                placeholder = { Text("0") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                modifier = Modifier.width(124.dp),
            )
        }
    }
}

/** RP-03: si alcanza. Positivo: asignarlo a ahorro o metas; negativo: recortar prescindibles. */
@Composable
private fun LineaBalance(balance: Monto) {
    val colores = MaterialTheme.colorScheme
    val (texto, color) = when {
        balance > Monto.CERO -> "Te sobran ${balance.formatear()}: pásalos a tu ahorro." to colores.primary
        balance < Monto.CERO -> "Te faltan ${Monto(-balance.centimos).formatear()}: recorta prescindibles." to colores.error
        else -> "Cada sol tiene destino." to colores.primary
    }
    Text(texto, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = color)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DialogoOtra(onAgregar: (String, TipoGasto?) -> Unit, onCancelar: () -> Unit) {
    var nombre by remember { mutableStateOf("") }
    // null = ingreso.
    var grupo by remember { mutableStateOf<TipoGasto?>(TipoGasto.PRESCINDIBLE) }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Agregar otra") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (listOf<TipoGasto?>(null) + TipoGasto.entries).forEach { opcion ->
                        FilterChip(
                            selected = grupo == opcion,
                            onClick = { grupo = opcion },
                            label = { Text(opcion?.etiqueta ?: "Ingreso") },
                        )
                    }
                }
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it.take(40) },
                    label = { Text("Nombre") },
                    placeholder = { Text(if (grupo == null) "Ej. clases de inglés" else "Ej. pensión de inglés") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onAgregar(nombre, grupo) }, enabled = nombre.isNotBlank()) { Text("Agregar") } },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    )
}
