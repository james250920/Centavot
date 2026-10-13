package com.app.centavot.presentation.screens.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.centavot.domain.model.EstadoLinea
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.PresupuestoMes
import com.app.centavot.domain.model.ProgresoMeta
import com.app.centavot.presentation.components.BarraAvance
import com.app.centavot.presentation.components.color
import com.app.centavot.presentation.components.formatear

private const val METAS_EN_INICIO = 2

@Composable
private fun TarjetaPlan(titulo: String, accion: String, onAccion: () -> Unit, contenido: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).semantics { heading() })
                TextButton(onClick = onAccion) { Text(accion) }
            }
            contenido()
        }
    }
}

/** Sin presupuesto, invita a armarlo; con presupuesto, cuánto lleva gastado del total y una frase. */
@Composable
internal fun TarjetaPresupuesto(presupuesto: PresupuestoMes, onAbrir: () -> Unit, onArmar: () -> Unit) {
    if (!presupuesto.hayPartidas) {
        TarjetaPlan("Tu presupuesto", "Armar", onArmar) {
            Text(
                "Marca en una lista lo que tienes cada mes y aquí verás si vas bien.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    TarjetaPlan("Tu presupuesto", "Ver", onAbrir) {
        val total = presupuesto.presupuestoGastos
        val real = presupuesto.realGastos
        val porcentaje = if (total > Monto.CERO) (real.centimos * 100 / total.centimos).toInt() else 0
        val estado = when {
            total <= Monto.CERO -> EstadoLinea.SIN_MONTO
            real > total -> EstadoLinea.PASADO
            porcentaje >= 90 -> EstadoLinea.CERCA
            else -> EstadoLinea.BIEN
        }
        Row(Modifier.fillMaxWidth()) {
            Text("Gastaste", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                if (total > Monto.CERO) "${real.formatear()} de ${total.formatear()}" else real.formatear(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        BarraAvance(porcentaje, estado.color(), if (total > Monto.CERO) "Gastaste el $porcentaje por ciento de tu presupuesto" else "Gastaste ${real.formatear()}")
        (presupuesto.lectura ?: "Ponle monto a tus partidas para saber si vas bien.").let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Sin metas, una línea para ponerse una; con metas, las dos primeras con su avance. */
@Composable
internal fun TarjetaMetas(metas: List<ProgresoMeta>, onAbrir: () -> Unit) {
    if (metas.isEmpty()) {
        TarjetaPlan("Tus metas", "Nueva", onAbrir) {
            Text(
                "Junta para un viaje, tus estudios o algo para el negocio, y mira cuánto llevas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    TarjetaPlan("Tus metas", "Ver", onAbrir) {
        metas.take(METAS_EN_INICIO).forEach { progreso ->
            Row(Modifier.fillMaxWidth()) {
                Text(
                    progreso.meta.nombre,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text("${progreso.porcentaje} %", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
            BarraAvance(progreso.porcentaje, MaterialTheme.colorScheme.primary, "${progreso.meta.nombre}: ${progreso.porcentaje} por ciento")
        }
        if (metas.size > METAS_EN_INICIO) {
            FilledTonalButton(onClick = onAbrir) { Text("Ver las ${metas.size}") }
        }
    }
}
