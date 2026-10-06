package com.app.centavot.presentation.screens.gasto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.remember
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
import com.app.centavot.domain.model.SubcategoriaGasto
import com.app.centavot.domain.model.para
import com.app.centavot.presentation.components.CampoFecha
import com.app.centavot.presentation.components.Iconos
import com.app.centavot.presentation.components.SelectorChips
import com.app.centavot.presentation.components.SelectorCategoria
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GastoScreen(
    id: String?,
    onCerrar: () -> Unit,
    viewModel: GastoViewModel = koinViewModel { parametersOf(id) },
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

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

            Seccion(titulo = "¿En qué? (opcional)") {
                SelectorChips(
                    opciones = SubcategoriaGasto.para(estado.categoria),
                    seleccionada = estado.subcategoria,
                    etiqueta = { it.etiqueta },
                    onSeleccionar = viewModel::onSubcategoriaElegida,
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

            Seccion(titulo = "Fecha") {
                CampoFecha(estado.fecha, estado.hoy, viewModel::onFechaElegida, estado.errorFecha)
            }
        }
    }

    if (estado.confirmandoEliminar) {
        AlertDialog(
            onDismissRequest = viewModel::cancelarEliminar,
            title = { Text("¿Eliminar este gasto?") },
            text = { Text("Úsalo solo si lo registraste por error. Ya no se contará en tu tope ni en tus reportes, y quedará anotado en tu actividad.") },
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
