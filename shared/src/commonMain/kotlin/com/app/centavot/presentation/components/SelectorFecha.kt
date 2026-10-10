package com.app.centavot.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

private const val MILIS_POR_DIA = 86_400_000L

/** Chips "Hoy", "Ayer" y "Otra fecha" (con calendario, sin fechas futuras). */
@Composable
fun CampoFecha(
    fecha: LocalDate,
    hoy: LocalDate,
    onFechaElegida: (LocalDate) -> Unit,
    error: String? = null,
) {
    var mostrarCalendario by remember { mutableStateOf(false) }
    val ayer = hoy.minus(1, DateTimeUnit.DAY)
    val esOtraFecha = fecha != hoy && fecha != ayer

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = fecha == hoy, onClick = { onFechaElegida(hoy) }, label = { Text("Hoy") })
            FilterChip(selected = fecha == ayer, onClick = { onFechaElegida(ayer) }, label = { Text("Ayer") })
            FilterChip(
                selected = esOtraFecha,
                onClick = { mostrarCalendario = true },
                label = { Text(if (esOtraFecha) fecha.formatearRelativo(hoy) else "Otra fecha") },
            )
        }
        error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
    }

    if (mostrarCalendario) {
        SelectorFecha(
            inicial = fecha,
            hoy = hoy,
            onElegir = {
                onFechaElegida(it)
                mostrarCalendario = false
            },
            onCancelar = { mostrarCalendario = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorFecha(
    inicial: LocalDate,
    hoy: LocalDate,
    onElegir: (LocalDate) -> Unit,
    onCancelar: () -> Unit,
) {
    val limite = hoy.toEpochDays() * MILIS_POR_DIA
    val estadoCalendario = rememberDatePickerState(
        initialSelectedDateMillis = inicial.toEpochDays() * MILIS_POR_DIA,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= limite
        },
    )
    DatePickerDialog(
        onDismissRequest = onCancelar,
        confirmButton = {
            TextButton(
                onClick = {
                    estadoCalendario.selectedDateMillis?.let { onElegir(LocalDate.fromEpochDays(it / MILIS_POR_DIA)) }
                },
            ) { Text("Aceptar") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    ) {
        DatePicker(state = estadoCalendario)
    }
}
