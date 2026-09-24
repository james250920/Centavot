package com.app.centavot.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.centavot.domain.model.Gasto
import kotlinx.datetime.LocalDate

@Composable
fun FilaGasto(
    gasto: Gasto,
    hoy: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    mostrarFecha: Boolean = true,
) {
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = {
            Text(
                text = gasto.descripcion ?: "Sin descripción",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                EtiquetaCategoria(gasto.categoria)
                if (mostrarFecha) {
                    Text(gasto.fecha.formatearRelativo(hoy), style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        trailingContent = {
            Text(
                text = gasto.monto.formatear(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
    )
}
