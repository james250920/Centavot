package com.app.centavot.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.app.centavot.domain.model.Historial
import com.app.centavot.domain.model.Monto

/** "ene", "feb"… para el eje del gráfico. */
private val MESES_CORTOS = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

/**
 * Gráfico de líneas de ventas (línea continua) y gastos de negocio (línea punteada) mes a mes.
 * Las dos series se distinguen por color y por trazo, no solo por color.
 */
@Composable
fun GraficoLineas(historial: Historial, modifier: Modifier = Modifier) {
    val colorVentas = MaterialTheme.colorScheme.primary
    val colorGastos = MaterialTheme.colorScheme.tertiary
    val colorGuia = MaterialTheme.colorScheme.outlineVariant
    val maximo = historial.meses.maxOf { maxOf(it.ventas.centimos, it.gastosNegocio.centimos) }.coerceAtLeast(1)
    val descripcion = historial.meses.joinToString("; ") { mes ->
        "${MESES_CORTOS[mes.mes.month.ordinal]}: vendiste ${mes.ventas.formatear()}, gastaste ${mes.gastosNegocio.formatear()}"
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
                .semantics { contentDescription = "Ventas y gastos por mes. $descripcion" },
        ) {
            val pasos = (historial.meses.size - 1).coerceAtLeast(1)
            fun punto(i: Int, valor: Monto) = Offset(
                x = size.width * i / pasos,
                y = size.height - size.height * valor.centimos / maximo,
            )
            listOf(0f, 0.5f, 1f).forEach { fraccion ->
                val y = size.height * fraccion
                drawLine(colorGuia, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            }
            fun linea(valores: List<Monto>, color: Color, punteada: Boolean) {
                val camino = Path()
                valores.forEachIndexed { i, valor ->
                    val p = punto(i, valor)
                    if (i == 0) camino.moveTo(p.x, p.y) else camino.lineTo(p.x, p.y)
                }
                drawPath(
                    camino,
                    color,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = if (punteada) PathEffect.dashPathEffect(floatArrayOf(12f, 10f)) else null,
                    ),
                )
                valores.forEachIndexed { i, valor -> drawCircle(color, radius = 4.dp.toPx(), center = punto(i, valor)) }
            }
            linea(historial.meses.map { it.gastosNegocio }, colorGastos, punteada = true)
            linea(historial.meses.map { it.ventas }, colorVentas, punteada = false)
        }
        Row(Modifier.fillMaxWidth()) {
            historial.meses.forEach { mes ->
                Text(
                    text = MESES_CORTOS[mes.mes.month.ordinal],
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Leyenda("Ventas (línea continua)", colorVentas)
            Leyenda("Gastos (punteada)", colorGastos)
        }
    }
}

@Composable
private fun Leyenda(texto: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Text(texto, style = MaterialTheme.typography.bodySmall)
    }
}
