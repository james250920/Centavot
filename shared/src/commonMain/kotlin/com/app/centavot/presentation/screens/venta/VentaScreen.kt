package com.app.centavot.presentation.screens.venta

import androidx.compose.ui.semantics.stateDescription
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.layout.heightIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.centavot.domain.model.Categoria
import com.app.centavot.domain.model.Monto
import com.app.centavot.presentation.components.CampoFecha
import com.app.centavot.presentation.components.DialogoConfirmar
import com.app.centavot.presentation.components.Iconos
import com.app.centavot.presentation.components.formatear
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VentaScreen(
    id: String?,
    onCerrar: () -> Unit,
    viewModel: VentaViewModel = koinViewModel { parametersOf(id) },
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(estado.terminado) {
        if (estado.terminado) onCerrar()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (estado.esEdicion) "Editar venta" else "Registrar venta") },
                navigationIcon = {
                    IconButton(onClick = onCerrar) { Icon(Iconos.Atras, contentDescription = "Volver") }
                },
                actions = {
                    if (estado.esEdicion) {
                        IconButton(onClick = viewModel::pedirEliminar) {
                            Icon(Iconos.Eliminar, contentDescription = "Eliminar venta")
                        }
                    } else if (estado.ventasEnEstaSesion > 0) {
                        TextButton(onClick = onCerrar) { Text("Listo") }
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
                Text(if (estado.esEdicion) "Guardar cambios" else "Guardar venta")
            }
        },
    ) { padding ->
        if (estado.cargando) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            estado.ultimaGuardada?.let { venta ->
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Iconos.Correcto, contentDescription = null)
                        Text(
                            text = "Guardaste ${venta.monto.formatear()}${venta.descripcion?.let { " ($it)" }.orEmpty()}. " +
                                "Ya puedes anotar la siguiente.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            if (!estado.esEdicion && estado.frecuentes.isNotEmpty()) {
                Seccion("Tus ventas frecuentes (un toque y listo)") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        estado.frecuentes.forEach { frecuente ->
                            FilledTonalButton(
                                onClick = { viewModel.registrarFrecuente(frecuente) },
                                enabled = !estado.guardando,
                                modifier = Modifier.height(48.dp),
                            ) {
                                Text("${frecuente.descripcion} · ${frecuente.monto.formatear()}")
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = estado.montoTexto,
                onValueChange = viewModel::onMontoCambiado,
                label = { Text("¿Cuánto vendiste?") },
                prefix = { Text("S/ ") },
                placeholder = { Text("0.00") },
                textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                isError = estado.errorMonto != null,
                supportingText = estado.errorMonto?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { viewModel.guardar() }),
                modifier = Modifier.fillMaxWidth(),
            )

            if (!estado.esEdicion) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MONTOS_RAPIDOS.forEach { soles ->
                        OutlinedButton(onClick = { viewModel.onMontoRapido(soles) }, modifier = Modifier.height(48.dp)) {
                            Text(Monto.soles(soles).formatear().removeSuffix(".00"))
                        }
                    }
                }
            }

            // Lo habitual (venta del negocio, hoy, sin nombre) no se pregunta: queda plegado con su resumen.
            var detallesAbiertos by rememberSaveable(estado.esEdicion) { mutableStateOf(estado.detallesAbiertosAlInicio()) }
            LaunchedEffect(estado.errorFecha) { if (estado.errorFecha != null) detallesAbiertos = true }
            DetallesPlegables(
                abiertos = detallesAbiertos,
                resumen = resumenDetallesVenta(estado.categoria, estado.fecha, estado.hoy, estado.descripcion),
                onCambiar = { detallesAbiertos = !detallesAbiertos },
            ) {
                OutlinedTextField(
                    value = estado.descripcion,
                    onValueChange = viewModel::onDescripcionCambiada,
                    label = { Text("¿Qué vendiste? (opcional)") },
                    placeholder = { Text("Ej. gaseosa, menú, arreglo de zapatos") },
                    supportingText = { Text("Con nombre, la próxima vez aparece arriba para un toque.") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )

                Seccion("¿De dónde vino la plata?") {
                    val opciones = listOf(Categoria.NEGOCIO to "Venta del negocio", Categoria.PERSONAL to "Ingreso personal")
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        opciones.forEachIndexed { i, (categoria, etiqueta) ->
                            SegmentedButton(
                                selected = estado.categoria == categoria,
                                onClick = { viewModel.onCategoriaElegida(categoria) },
                                shape = SegmentedButtonDefaults.itemShape(index = i, count = opciones.size),
                            ) { Text(etiqueta) }
                        }
                    }
                    Text(
                        text = "Ingreso personal: un sueldo, un regalo o plata que no es del negocio. No cuenta para tu tope.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Seccion("Fecha") {
                    CampoFecha(estado.fecha, estado.hoy, viewModel::onFechaElegida, estado.errorFecha)
                }
            }
        }
    }

    if (estado.confirmandoEliminar) {
        DialogoConfirmar(
            titulo = "¿Eliminar esta venta?",
            mensaje = "Úsalo solo si la registraste por error. Ya no se contará en tus totales ni en tu tope, y quedará anotado en tu actividad.",
            textoConfirmar = "Eliminar",
            onConfirmar = viewModel::confirmarEliminar,
            onCancelar = viewModel::cancelarEliminar,
        )
    }
}

/** Fila que muestra lo que se guardará y, al tocarla, abre los campos para cambiarlo. */
@Composable
private fun DetallesPlegables(
    abiertos: Boolean,
    resumen: String,
    onCambiar: () -> Unit,
    contenido: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            onClick = onCambiar,
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth().semantics {
                stateDescription = if (abiertos) "Abierto" else "Cerrado"
            },
        ) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(if (abiertos) "Menos detalles" else "Cambiar detalles", style = MaterialTheme.typography.labelLarge)
                    Text(resumen, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(if (abiertos) Iconos.Plegar else Iconos.Desplegar, contentDescription = null)
            }
        }
        AnimatedVisibility(visible = abiertos) {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) { contenido() }
        }
    }
}

@Composable
private fun Seccion(titulo: String, contenido: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleSmall)
        contenido()
    }
}
