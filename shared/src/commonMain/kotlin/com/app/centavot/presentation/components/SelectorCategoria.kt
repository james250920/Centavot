package com.app.centavot.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.app.centavot.domain.model.Categoria

private val OPCIONES = listOf(Categoria.NEGOCIO, Categoria.PERSONAL)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorCategoria(
    seleccionada: Categoria?,
    onSeleccionar: (Categoria) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        OPCIONES.forEachIndexed { indice, categoria ->
            SegmentedButton(
                selected = seleccionada == categoria,
                onClick = { onSeleccionar(categoria) },
                shape = SegmentedButtonDefaults.itemShape(index = indice, count = OPCIONES.size),
            ) {
                Text(categoria.etiqueta)
            }
        }
    }
}
