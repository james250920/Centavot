package com.app.lucka.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Chips de opción única; tocar la opción elegida la quita (el campo es opcional). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> SelectorChips(
    opciones: List<T>,
    seleccionada: T?,
    etiqueta: (T) -> String,
    onSeleccionar: (T?) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        opciones.forEach { opcion ->
            FilterChip(
                selected = opcion == seleccionada,
                onClick = { onSeleccionar(if (opcion == seleccionada) null else opcion) },
                label = { Text(etiqueta(opcion)) },
            )
        }
    }
}
