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
import com.app.centavot.domain.model.TipoEntrada
import com.app.centavot.domain.model.Monto
import com.app.centavot.presentation.components.CampoFecha
import com.app.centavot.presentation.components.DetallesPlegables
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
    retiro: Boolean = false,
    viewModel: VentaViewModel = koinViewModel { parametersOf(id, retiro) },
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val textos = textosDe(estado.tipo)

    LaunchedEffect(estado.terminado) {
        if (estado.terminado) onCerrar()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (estado.esEdicion) textos.tituloEditar else textos.tituloNuevo) },
                navigationIcon = {
                    IconButton(onClick = onCerrar) { Icon(Iconos.Atras, contentDescription = "Volver") }
                },
                actions = {
                    if (estado.esEdicion) {
                        IconButton(onClick = viewModel::pedirEliminar) {
                            Icon(Iconos.Eliminar, contentDescription = "Eliminar ${estado.tipo.nombre}")
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
                Text(if (estado.esEdicion) "Guardar cambios" else textos.guardar)
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
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = viewModel::deshacerUltima) { Text("Deshacer") }
                    }
                }
            }
            estado.ventaQuitada?.let { monto ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                ) {
                    Text(
                        "Quitaste ${textos.articulo} ${estado.tipo.nombre} de ${monto.formatear()}.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    )
                }
            }

            if (!estado.esEdicion && estado.frecuentes.isNotEmpty()) {
                Seccion(textos.frecuentes) {
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
                label = { Text(textos.monto) },
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

            // Lo habitual (hoy, sin nombre) no se pregunta: queda plegado con su resumen.
            var detallesAbiertos by rememberSaveable(estado.esEdicion) { mutableStateOf(estado.detallesAbiertosAlInicio()) }
            LaunchedEffect(estado.errorFecha) { if (estado.errorFecha != null) detallesAbiertos = true }
            DetallesPlegables(
                abiertos = detallesAbiertos,
                resumen = resumenDetallesVenta(estado.fecha, estado.hoy, estado.descripcion),
                onCambiar = { detallesAbiertos = !detallesAbiertos },
            ) {
                OutlinedTextField(
                    value = estado.descripcion,
                    onValueChange = viewModel::onDescripcionCambiada,
                    label = { Text(textos.descripcion) },
                    placeholder = { Text(textos.ejemplo) },
                    supportingText = textos.ayudaDescripcion?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )

                Seccion("Fecha") {
                    CampoFecha(estado.fecha, estado.hoy, viewModel::onFechaElegida, estado.errorFecha)
                }
            }
        }
    }

    if (estado.confirmandoEliminar) {
        DialogoConfirmar(
            titulo = "¿Eliminar ${if (estado.tipo == TipoEntrada.VENTA) "esta" else "este"} ${estado.tipo.nombre}?",
            mensaje = "Úsalo solo si lo registraste por error. Ya no se contará en tus totales" +
                (if (estado.tipo == TipoEntrada.VENTA) " ni en tu tope" else "") + ", y quedará anotado en tu actividad.",
            textoConfirmar = "Eliminar",
            onConfirmar = viewModel::confirmarEliminar,
            onCancelar = viewModel::cancelarEliminar,
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

/** Los textos de la pantalla según lo que se anota. */
private data class TextosEntrada(
    val tituloNuevo: String,
    val tituloEditar: String,
    val guardar: String,
    val monto: String,
    val frecuentes: String,
    val descripcion: String,
    val ejemplo: String,
    val ayudaDescripcion: String?,
    val articulo: String,
)

private fun textosDe(tipo: TipoEntrada): TextosEntrada = when (tipo) {
    TipoEntrada.VENTA -> TextosEntrada(
        tituloNuevo = "Registrar venta",
        tituloEditar = "Editar venta",
        guardar = "Guardar venta",
        monto = "¿Cuánto vendiste?",
        frecuentes = "Tus ventas frecuentes (un toque y listo)",
        descripcion = "¿Qué vendiste? (opcional)",
        ejemplo = "Ej. gaseosa, menú, arreglo de zapatos",
        ayudaDescripcion = "Con nombre, la próxima vez aparece arriba para un toque.",
        articulo = "la",
    )
    TipoEntrada.INGRESO -> TextosEntrada(
        tituloNuevo = "Registrar ingreso",
        tituloEditar = "Editar ingreso",
        guardar = "Guardar ingreso",
        monto = "¿Cuánto te entró?",
        frecuentes = "Tus ingresos frecuentes (un toque y listo)",
        descripcion = "¿De qué es? (opcional)",
        ejemplo = "Ej. sueldo, propina, cachuelo",
        ayudaDescripcion = "Con nombre, la próxima vez aparece arriba para un toque.",
        articulo = "el",
    )
    TipoEntrada.RETIRO -> TextosEntrada(
        tituloNuevo = "Saqué para la casa",
        tituloEditar = "Editar retiro",
        guardar = "Guardar",
        monto = "¿Cuánto sacaste de la caja?",
        frecuentes = "",
        descripcion = "¿Para qué? (opcional)",
        ejemplo = "Ej. mercado, pasajes, colegio",
        ayudaDescripcion = "Se resta de tu caja y aparece como ingreso en tu plata personal.",
        articulo = "el",
    )
}
