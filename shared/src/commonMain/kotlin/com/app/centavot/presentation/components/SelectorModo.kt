package com.app.centavot.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.app.centavot.domain.model.Modo

/** Primero lo personal, como lo plantea la pregunta: "¿lo usarás para tu plata o para tu negocio?". */
private val ORDEN = listOf(Modo.PERSONAL, Modo.NEGOCIO)

val Modo.descripcion: String
    get() = when (this) {
        Modo.PERSONAL -> "Tu presupuesto: sueldo, gastos de la casa, a quién le prestaste y cuánto ahorras."
        Modo.NEGOCIO -> "Tu bodega o puesto: ventas, compras, fiados, el tope de tu régimen y el resumen para tu contador."
    }

val Modo.icono: ImageVector
    get() = when (this) {
        Modo.PERSONAL -> Iconos.Personal
        Modo.NEGOCIO -> Iconos.Negocio
    }

/** Las dos formas de usar Centavot, como tarjetas grandes de opción única. */
@Composable
fun SelectorModo(seleccionado: Modo?, onSeleccionar: (Modo) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ORDEN.forEach { modo -> OpcionModo(modo, modo == seleccionado, onSeleccionar = { onSeleccionar(modo) }) }
    }
}

@Composable
private fun OpcionModo(modo: Modo, seleccionado: Boolean, onSeleccionar: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = seleccionado, onClick = onSeleccionar, role = Role.RadioButton),
        colors = CardDefaults.cardColors(
            containerColor = if (seleccionado) colores.primaryContainer else colores.surfaceContainerLow,
            contentColor = if (seleccionado) colores.onPrimaryContainer else colores.onSurface,
        ),
        border = if (seleccionado) BorderStroke(2.dp, colores.primary) else null,
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(modo.icono, contentDescription = null, tint = colores.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(modo.etiqueta, style = MaterialTheme.typography.titleMedium)
                Text(
                    modo.descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (seleccionado) colores.onPrimaryContainer else colores.onSurfaceVariant,
                )
            }
            RadioButton(selected = seleccionado, onClick = null)
        }
    }
}
