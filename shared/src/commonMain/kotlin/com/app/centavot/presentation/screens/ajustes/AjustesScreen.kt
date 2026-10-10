package com.app.centavot.presentation.screens.ajustes

import com.app.centavot.presentation.components.LogoCentavot
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.centavot.domain.model.PeriodoTope
import com.app.centavot.domain.model.ResumenUso
import com.app.centavot.domain.model.Rubro
import com.app.centavot.presentation.components.DialogoConfirmar
import com.app.centavot.presentation.components.Iconos
import com.app.centavot.presentation.components.SelectorChips
import com.app.centavot.presentation.components.formatear
import com.app.centavot.presentation.components.parsearMonto
import com.app.centavot.presentation.components.parsearTasa
import com.app.centavot.domain.model.Perfil
import org.koin.compose.viewmodel.koinViewModel

/**
 * Perfil y ajustes financieros. Es el primer paso del onboarding ([esPrimeraVez])
 * y también se abre desde Inicio.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(
    esPrimeraVez: Boolean,
    onCerrar: () -> Unit,
    onCambiarRegimen: () -> Unit,
    onVerPrivacidad: () -> Unit = {},
    viewModel: AjustesViewModel = koinViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(estado.terminado) {
        if (estado.terminado) onCerrar()
    }

    Scaffold(
        topBar = {
            if (!esPrimeraVez) {
                TopAppBar(
                    title = { Text("Ajustes") },
                    navigationIcon = {
                        IconButton(onClick = onCerrar) { Icon(Iconos.Atras, contentDescription = "Volver") }
                    },
                )
            }
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
                Text(if (esPrimeraVez) "Continuar" else "Guardar cambios")
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
            if (esPrimeraVez) {
                Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LogoCentavot(tamano = 88.dp)
                    Text(
                        text = "Te damos la bienvenida a Centavot",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Tu cuaderno, pero que suma solo. Cuéntanos un poco de ti para empezar.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            TarjetaPrivacidad()

            OutlinedTextField(
                value = estado.nombre,
                onValueChange = viewModel::onNombreCambiado,
                label = { Text("¿Cómo te llamas?") },
                isError = estado.errorNombre != null,
                supportingText = estado.errorNombre?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("¿A qué se dedica tu negocio?", style = MaterialTheme.typography.titleSmall)
                SelectorChips(
                    opciones = Rubro.entries,
                    seleccionada = estado.rubro,
                    etiqueta = { it.etiqueta },
                    onSeleccionar = viewModel::onRubroElegido,
                )
                estado.errorRubro?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }

            HorizontalDivider()

            Text("Tu meta de ahorro", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Cuando registres tus ventas, la meta se calcula sobre lo que de verdad ganas. " +
                    "Mientras tanto, usamos el ingreso que pongas aquí.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = estado.ingresoTexto,
                onValueChange = viewModel::onIngresoCambiado,
                label = { Text(if (esPrimeraVez) "Ingreso mensual (opcional)" else "Ingreso mensual") },
                prefix = { Text("S/ ") },
                placeholder = { Text("0.00") },
                isError = estado.errorIngreso != null,
                supportingText = { Text(estado.errorIngreso ?: "Lo que ganas en un mes normal, más o menos. Si tienes sueldo fijo, pon tu sueldo.") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            val meta = parsearMonto(estado.ingresoTexto.ifBlank { "0" })?.let { ingreso ->
                parsearTasa(estado.tasaTexto)?.let { tasa -> Perfil("", null, ingreso, tasa).metaAhorro }
            }
            OutlinedTextField(
                value = estado.tasaTexto,
                onValueChange = viewModel::onTasaCambiada,
                label = { Text("¿Qué porcentaje quieres ahorrar?") },
                suffix = { Text("%") },
                placeholder = { Text("10") },
                isError = estado.errorTasa != null,
                supportingText = {
                    Text(
                        estado.errorTasa
                            ?: meta?.takeIf { it.centimos > 0 }?.let { "Tu meta de ahorro será ${it.formatear()} al mes." }
                            ?: "Entre 0 y 100, con un decimal como máximo.",
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )

            estado.regimen?.takeIf { !esPrimeraVez }?.let { regimen ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Régimen tributario", style = MaterialTheme.typography.labelLarge)
                            val periodo = if (regimen.periodo == PeriodoTope.MENSUAL) "al mes" else "al año"
                            Text(
                                text = "${regimen.nombre} · tope ${regimen.tope.formatear()} $periodo",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = onCambiarRegimen) { Text("Cambiar") }
                    }
                }
            }

            estado.uso?.takeIf { !esPrimeraVez && it.primerUso != null }?.let { uso -> TarjetaUso(uso) }

            if (!esPrimeraVez) {
                TarjetaTusDatos(
                    trabajando = estado.trabajandoConDatos,
                    onVerPrivacidad = onVerPrivacidad,
                    onExportar = viewModel::exportarTodosMisDatos,
                    onBorrar = viewModel::pedirBorrado,
                )
            }
        }
    }

    if (estado.confirmandoBorrado) {
        DialogoConfirmar(
            titulo = "¿Borrar todos tus datos?",
            mensaje = "Se borrarán de este celular tus ventas, gastos, cobros, contactos, tu perfil y tu historial. " +
                "No se puede deshacer. Si quieres guardar una copia, primero usa \"Exportar todos mis datos\".",
            textoConfirmar = "Borrar todo",
            onConfirmar = viewModel::confirmarBorrado,
            onCancelar = viewModel::cancelarBorrado,
        )
    }
}

/** Derechos sobre los datos (Ley 29733): leer el aviso, llevarse los datos y borrarlos. */
@Composable
private fun TarjetaTusDatos(
    trabajando: Boolean,
    onVerPrivacidad: () -> Unit,
    onExportar: () -> Unit,
    onBorrar: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Tus datos", style = MaterialTheme.typography.titleSmall)
            Text(
                "Todo está solo en este celular y no se copia a la nube. Para no perderlo si cambias de celular, " +
                    "exporta tus datos y guárdalos donde quieras.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onVerPrivacidad, modifier = Modifier.fillMaxWidth()) { Text("Ver aviso de privacidad") }
            OutlinedButton(onClick = onExportar, enabled = !trabajando, modifier = Modifier.fillMaxWidth()) {
                Text("Exportar todos mis datos")
            }
            OutlinedButton(
                onClick = onBorrar,
                enabled = !trabajando,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text("Borrar todos mis datos") }
        }
    }
}

