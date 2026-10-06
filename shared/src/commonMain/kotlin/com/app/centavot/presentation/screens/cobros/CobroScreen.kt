package com.app.centavot.presentation.screens.cobros

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
                title = { Text(if (estado.esEdicion) "Editar cobro" else "Registrar cobro") },
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
                Text(if (estado.esEdicion) "Guardar cambios" else "Registrar cobro")
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("¿Qué es?", style = MaterialTheme.typography.titleSmall)
                SelectorChips(
                    opciones = TipoCobro.entries,
                    seleccionada = estado.tipo,
                    etiqueta = { it.etiqueta },
                    onSeleccionar = viewModel::onTipoElegido,
                )
                Text(
                    text = if (esPedido) {
                        "Un trabajo o pedido que te encargaron (zapatos, costura, menús). Anota el adelanto si te dieron uno."
                    } else {
                        "Lo que fiaste o prestaste a alguien."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (esPedido) "¿De quién es el pedido?" else "¿Quién te debe?", style = MaterialTheme.typography.titleSmall)
                if (estado.contactos.isEmpty()) {
                    Text(
                        text = "Aún no tienes contactos. Agrega a la persona que te debe para registrar el cobro.",
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
                label = { Text(if (esPedido) "Precio total del pedido" else "Monto") },
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
                    supportingText = { Text(estado.errorAdelanto ?: "Lo que ya te pagó. Te deberá el resto.") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                value = estado.motivo,
                onValueChange = viewModel::onMotivoCambiado,
                label = { Text(if (esPedido) "¿Qué pidió?" else "Motivo") },
                placeholder = { Text(if (esPedido) "Ej. arreglo de zapatos, 20 menús" else "Ej. fiado de abarrotes, préstamo") },
                isError = estado.errorMotivo != null,
                supportingText = estado.errorMotivo?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )

            if (!estado.esEdicion) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Contarlo como venta", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (estado.contarComoVenta) {
                                "Se suma hoy a tus ventas y a tu tope. Cuando te pague no se vuelve a contar."
                            } else {
                                "No se suma a tus ventas. Úsalo si ya anotaste la venta o si es un préstamo de plata."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = estado.contarComoVenta,
                        onCheckedChange = viewModel::onContarComoVenta,
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

    if (estado.agregandoContacto) {
        DialogoNuevoContacto(
            error = estado.errorNuevoContacto,
            onGuardar = viewModel::guardarNuevoContacto,
            onCancelar = viewModel::cancelarNuevoContacto,
        )
    }
}
