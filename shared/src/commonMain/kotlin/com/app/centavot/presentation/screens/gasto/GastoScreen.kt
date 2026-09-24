package com.app.centavot.presentation.screens.gasto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.centavot.presentation.components.Iconos
import com.app.centavot.presentation.components.SelectorCategoria
import com.app.centavot.presentation.components.formatearRelativo
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val MILIS_POR_DIA = 86_400_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GastoScreen(
    id: String?,
    onCerrar: () -> Unit,
    viewModel: GastoViewModel = koinViewModel { parametersOf(id) },
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var mostrarCalendario by remember { mutableStateOf(false) }

    LaunchedEffect(estado.terminado) {
        if (estado.terminado) onCerrar()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (estado.esEdicion) "Editar gasto" else "Nuevo gasto") },
                navigationIcon = {
                    IconButton(onClick = onCerrar) { Icon(Iconos.Atras, contentDescription = "Volver") }
                },
                actions = {
                    if (estado.esEdicion) {
                        IconButton(onClick = viewModel::pedirEliminar) {
                            Icon(Iconos.Eliminar, contentDescription = "Eliminar gasto")
                        }
                    }
                },
            )
        },
        bottomBar = {
            Button(
                onClick = viewModel::guardar,
                enabled = !estado.guardando && !estado.cargando,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(16.dp)
                    .height(56.dp),
            ) {
                Text(if (estado.esEdicion) "Guardar cambios" else "Guardar gasto")
            }
        },
    ) { padding ->
        if (estado.cargando) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val enfocarMonto = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            if (!estado.esEdicion) enfocarMonto.requestFocus()
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            OutlinedTextField(
                value = estado.montoTexto,
                onValueChange = viewModel::onMontoCambiado,
                label = { Text("Monto") },
                prefix = { Text("S/ ") },
                placeholder = { Text("0.00") },
                textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                isError = estado.errorMonto != null,
                supportingText = estado.errorMonto?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth().focusRequester(enfocarMonto),
            )

            Seccion(titulo = "¿Para qué fue?") {
                SelectorCategoria(estado.categoria, viewModel::onCategoriaElegida)
                Text(
                    text = estado.errorCategoria
                        ?: "Negocio: mercadería, alquiler del puesto, pasajes de trabajo. Personal: casa, comida, familia.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (estado.errorCategoria != null) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedTextField(
                value = estado.descripcion,
                onValueChange = viewModel::onDescripcionCambiada,
                label = { Text("Descripción (opcional)") },
                placeholder = { Text("Ej. mercadería, pasaje, luz") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            val ayer = estado.hoy.minus(1, DateTimeUnit.DAY)
            val esOtraFecha = estado.fecha != estado.hoy && estado.fecha != ayer
            Seccion(titulo = "Fecha") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = estado.fecha == estado.hoy,
                        onClick = { viewModel.onFechaElegida(estado.hoy) },
                        label = { Text("Hoy") },
                    )
                    FilterChip(
                        selected = estado.fecha == ayer,
                        onClick = { viewModel.onFechaElegida(ayer) },
                        label = { Text("Ayer") },
                    )
                    FilterChip(
                        selected = esOtraFecha,
                        onClick = { mostrarCalendario = true },
                        label = { Text(if (esOtraFecha) estado.fecha.formatearRelativo(estado.hoy) else "Otra fecha") },
                    )
                }
                estado.errorFecha?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (mostrarCalendario) {
        SelectorFecha(
            inicial = estado.fecha,
            hoy = estado.hoy,
            onElegir = {
                viewModel.onFechaElegida(it)
                mostrarCalendario = false
            },
            onCancelar = { mostrarCalendario = false },
        )
    }

    if (estado.confirmandoEliminar) {
        AlertDialog(
            onDismissRequest = viewModel::cancelarEliminar,
            title = { Text("¿Eliminar este gasto?") },
            text = { Text("Ya no se contará en tu tope ni en tus reportes. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmarEliminar,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelarEliminar) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun Seccion(titulo: String, contenido: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleSmall)
        contenido()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorFecha(
    inicial: LocalDate,
    hoy: LocalDate,
    onElegir: (LocalDate) -> Unit,
    onCancelar: () -> Unit,
) {
    val limite = hoy.toEpochDays() * MILIS_POR_DIA
    val estadoCalendario = rememberDatePickerState(
        initialSelectedDateMillis = inicial.toEpochDays() * MILIS_POR_DIA,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= limite
        },
    )
    DatePickerDialog(
        onDismissRequest = onCancelar,
        confirmButton = {
            TextButton(
                onClick = {
                    estadoCalendario.selectedDateMillis?.let { onElegir(LocalDate.fromEpochDays(it / MILIS_POR_DIA)) }
                },
            ) { Text("Aceptar") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    ) {
        DatePicker(state = estadoCalendario)
    }
}
