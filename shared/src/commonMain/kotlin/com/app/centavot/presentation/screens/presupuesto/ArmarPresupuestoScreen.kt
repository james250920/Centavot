package com.app.centavot.presentation.screens.presupuesto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.centavot.domain.model.Modo
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.TipoGasto
import com.app.centavot.presentation.components.Iconos
import com.app.centavot.presentation.components.formatearCorto
import org.koin.compose.viewmodel.koinViewModel

/** "Arma tu presupuesto": la plantilla con checks, un bloque por grupo y el balance abajo. */
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
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        estado.balance?.let { LineaBalance(it) }
                        Button(
                            onClick = viewModel::guardar,
                            enabled = !estado.guardando && estado.marcados > 0,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                        ) { Text(if (estado.marcados == 0) "Marca al menos una" else "Guardar presupuesto (${estado.marcados})") }
                    }
                }
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // El orden de la guía (documento 04 §7): lo que entra, el ahorro primero, y luego los gastos.
            gruposArmar(estado.modo).forEach { (grupo, titulo) ->
                val renglones = estado.renglones.filter { it.grupo == grupo }
                if (renglones.isEmpty()) return@forEach
                item(key = titulo) {
                    val total = estado.totalDe(grupo)
                    BloqueGrupo(
                        titulo = titulo,
                        total = total.takeIf { it > Monto.CERO }?.formatearCorto(),
                        nota = if (grupo == TipoGasto.AHORRO) estado.ahorroSugerido?.let { "Sugerido: ${it.formatearCorto()} (10 %)" } else null,
                    ) {
                        renglones.forEachIndexed { indice, renglon ->
                            if (indice > 0) SeparadorFila()
                            FilaRenglon(renglon, viewModel)
                        }
                        SeparadorFila()
                        TextButton(onClick = { viewModel.pedirOtra(grupo) }) {
                            Icon(Iconos.Agregar, contentDescription = null)
                            Text("Agregar", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        }
    }

    if (estado.agregandoOtra) {
        val titulo = gruposArmar(estado.modo).first { it.first == estado.grupoOtra }.second
        DialogoOtra(
            titulo = titulo,
            esIngreso = estado.grupoOtra == null,
            onAgregar = { viewModel.agregarOtra(it, estado.grupoOtra) },
            onCancelar = viewModel::cancelarOtra,
        )
    }
}

/** Los grupos en el orden de la guía; null = lo que entra. */
private fun gruposArmar(modo: Modo): List<Pair<TipoGasto?, String>> = listOf(
    null to if (modo == Modo.PERSONAL) "Lo que te entra" else "Cuánto esperas vender",
    TipoGasto.AHORRO to "Tu ahorro, primero",
    TipoGasto.FIJO to TipoGasto.FIJO.etiqueta,
    TipoGasto.VARIABLE to TipoGasto.VARIABLE.etiqueta,
    TipoGasto.PRESCINDIBLE to TipoGasto.PRESCINDIBLE.etiqueta,
)

/** Toda la fila marca y desmarca; el monto va a la derecha, como en un cuaderno. */
@Composable
private fun FilaRenglon(renglon: Renglon, viewModel: ArmarPresupuestoViewModel) {
    val nombre = if (renglon.item.semanal) "${renglon.item.nombre} (por semana)" else renglon.item.nombre
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .toggleable(value = renglon.marcado, role = Role.Checkbox) { viewModel.onMarcar(renglon.item, it) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = renglon.marcado, onCheckedChange = null)
            Text(
                nombre,
                style = MaterialTheme.typography.bodyLarge,
                color = if (renglon.marcado) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        TextField(
            value = renglon.montoTexto,
            onValueChange = { viewModel.onMonto(renglon.item, it) },
            prefix = { Text("S/ ") },
            placeholder = { Text("0", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End, fontWeight = FontWeight.SemiBold),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            // Un fondo suave en vez de línea: la fila ya tiene su separador.
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier
                .width(120.dp)
                .padding(vertical = 4.dp)
                .semantics { contentDescription = "Monto de ${renglon.item.nombre}" },
        )
    }
}

/** RP-03: si alcanza. Positivo: asignarlo a ahorro o metas; negativo: recortar prescindibles. */
@Composable
private fun LineaBalance(balance: Monto) {
    val colores = MaterialTheme.colorScheme
    val (texto, color) = when {
        balance > Monto.CERO -> "Te sobran ${balance.formatearCorto()} · pásalos a tu ahorro" to colores.primary
        balance < Monto.CERO -> "Te faltan ${Monto(-balance.centimos).formatearCorto()} · recorta prescindibles" to colores.error
        else -> "Cada sol tiene destino" to colores.primary
    }
    Text(texto, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = color)
}

@Composable
private fun DialogoOtra(titulo: String, esIngreso: Boolean, onAgregar: (String) -> Unit, onCancelar: () -> Unit) {
    var nombre by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Agregar en $titulo") },
        text = {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it.take(40) },
                label = { Text("Nombre") },
                placeholder = { Text(if (esIngreso) "Ej. clases de inglés" else "Ej. pensión de inglés") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
        },
        confirmButton = { TextButton(onClick = { onAgregar(nombre) }, enabled = nombre.isNotBlank()) { Text("Agregar") } },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    )
}
