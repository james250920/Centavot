package com.app.centavot.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.centavot.domain.model.Modo
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.ResumenPeriodo
import com.app.centavot.domain.model.lecturaDelMes

/**
 * Lo primero al abrir: cuánto entró y cuánto se gastó en el mes, en dos barras, con una frase que lo explica.
 * Como el cuadro de la banca, pero sin mezclar lo personal con el negocio.
 */
@Composable
fun TarjetaEsteMes(modo: Modo, mes: ResumenPeriodo, modifier: Modifier = Modifier) {
    val lectura = mes.lecturaDelMes(modo) ?: return
    val (entro, gasto) = if (modo == Modo.NEGOCIO) mes.ventas to mes.gastosNegocio else mes.ingresosPersonales to mes.gastosPersonales
    val maximo = maxOf(entro.centimos, gasto.centimos).coerceAtLeast(1)
    val colores = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colores.surfaceContainerLow),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Este mes", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            Barra(if (modo == Modo.NEGOCIO) "Vendiste" else "Te entró", entro, entro.centimos.toFloat() / maximo, colores.primary)
            Barra("Gastaste", gasto, gasto.centimos.toFloat() / maximo, colores.outline)
            Text(lectura, style = MaterialTheme.typography.bodyMedium, color = colores.onSurfaceVariant)
        }
    }
}

@Composable
private fun Barra(etiqueta: String, monto: Monto, fraccion: Float, color: Color) {
    Column(
        Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = "$etiqueta ${monto.formatear()}" },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            Text(etiqueta, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(monto.formatear(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        Box(Modifier.fillMaxWidth().height(10.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(5.dp))) {
            if (fraccion > 0f) {
                Box(Modifier.fillMaxWidth(fraccion.coerceIn(0.02f, 1f)).height(10.dp).background(color, RoundedCornerShape(5.dp)))
            }
        }
    }
}
