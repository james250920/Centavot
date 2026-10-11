package com.app.centavot.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.centavot.domain.model.EstadoTope
import com.app.centavot.domain.model.MedidaTope
import com.app.centavot.domain.model.NivelAlerta
import com.app.centavot.domain.model.PeriodoTope
import com.app.centavot.domain.model.ProximidadTope

private data class EstiloAlerta(
    val color: Color,
    val fondo: Color,
    val texto: Color,
    val icono: ImageVector,
    val mensaje: String,
)

@Composable
fun TarjetaTope(
    estado: EstadoTope,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    val regimen = estado.regimen
    val periodo = if (regimen.periodo == PeriodoTope.MENSUAL) "este mes" else "este año"
    val (medida, proximidad) = estado.principal
    val restante = proximidad.restante.formatear()
    val estilo = when (proximidad.nivelAlerta) {
        NivelAlerta.NINGUNA -> EstiloAlerta(
            colores.primary, colores.primaryContainer, colores.onPrimaryContainer, Iconos.Correcto,
            "Vas bien: te quedan $restante.",
        )
        NivelAlerta.AVISO_80 -> EstiloAlerta(
            colores.tertiary, colores.tertiaryContainer, colores.onTertiaryContainer, Iconos.Aviso,
            "En ${medida.etiqueta} te quedan $restante $periodo.",
        )
        NivelAlerta.AVISO_90 -> EstiloAlerta(
            colores.tertiary, colores.tertiaryContainer, colores.onTertiaryContainer, Iconos.Aviso,
            "En ${medida.etiqueta} te quedan $restante. Habla con tu contador.",
        )
        NivelAlerta.TOPE_ALCANZADO -> EstiloAlerta(
            colores.error, colores.errorContainer, colores.onErrorContainer, Iconos.Aviso,
            "Llegaste al tope. Habla con tu contador.",
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colores.surfaceContainerLow),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "${regimen.nombre} · tope ${proximidad.tope.formatear()} ${if (regimen.periodo == PeriodoTope.MENSUAL) "al mes" else "al año"}",
                style = MaterialTheme.typography.labelLarge,
                color = colores.onSurfaceVariant,
            )
            estado.medidas.forEach { (cual, valor) ->
                BarraTope(cual, valor, periodo, if (cual == medida) estilo.color else colores.primary)
            }
            Surface(color = estilo.fondo, contentColor = estilo.texto, shape = MaterialTheme.shapes.medium) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(estilo.icono, contentDescription = null)
                    Text(estilo.mensaje, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun BarraTope(medida: MedidaTope, proximidad: ProximidadTope, periodo: String, color: Color) {
    val colores = MaterialTheme.colorScheme
    val nombre = medida.etiqueta.replaceFirstChar { it.uppercase() }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "${proximidad.porcentaje} %",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            Text(
                text = "$nombre $periodo: ${proximidad.acumulado.formatear()}",
                style = MaterialTheme.typography.bodyMedium,
                color = colores.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        LinearProgressIndicator(
            progress = { (proximidad.porcentaje / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .semantics { contentDescription = "$nombre: ${proximidad.porcentaje} por ciento del tope usado" },
            color = color,
            trackColor = colores.surfaceVariant,
            strokeCap = StrokeCap.Round,
        )
    }
}
