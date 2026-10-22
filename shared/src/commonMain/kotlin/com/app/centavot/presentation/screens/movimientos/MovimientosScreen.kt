package com.app.centavot.presentation.screens.movimientos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.centavot.domain.model.Modo
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
            // Qué movimientos: una fila que se desliza, con margen a los dos lados para que el último chip se vea entero.
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(FiltroMovimientos.para(estado.modo)) { filtro ->
                    FilterChip(
                        selected = estado.filtro == filtro,
                        onClick = { viewModel.onFiltro(filtro) },
                        label = { Text(filtro.etiqueta(estado.modo)) },
                    )
                }
            }
            // De cuándo: un solo botón con menú, en vez de una segunda fila de chips que se salía de la pantalla.
            SelectorPeriodo(
                fechas = estado.fechas,
                hoy = estado.hoy,
                onElegir = viewModel::onFechas,
                onElegirFechas = { eligiendoFechas = true },
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            when {
                estado.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                !estado.hayMovimientos -> EstadoVacio(
                    titulo = "Aún no hay movimientos",
                    mensaje = if (estado.modo == Modo.NEGOCIO) {
                        "Cuando registres ventas y gastos, aparecerán aquí agrupados por día."
                    } else {
                        "Cuando registres lo que te entra y lo que gastas, aparecerá aquí agrupado por día."
                    },
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        Button(onClick = onRegistrarVenta) { Text(if (estado.modo == Modo.NEGOCIO) "Registrar venta" else "Registrar ingreso") }
                        Button(onClick = onRegistrarGasto) { Text("Registrar gasto") }
                    }
                }
                estado.grupos.isEmpty() -> EstadoVacio(
                    titulo = "Nada con estos filtros",
                    mensaje = "Prueba con otras fechas u otro tipo de movimiento.",
                )
                else -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    item(key = "totales") { Totales(estado) }
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
                                        grupo.entro.takeIf { it > Monto.CERO }?.let { "+${it.formatear()}" },
                                        grupo.salio.takeIf { it > Monto.CERO }?.let { "−${it.formatear()}" },
                                    ).joinToString("  "),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        items(grupo.movimientos, key = { "${it::class.simpleName}-${it.id}" }) { movimiento ->
                            FilaMovimiento(movimiento, estado.hoy, onClick = { onAbrirMovimiento(movimiento) }, mostrarFecha = false, modo = estado.modo)
                        }
                    }
                }
            }
        }
    }

    if (eligiendoFechas) {
        SelectorRango(
            hoy = estado.hoy,
            actual = estado.fechas as? FiltroFechas.Rango,
            onElegir = { desde, hasta ->
                viewModel.onFechas(FiltroFechas.Rango(desde, hasta))
                eligiendoFechas = false
            },
            onCancelar = { eligiendoFechas = false },
        )
    }
}

/** El botón del período: muestra el elegido ("Este mes", "5 – 12 oct") y abre un menú con las opciones. */
@Composable
private fun SelectorPeriodo(
    fechas: FiltroFechas,
    hoy: LocalDate,
    onElegir: (FiltroFechas) -> Unit,
    onElegirFechas: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var abierto by remember { mutableStateOf(false) }
    Box(modifier) {
        FilterChip(
            selected = fechas != FiltroFechas.Todo,
            onClick = { abierto = true },
            leadingIcon = { Icon(Iconos.Calendario, contentDescription = null, modifier = Modifier.size(18.dp)) },
            trailingIcon = { Icon(Iconos.Desplegar, contentDescription = null, modifier = Modifier.size(18.dp)) },
            label = { Text(if (fechas == FiltroFechas.Todo) "Todas las fechas" else fechas.etiquetaEn(hoy)) },
        )
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            val opciones = listOf(FiltroFechas.Todo) + Periodo.entries.map { FiltroFechas.Rapido(it) }
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(if (opcion == FiltroFechas.Todo) "Todas las fechas" else opcion.etiqueta) },
                    leadingIcon = { if (fechas == opcion) Icon(Iconos.Correcto, contentDescription = "Elegido") },
                    onClick = {
                        abierto = false
                        onElegir(opcion)
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(if (fechas is FiltroFechas.Rango) "Otras fechas (${fechas.etiquetaEn(hoy)})" else "Elegir fechas…") },
                leadingIcon = {
                    if (fechas is FiltroFechas.Rango) Icon(Iconos.Correcto, contentDescription = "Elegido") else Icon(Iconos.Calendario, contentDescription = null)
                },
                onClick = {
                    abierto = false
                    onElegirFechas()
                },
            )
        }
    }
}

/** Lo que entró, lo que se gastó y (en el negocio) lo que fue para la casa, en columnas que se leen de un vistazo. */
@Composable
private fun Totales(estado: MovimientosUiState) {
    val columnas = buildList {
        add((if (estado.modo == Modo.NEGOCIO) "Vendiste" else "Te entró") to estado.totalEntro)
        add("Gastaste" to estado.totalGastado)
        if (estado.totalParaLaCasa > Monto.CERO) add("Para la casa" to estado.totalParaLaCasa)
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            columnas.forEach { (etiqueta, monto) ->
                Column(Modifier.weight(1f)) {
                    Text(etiqueta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(monto.formatear(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }
    }
}

/**
 * Elegir un rango de fechas en pantalla completa, como pide Material 3 para rangos: el calendario entero se ve
 * y el título es corto (el de la versión en diálogo no cabía y se partía letra por letra).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorRango(hoy: LocalDate, actual: FiltroFechas.Rango?, onElegir: (LocalDate, LocalDate) -> Unit, onCancelar: () -> Unit) {
    val limite = hoy.toEpochDays() * MILIS_POR_DIA
    val estadoRango = rememberDateRangePickerState(
        initialSelectedStartDateMillis = actual?.desde?.toEpochDays()?.times(MILIS_POR_DIA),
        initialSelectedEndDateMillis = actual?.hasta?.toEpochDays()?.times(MILIS_POR_DIA),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= limite
        },
    )
    val desde = estadoRango.selectedStartDateMillis?.let { LocalDate.fromEpochDays(it / MILIS_POR_DIA) }
    val hasta = estadoRango.selectedEndDateMillis?.let { LocalDate.fromEpochDays(it / MILIS_POR_DIA) }
    Dialog(onDismissRequest = onCancelar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
            Column {
                TopAppBar(
                    title = { Text("Elige las fechas") },
                    navigationIcon = { IconButton(onClick = onCancelar) { Icon(Iconos.Quitar, contentDescription = "Cancelar") } },
                    actions = {
                        TextButton(enabled = desde != null, onClick = { desde?.let { onElegir(it, hasta ?: it) } }) { Text("Listo") }
                    },
                )
                DateRangePicker(
                    state = estadoRango,
                    title = null,
                    headline = {
                        Text(
                            text = when {
                                desde == null -> "Toca el primer día"
                                hasta == null -> "${etiquetaRango(desde, desde, hoy)} · toca el último día"
                                else -> etiquetaRango(desde, hasta, hoy)
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp),
                        )
                    },
                    showModeToggle = false,
                    // Mismo fondo Lana que la barra de arriba: una sola superficie, no dos bandas.
                    colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
