package com.app.centavot.presentation.screens.cobros

import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.toggleable
import com.app.centavot.presentation.components.DetallesPlegables
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SegmentedButton
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.TipoCobro
import com.app.centavot.presentation.components.CampoFecha
import com.app.centavot.presentation.components.DialogoNuevoContacto
import com.app.centavot.presentation.components.Iconos
import com.app.centavot.presentation.components.SelectorChips
import com.app.centavot.presentation.components.formatear
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CobroScreen(
    id: String?,
    onCerrar: () -> Unit,
    viewModel: CobroViewModel = koinViewModel { parametersOf(id) },
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(estado.terminado) {
        if (estado.terminado) onCerrar()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val queEs = if (estado.esNegocio) "cobro" else "préstamo"
                    Text(if (estado.esEdicion) "Editar $queEs" else "Registrar $queEs")
                },
                navigationIcon = {
                    IconButton(onClick = onCerrar) { Icon(Iconos.Atras, contentDescription = "Volver") }
                },
            )
        },
        bottomBar = {
            Button(
                onClick = viewModel::guardar,
                enabled = !estado.guardando && !estado.cargando && estado.contactos.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(16.dp)
                    .height(56.dp),
            ) {
                Text(if (estado.esEdicion) "Guardar cambios" else if (estado.esNegocio) "Registrar cobro" else "Registrar préstamo")
            }
        },
    ) { padding ->
        if (estado.cargando) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val esPedido = estado.tipo == TipoCobro.PEDIDO
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // En lo personal solo hay préstamos: no se pregunta si es fiado o pedido.
            if (estado.esNegocio) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("¿Qué es?", style = MaterialTheme.typography.titleSmall)
                // Dos opciones excluyentes, una siempre elegida.
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    TipoCobro.entries.forEachIndexed { i, tipo ->
                        SegmentedButton(
                            selected = estado.tipo == tipo,
                            onClick = { viewModel.onTipoElegido(tipo) },
                            shape = SegmentedButtonDefaults.itemShape(index = i, count = TipoCobro.entries.size),
                        ) { Text(tipo.etiqueta) }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (esPedido) "¿De quién es el pedido?" else "¿Quién te debe?", style = MaterialTheme.typography.titleSmall)
                if (estado.contactos.isEmpty()) {
                    Text(
                        text = "Aún no tienes contactos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(onClick = viewModel::pedirNuevoContacto) {
                        Icon(Iconos.Agregar, contentDescription = null)
                        Text("Agregar contacto", modifier = Modifier.padding(start = 8.dp))
                    }
                } else {
                    SelectorChips(
                        opciones = estado.contactos,
                        seleccionada = estado.contacto,
                        etiqueta = { it.nombre },
                        onSeleccionar = viewModel::onContactoElegido,
                    )
                    AssistChip(
                        onClick = viewModel::pedirNuevoContacto,
                        label = { Text("Nuevo contacto") },
                        leadingIcon = { Icon(Iconos.Agregar, contentDescription = null) },
                    )
                }
                estado.errorContacto?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }

            OutlinedTextField(
                value = estado.montoTexto,
                onValueChange = viewModel::onMontoCambiado,
                label = { Text(if (esPedido) "Precio total del pedido" else "¿Cuánto te debe?") },
                prefix = { Text("S/ ") },
                placeholder = { Text("0.00") },
                textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                isError = estado.errorMonto != null,
                supportingText = estado.errorMonto?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            if (estado.abonado > Monto.CERO) {
                Text(
                    text = "Ya te abonó ${estado.abonado.formatear()}. Esos abonos se mantienen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (esPedido) {
                OutlinedTextField(
                    value = estado.adelantoTexto,
                    onValueChange = viewModel::onAdelantoCambiado,
                    label = { Text("Adelanto (opcional)") },
                    prefix = { Text("S/ ") },
                    placeholder = { Text("0.00") },
                    isError = estado.errorAdelanto != null,
                    supportingText = { Text(estado.errorAdelanto ?: "Lo que ya te pagó.") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                value = estado.motivo,
                onValueChange = viewModel::onMotivoCambiado,
                label = { Text(if (esPedido) "¿Qué pidió?" else "Motivo") },
                placeholder = {
                    Text(
                        when {
                            esPedido -> "Ej. arreglo de zapatos, 20 menús"
                            estado.esNegocio -> "Ej. fiado de abarrotes, préstamo"
                            else -> "Ej. préstamo, le pagué el pasaje"
                        },
                    )
                },
                isError = estado.errorMotivo != null,
                supportingText = estado.errorMotivo?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )

            // Lo habitual (se suma a las ventas, desde hoy) queda plegado con su resumen.
            var detallesAbiertos by rememberSaveable(estado.esEdicion, estado.cargando) {
                mutableStateOf(estado.detallesAbiertosAlInicio())
            }
            LaunchedEffect(estado.errorFecha) { if (estado.errorFecha != null) detallesAbiertos = true }
            DetallesPlegables(
                abiertos = detallesAbiertos,
                resumen = resumenDetallesCobro(estado.esEdicion, estado.contarComoVenta, estado.fecha, estado.hoy, estado.esNegocio),
                onCambiar = { detallesAbiertos = !detallesAbiertos },
            ) {
                if (!estado.esEdicion && estado.esNegocio) {
                    // Toda la fila es el interruptor: se puede tocar el texto, no solo el switch.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.toggleable(
                            value = estado.contarComoVenta,
                            role = Role.Switch,
                            onValueChange = viewModel::onContarComoVenta,
                        ),
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Contarlo como venta", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = if (estado.contarComoVenta) {
                                    "Se suma a tus ventas. Al cobrar no se vuelve a sumar."
                                } else {
                                    "No se suma. Úsalo si ya anotaste la venta o si prestaste plata."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = estado.contarComoVenta,
                            onCheckedChange = null,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (esPedido) "¿Cuándo te lo pidió?" else "¿Desde cuándo te debe?", style = MaterialTheme.typography.titleSmall)
                    CampoFecha(estado.fecha, estado.hoy, viewModel::onFechaElegida, estado.errorFecha)
                }
            }
        }
    }

    if (estado.agregandoContacto) {
        DialogoNuevoContacto(
            error = estado.errorNuevoContacto,
            onGuardar = viewModel::guardarNuevoContacto,
            onCancelar = viewModel::cancelarNuevoContacto,
        )
    }
}
