package com.app.lucka.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.lucka.domain.model.Gasto
import com.app.lucka.domain.model.Ingreso
import com.app.lucka.domain.model.Monto
import kotlinx.datetime.LocalDate

/** Una venta (o ingreso) o un gasto, para mostrarlos juntos en una sola lista. */
sealed interface Movimiento {
    val id: String
    val fecha: LocalDate
    val monto: Monto

    data class Entrada(val ingreso: Ingreso) : Movimiento {
        override val id get() = ingreso.id
        override val fecha get() = ingreso.fecha
        override val monto get() = ingreso.monto
    }

    data class Salida(val gasto: Gasto) : Movimiento {
        override val id get() = gasto.id
        override val fecha get() = gasto.fecha
        override val monto get() = gasto.monto
    }
}

/** Ventas y gastos juntos, del más reciente al más antiguo. */
fun movimientosDe(ingresos: List<Ingreso>, gastos: List<Gasto>): List<Movimiento> =
    (ingresos.map { Movimiento.Entrada(it) } + gastos.map { Movimiento.Salida(it) }).sortedByDescending { it.fecha }

@Composable
fun FilaMovimiento(
    movimiento: Movimiento,
    hoy: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    mostrarFecha: Boolean = true,
) {
    when (movimiento) {
        is Movimiento.Salida -> FilaGasto(movimiento.gasto, hoy, onClick, modifier, mostrarFecha)
        is Movimiento.Entrada -> FilaIngreso(movimiento.ingreso, hoy, onClick, modifier, mostrarFecha)
    }
}

@Composable
fun FilaIngreso(
    ingreso: Ingreso,
    hoy: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    mostrarFecha: Boolean = true,
) {
    val colores = MaterialTheme.colorScheme
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = {
            Text(
                text = ingreso.descripcion ?: if (ingreso.esDeNegocio) "Venta" else "Ingreso",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = colores.primary, contentColor = colores.onPrimary, shape = MaterialTheme.shapes.small) {
                    Text(
                        text = if (ingreso.esDeNegocio) "Venta" else "Ingreso personal",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
                if (mostrarFecha) {
                    Text(ingreso.fecha.formatearRelativo(hoy), style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        trailingContent = {
            Text(
                text = "+ ${ingreso.monto.formatear()}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colores.primary,
            )
        },
    )
}
