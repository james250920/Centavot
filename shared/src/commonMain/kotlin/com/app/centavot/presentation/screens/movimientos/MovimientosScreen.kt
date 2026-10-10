package com.app.centavot.presentation.screens.movimientos

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.Periodo
import com.app.centavot.presentation.components.EstadoVacio
import com.app.centavot.presentation.components.FilaMovimiento
import com.app.centavot.presentation.components.Iconos
import com.app.centavot.presentation.components.Movimiento
import com.app.centavot.presentation.components.formatear
import com.app.centavot.presentation.components.formatearRelativo
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel

private const val MILIS_POR_DIA = 86_400_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovimientosScreen(
    onAbrirMovimiento: (Movimiento) -> Unit,
    onRegistrarVenta: () -> Unit,
    onRegistrarGasto: () -> Unit,
    viewModel: MovimientosViewModel = koinViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var eligiendoFechas by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("Movimientos") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FiltroMovimientos.entries.forEach { filtro ->
                    FilterChip(
                        selected = estado.filtro == filtro,
                        onClick = { viewModel.onFiltro(filtro) },
                        label = { Text(filtro.etiqueta) },
                    )
                }
            }
            val opcionesFecha = listOf(FiltroFechas.Todo) + Periodo.entries.map { FiltroFechas.Rapido(it) }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                opcionesFecha.forEach { opcion ->
                    FilterChip(
                        selected = estado.fechas == opcion,
                        onClick = { viewModel.onFechas(opcion) },
                        label = { Text(opcion.etiqueta) },
                    )
                }
                val rango = estado.fechas as? FiltroFechas.Rango
                FilterChip(
                    selected = rango != null,
                    onClick = { eligiendoFechas = true },
                    leadingIcon = { Icon(Iconos.Calendario, contentDescription = null) },
                    label = {
                        Text(
                            rango?.let { "${it.desde.formatearRelativo(estado.hoy)} – ${it.hasta.formatearRelativo(estado.hoy)}" }
                                ?: "Elegir fechas",
                        )
                    },
                )
            }

            when {
                estado.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                !estado.hayMovimientos -> EstadoVacio(
                    titulo = "Aún no hay movimientos",
                    mensaje = "Cuando registres ventas y gastos, aparecerán aquí agrupados por día.",
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        Button(onClick = onRegistrarVenta) { Text("Registrar venta") }
                        Button(onClick = onRegistrarGasto) { Text("Registrar gasto") }
                    }
                }
                estado.grupos.isEmpty() -> EstadoVacio(
                    titulo = "Nada con estos filtros",
                    mensaje = "Prueba con otras fechas u otro tipo de movimiento.",
                )
                else -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    item(key = "totales") {
                        Text(
                            text = buildString {
                                append("Vendiste ${estado.totalVendido.formatear()} · Gastaste ${estado.totalGastado.formatear()}")
                                if (estado.totalIngresosPersonales > Monto.CERO) {
                                    append(" · Ingresos personales ${estado.totalIngresosPersonales.formatear()}")
                                }
                            },
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                        )
                    }
                    estado.grupos.forEach { grupo ->
                        item(key = "dia-${grupo.fecha}") {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = grupo.fecha.formatearRelativo(estado.hoy),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = listOfNotNull(
                                        grupo.vendido.takeIf { it > Monto.CERO }?.let { "+${it.formatear()}" },
                                        grupo.ingresosPersonales.takeIf { it > Monto.CERO }?.let { "(personal +${it.formatear()})" },
                                        grupo.gastado.takeIf { it > Monto.CERO }?.let { "−${it.formatear()}" },
                                    ).joinToString("  "),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        items(grupo.movimientos, key = { "${it::class.simpleName}-${it.id}" }) { movimiento ->
                            FilaMovimiento(movimiento, estado.hoy, onClick = { onAbrirMovimiento(movimiento) }, mostrarFecha = false)
                        }
                    }
                }
            }
        }
    }

    if (eligiendoFechas) {
        SelectorRango(
            hoy = estado.hoy,
            onElegir = { desde, hasta ->
                viewModel.onFechas(FiltroFechas.Rango(desde, hasta))
                eligiendoFechas = false
            },
            onCancelar = { eligiendoFechas = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorRango(hoy: LocalDate, onElegir: (LocalDate, LocalDate) -> Unit, onCancelar: () -> Unit) {
    val limite = hoy.toEpochDays() * MILIS_POR_DIA
    val estadoRango = rememberDateRangePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= limite
        },
    )
    DatePickerDialog(
        onDismissRequest = onCancelar,
        confirmButton = {
            TextButton(
                enabled = estadoRango.selectedStartDateMillis != null,
                onClick = {
                    val desde = estadoRango.selectedStartDateMillis ?: return@TextButton
                    val hasta = estadoRango.selectedEndDateMillis ?: desde
                    onElegir(LocalDate.fromEpochDays(desde / MILIS_POR_DIA), LocalDate.fromEpochDays(hasta / MILIS_POR_DIA))
                },
            ) { Text("Aceptar") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    ) {
        DateRangePicker(state = estadoRango, modifier = Modifier.height(500.dp))
    }
}
