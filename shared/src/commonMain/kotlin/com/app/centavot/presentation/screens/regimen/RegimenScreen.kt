package com.app.centavot.presentation.screens.regimen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.centavot.domain.model.PeriodoTope
import com.app.centavot.domain.model.RegimenTributario
import com.app.centavot.presentation.components.Iconos
import com.app.centavot.presentation.components.formatear
import org.koin.compose.viewmodel.koinViewModel

/**
 * Elegir el régimen tributario. Es la primera pantalla de la app ([esPrimeraVez])
 * y también se abre desde Inicio para cambiarlo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegimenScreen(
    esPrimeraVez: Boolean,
    onCerrar: () -> Unit,
    viewModel: RegimenViewModel = koinViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(estado.terminado) {
        if (estado.terminado) onCerrar()
    }

    Scaffold(
        topBar = {
            if (!esPrimeraVez) {
                TopAppBar(
                    title = { Text("Tu régimen") },
                    navigationIcon = {
                        IconButton(onClick = onCerrar) { Icon(Iconos.Atras, contentDescription = "Volver") }
                    },
                )
            }
        },
        bottomBar = {
            Button(
                onClick = viewModel::guardar,
                enabled = estado.seleccionado != null && !estado.guardando,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(56.dp),
            ) {
                Text(if (esPrimeraVez) "Empezar" else "Guardar")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (esPrimeraVez) {
                Text(
                    text = "Te damos la bienvenida a Centavot",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 32.dp),
                )
            }
            Text("¿En qué régimen tributario estás?", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Lo usamos para avisarte antes de que llegues a tu tope y armar tu reporte para SUNAT.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Column(Modifier.selectableGroup().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                estado.opciones.forEach { opcion ->
                    OpcionRegimen(
                        regimen = opcion,
                        seleccionado = opcion == estado.seleccionado,
                        onSeleccionar = { viewModel.seleccionar(opcion) },
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                Icon(Iconos.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "Montos referenciales de SUNAT. Si no sabes tu categoría, revísala en tu Clave SOL o pregúntale a tu contador.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun OpcionRegimen(
    regimen: RegimenTributario,
    seleccionado: Boolean,
    onSeleccionar: () -> Unit,
) {
    val colores = MaterialTheme.colorScheme
    val periodo = if (regimen.periodo == PeriodoTope.MENSUAL) "al mes" else "al año"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = seleccionado, onClick = onSeleccionar, role = Role.RadioButton),
        colors = CardDefaults.cardColors(
            containerColor = if (seleccionado) colores.primaryContainer else colores.surfaceContainerLow,
        ),
        border = if (seleccionado) BorderStroke(2.dp, colores.primary) else null,
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = seleccionado, onClick = null)
            Column(Modifier.padding(start = 12.dp)) {
                Text(regimen.nombre, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Tope: ${regimen.tope.formatear()} $periodo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colores.onSurfaceVariant,
                )
            }
        }
    }
}
