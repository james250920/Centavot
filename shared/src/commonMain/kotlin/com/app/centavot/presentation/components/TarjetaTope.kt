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
import com.app.centavot.domain.model.NivelAlerta
import com.app.centavot.domain.model.PeriodoTope
import com.app.centavot.domain.model.ProximidadTope
import com.app.centavot.domain.model.RegimenTributario

private data class EstiloAlerta(
    val color: Color,
    val fondo: Color,
    val texto: Color,
    val icono: ImageVector,
    val mensaje: String,
)

@Composable
fun TarjetaTope(
    proximidad: ProximidadTope,
    regimen: RegimenTributario,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    val periodo = if (regimen.periodo == PeriodoTope.MENSUAL) "este mes" else "este año"
    val restante = proximidad.restante.formatear()
    val estilo = when (proximidad.nivelAlerta) {
        NivelAlerta.NINGUNA -> EstiloAlerta(
            colores.primary, colores.primaryContainer, colores.onPrimaryContainer, Iconos.Correcto,
            "Vas bien. Te quedan $restante antes de llegar a tu tope.",
        )
        NivelAlerta.AVISO_80 -> EstiloAlerta(
            colores.tertiary, colores.tertiaryContainer, colores.onTertiaryContainer, Iconos.Aviso,
            "Ya pasaste el 80 % de tu tope. Te quedan $restante $periodo.",
        )
        NivelAlerta.AVISO_90 -> EstiloAlerta(
            colores.tertiary, colores.tertiaryContainer, colores.onTertiaryContainer, Iconos.Aviso,
            "Estás muy cerca del tope: te quedan $restante. Consulta con tu contador.",
        )
        NivelAlerta.TOPE_ALCANZADO -> EstiloAlerta(
            colores.error, colores.errorContainer, colores.onErrorContainer, Iconos.Aviso,
            "Llegaste al tope de tu régimen. Habla con tu contador para evitar una multa.",
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colores.surfaceContainerLow),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Tope de tu régimen · ${regimen.nombre}",
                style = MaterialTheme.typography.labelLarge,
                color = colores.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${proximidad.porcentaje} %",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = estilo.color,
                )
                Text(
                    text = "usado $periodo",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colores.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            LinearProgressIndicator(
                progress = { (proximidad.porcentaje / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .semantics { contentDescription = "${proximidad.porcentaje} por ciento del tope usado" },
                color = estilo.color,
                trackColor = colores.surfaceVariant,
                strokeCap = StrokeCap.Round,
            )
            Text(
                text = "${proximidad.acumulado.formatear()} en gastos de negocio de ${proximidad.tope.formatear()}",
                style = MaterialTheme.typography.bodyMedium,
                color = colores.onSurfaceVariant,
            )
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
