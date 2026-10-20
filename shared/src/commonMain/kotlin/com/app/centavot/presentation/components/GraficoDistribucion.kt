package com.app.centavot.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.sumar

/**
 * Primero los colores de la marca (Tinta, Maíz, Carmín) y luego tonos andinos que se distinguen entre sí
 * sobre fondo Lana; el último se repite si hay más partes.
 */
private val PALETA = listOf(
    Color(0xFF1B1410), Color(0xFFF4B400), Color(0xFFC1121F), Color(0xFF2F6690),
    Color(0xFF9C5B3B), Color(0xFF3A7D44), Color(0xFF7B5CC4), Color(0xFFB8A48C), Color(0xFF8C7D70),
)

data class ParteGrafico(val etiqueta: String, val monto: Monto)

/** Gráfico de dona con su leyenda: etiqueta, monto y porcentaje de cada parte. */
@Composable
fun GraficoDistribucion(partes: List<ParteGrafico>, modifier: Modifier = Modifier) {
    val total = partes.map { it.monto }.sumar()
    if (total <= Monto.CERO) return
    val porcentajes = partes.map { (it.monto.centimos * 100 / total.centimos).toInt() }
    val colorPista = MaterialTheme.colorScheme.surfaceVariant

    Row(modifier, horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(120.dp).clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(120.dp)) {
                val grosor = size.minDimension * 0.18f
                val estilo = Stroke(width = grosor)
                val inset = grosor / 2
                val area = Size(size.width - grosor, size.height - grosor)
                val origen = Offset(inset, inset)
                drawArc(colorPista, 0f, 360f, useCenter = false, topLeft = origen, size = area, style = estilo)
                var inicio = -90f
                partes.forEachIndexed { i, parte ->
                    val barrido = 360f * parte.monto.centimos / total.centimos
                    // Un pequeño espacio entre partes ayuda a distinguirlas sin depender solo del color.
                    val espacio = if (partes.size > 1) 1.5f else 0f
                    drawArc(
                        color = PALETA[minOf(i, PALETA.lastIndex)],
                        startAngle = inicio + espacio / 2,
                        sweepAngle = (barrido - espacio).coerceAtLeast(0.5f),
                        useCenter = false,
                        topLeft = origen,
                        size = area,
                        style = estilo,
                    )
                    inicio += barrido
                }
            }
            Text(total.formatear(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            partes.forEachIndexed { i, parte ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.clearAndSetSemantics {
                        contentDescription = "${parte.etiqueta}: ${parte.monto.formatear()}, ${porcentajes[i]} por ciento"
                    },
                ) {
                    Box(Modifier.size(10.dp).background(PALETA[minOf(i, PALETA.lastIndex)], CircleShape))
                    Text(
                        text = parte.etiqueta,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text(
                        text = "${porcentajes[i]} %",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
