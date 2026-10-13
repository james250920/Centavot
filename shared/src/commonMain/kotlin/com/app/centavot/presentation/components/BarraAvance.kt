package com.app.centavot.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.app.centavot.domain.model.EstadoLinea

/** Barra de avance de 8 dp (presupuesto, metas). [descripcion] es lo que lee TalkBack. */
@Composable
fun BarraAvance(porcentaje: Int, color: Color, descripcion: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(4.dp))
            .semantics { contentDescription = descripcion },
    ) {
        if (porcentaje > 0) {
            Box(
                Modifier
                    .fillMaxWidth((porcentaje.coerceAtMost(100) / 100f).coerceAtLeast(0.02f))
                    .height(8.dp)
                    .background(color, RoundedCornerShape(4.dp)),
            )
        }
    }
}

/** Colores con un solo significado: verde bien, ámbar atención (desde el 90 %), rojo pasado. */
@Composable
fun EstadoLinea.color(): Color = when (this) {
    EstadoLinea.SIN_MONTO -> MaterialTheme.colorScheme.outline
    EstadoLinea.BIEN -> MaterialTheme.colorScheme.primary
    EstadoLinea.CERCA -> MaterialTheme.colorScheme.tertiary
    EstadoLinea.PASADO -> MaterialTheme.colorScheme.error
}
