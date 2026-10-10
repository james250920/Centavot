package com.app.centavot.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

/**
 * Patrón de los formularios de registro: lo habitual no se pregunta. Una fila muestra lo que se va a
 * guardar ([resumen]) y, al tocarla, abre los campos para cambiarlo.
 */
@Composable
fun DetallesPlegables(
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