/** Lo que la entrevista pidió dejar claro: sin banco, sin SUNAT, los datos son del usuario. */
@Composable
private fun TarjetaPrivacidad() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Iconos.Candado, contentDescription = null)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Tus datos son tuyos", style = MaterialTheme.typography.titleSmall)
                Text("• No pedimos tus claves ni tu cuenta del banco.", style = MaterialTheme.typography.bodyMedium)
                Text("• No le enviamos nada a SUNAT. Los reportes solo los ves tú y a quien tú se los mandes.", style = MaterialTheme.typography.bodyMedium)
                Text("• Todo se guarda solo en tu celular.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Indicadores de uso de la primera fase (se miden solo en este celular). */
@Composable
private fun TarjetaUso(uso: ResumenUso) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Cómo vienes usando Centavot", style = MaterialTheme.typography.titleSmall)
            val respuesta = uso.respuestasCuaderno.lastOrNull()?.let { if (it) "más fácil" else "todavía no" } ?: "sin responder"
            listOf(
                "Veces que usaste la app hoy" to "${uso.aperturasHoy} ${if (uso.aperturasHoy == 1) "vez" else "veces"}",
                "Registros de hoy" to "${uso.registrosHoy}",
                "Días con registros (últimos 7)" to "${uso.diasConRegistroUltimos7} de 7",
                "Días con registros desde que empezaste" to "${uso.diasConRegistro} de ${uso.diasDesdePrimerUso + 1}",
                "Registros por día (cuando registras)" to "${uso.registrosPorDiaActivo}",
                "¿Más fácil que el cuaderno?" to respuesta,
            ).forEach { (titulo, valor) ->
                Row(Modifier.fillMaxWidth()) {
                    Text(titulo, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(valor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
