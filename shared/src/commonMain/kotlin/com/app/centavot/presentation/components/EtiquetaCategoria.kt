package com.app.centavot.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.centavot.domain.model.Categoria

val Categoria.etiqueta: String
    get() = when (this) {
        Categoria.NEGOCIO -> "Negocio"
        Categoria.PERSONAL -> "Personal"
    }

@Composable
fun EtiquetaCategoria(categoria: Categoria?, modifier: Modifier = Modifier) {
    val colores = MaterialTheme.colorScheme
    val (fondo, texto) = when (categoria) {
        Categoria.NEGOCIO -> colores.primaryContainer to colores.onPrimaryContainer
        Categoria.PERSONAL -> colores.secondaryContainer to colores.onSecondaryContainer
        null -> colores.tertiaryContainer to colores.onTertiaryContainer
    }
    Surface(color = fondo, contentColor = texto, shape = MaterialTheme.shapes.small, modifier = modifier) {
        Text(
            text = categoria?.etiqueta ?: "Sin clasificar",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
