package com.app.centavot.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * Una pregunta que se toca para ver la respuesta. Para las explicaciones que solo necesita quien
 * tiene la duda: la pantalla queda corta y la ayuda sigue a un toque.
 */
@Composable
fun AyudaPlegable(pregunta: String, respuestas: List<String>, modifier: Modifier = Modifier) {
    var abierta by rememberSaveable(pregunta) { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .toggleable(value = abierta, role = Role.Button, onValueChange = { abierta = it }),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Iconos.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(pregunta, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
            Icon(if (abierta) Iconos.Plegar else Iconos.Desplegar, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        AnimatedVisibility(visible = abierta) {
            Column(Modifier.padding(start = 32.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                respuestas.forEach {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
