package com.app.centavot.presentation.screens.reporte

import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.centavot.domain.model.Historial
import com.app.centavot.domain.model.Modo
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.TipoCobro
import com.app.centavot.domain.model.sumar
import com.app.centavot.presentation.components.EstadoVacio
import com.app.centavot.presentation.components.FilaGasto
import com.app.centavot.presentation.components.FilaIngreso
import com.app.centavot.presentation.components.GraficoLineas
import com.app.centavot.presentation.components.Iconos
import com.app.centavot.presentation.components.formatear
import com.app.centavot.presentation.components.formatearRelativo
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReporteScreen(
    onAbrirGasto: (String) -> Unit,
    onAbrirVenta: (String) -> Unit,
    viewModel: ReporteViewModel = koinViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reportes") },
                actions = {
                    // Con texto: un ícono de compartir solo no dice qué se manda ni a quién.
                    TextButton(onClick = viewModel::exportar, enabled = estado.puedeExportar) {
                        Icon(Iconos.Compartir, contentDescription = null)
                        Text(
                            if (estado.pestana == PestanaReporte.NEGOCIO) "Enviar al contador" else "Compartir",
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            val pestanas = PestanaReporte.para(estado.modo)
            PrimaryTabRow(selectedTabIndex = pestanas.indexOf(estado.pestana).coerceAtLeast(0)) {
                pestanas.forEach { pestana ->
                    Tab(
                        selected = pestana == estado.pestana,
                        onClick = { viewModel.onPestana(pestana) },
                        text = { Text(pestana.etiqueta) },
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (estado.pestana != PestanaReporte.ME_DEBEN) {
                    item { SelectorMes(estado, viewModel::mesAnterior, viewModel::mesSiguiente) }
                }
                if (estado.cargando) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    return@LazyColumn
                }
                when (estado.pestana) {
                    PestanaReporte.NEGOCIO -> contenidoNegocio(estado, onAbrirGasto, onAbrirVenta)
                    PestanaReporte.PERSONAL -> contenidoPersonal(estado, onAbrirGasto, onAbrirVenta)
                    PestanaReporte.ME_DEBEN -> contenidoMeDeben(estado)
                }
            }
        }
    }
}

@Composable
private fun SelectorMes(estado: ReporteUiState, onAnterior: () -> Unit, onSiguiente: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onAnterior) { Icon(Iconos.Anterior, contentDescription = "Mes anterior") }
        Text(
            text = estado.periodo.formatear(),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onSiguiente, enabled = estado.puedeAvanzar) {
            Icon(Iconos.Siguiente, contentDescription = "Mes siguiente")
        }
    }
}

private fun LazyListScope.contenidoNegocio(
    estado: ReporteUiState,
    onAbrirGasto: (String) -> Unit,
    onAbrirVenta: (String) -> Unit,
) {
    val reporte = estado.reporte
    if (reporte == null) {
        item { EstadoVacio("Elige tu régimen", "Está en Ajustes.") }
        return
    }
    item {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Resumen para tu contador · ${reporte.regimen.nombre}", style = MaterialTheme.typography.labelLarge)
                FilaTotal("Ventas", reporte.totalVentas)
                FilaTotal("Compras y gastos", reporte.total)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                FilaTotal(if (reporte.ganancia < Monto.CERO) "Pérdida" else "Ganancia", reporte.ganancia, destacado = true)
            }
        }
    }
    item { AvisoPrivacidad() }
    estado.historial?.takeIf { it.hayDatos }?.let { historial ->
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Cómo te fue mes a mes", style = MaterialTheme.typography.titleMedium)
                    GraficoLineas(historial)
                    lecturasDe(historial).forEach { lectura ->
                        Text("• $lectura", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
    if (reporte.estaVacio) {
        item {
            EstadoVacio(
                titulo = "Sin movimientos del negocio",
                mensaje = "No registraste ventas ni gastos de negocio en ${estado.periodo.formatear().lowercase()}.",
            )
        }
        return
    }
    if (reporte.ventas.isNotEmpty()) {
        item { Text("Ventas (${reporte.ventas.size})", style = MaterialTheme.typography.titleMedium) }
        item {
            Lista {
                reporte.ventas.forEach { venta -> FilaIngreso(venta, estado.hoy, onClick = { onAbrirVenta(venta.id) }) }
            }
        }
    }
    if (reporte.gastos.isNotEmpty()) {
        item { Text("Compras y gastos (${reporte.gastos.size})", style = MaterialTheme.typography.titleMedium) }
        item {
            Lista {
                reporte.gastos.forEach { gasto -> FilaGasto(gasto, estado.hoy, onClick = { onAbrirGasto(gasto.id) }) }
            }
        }
    }
}

/** Frases simples sobre el historial: el comerciante ya lo intuye, aquí lo ve en números. */
private fun lecturasDe(historial: Historial): List<String> = listOfNotNull(
    historial.mejorMes?.let { "Tu mejor mes fue ${it.mes.formatear().lowercase()}: vendiste ${it.ventas.formatear()}." },
    historial.peorMes?.let { "Tu mes más flojo fue ${it.mes.formatear().lowercase()}: ${it.ventas.formatear()}." },
    historial.variacionUltimoMes?.let { variacion ->
        when {
            variacion > 0 -> "Este mes vendes $variacion % más que el pasado."
            variacion < 0 -> "Este mes vendes ${-variacion} % menos que el pasado."
            else -> "Este mes vas igual que el pasado."
        }
    },
)

private fun LazyListScope.contenidoPersonal(
    estado: ReporteUiState,
    onAbrirGasto: (String) -> Unit,
    onAbrirIngreso: (String) -> Unit,
) {
    val gastos = estado.gastosPersonales
    val ingresos = estado.ingresosPersonales
    val entro = ingresos.map { it.monto }.sumar()
    val gasto = gastos.map { it.monto }.sumar()
    item {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilaTotal("Te entró", entro)
                FilaTotal("Gastaste", gasto)
                val quedo = entro - gasto
                FilaTotal(if (quedo < Monto.CERO) "Gastaste de más" else "Te quedó", Monto(kotlin.math.abs(quedo.centimos)), destacado = true)
                Text(
                    "Solo para ti: no es para SUNAT.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
    if (gastos.isEmpty() && ingresos.isEmpty()) {
        item { EstadoVacio("Sin movimientos", "No registraste ingresos ni gastos en ${estado.periodo.formatear().lowercase()}.") }
        return
    }
    if (ingresos.isNotEmpty()) {
        item { Text("Lo que te entró (${ingresos.size})", style = MaterialTheme.typography.titleMedium) }
        item {
            Lista {
                ingresos.forEach { ingreso ->
                    FilaIngreso(ingreso, estado.hoy, onClick = { onAbrirIngreso(ingreso.id) }, modo = Modo.PERSONAL)
                }
            }
        }
    }
    if (gastos.isNotEmpty()) {
        item { Text("Gastos (${gastos.size})", style = MaterialTheme.typography.titleMedium) }
        item { Lista { gastos.forEach { g -> FilaGasto(g, estado.hoy, onClick = { onAbrirGasto(g.id) }) } } }
    }
}

private fun LazyListScope.contenidoMeDeben(estado: ReporteUiState) {
    val resumen = estado.resumenCobros
    item {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Te deben en total", style = MaterialTheme.typography.labelLarge)
                Text(resumen.totalPendiente.formatear(), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                resumen.porContacto.forEach { deuda ->
                    Row(Modifier.fillMaxWidth()) {
                        Text(deuda.contacto.nombre, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(deuda.total.formatear(), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
    if (estado.cobrosPendientes.isEmpty()) {
        item { EstadoVacio("Nadie te debe", if (estado.modo == Modo.NEGOCIO) "Aquí aparecen los fiados y pedidos por cobrar." else "Aquí aparece la plata que prestaste.") }
        return
    }
    item {
        Lista {
            estado.cobrosPendientes.forEach { cobro ->
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    overlineContent = { Text("${cobro.contacto.nombre} · ${cobro.tipo.etiqueta}") },
                    headlineContent = { Text(cobro.motivo, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supportingContent = {
                        Text(
                            buildString {
                                append("Desde ${cobro.fecha.formatearRelativo(estado.hoy).lowercase()}")
                                if (cobro.tipo == TipoCobro.PEDIDO && cobro.adelanto > Monto.CERO) {
                                    append(" · adelantó ${cobro.adelanto.formatear()}")
                                }
                                if (cobro.abonado > Monto.CERO) append(" · abonó ${cobro.abonado.formatear()}")
                            },
                        )
                    },
                    trailingContent = { Text(cobro.saldo.formatear(), fontWeight = FontWeight.SemiBold) },
                )
            }
        }
    }
}

@Composable
private fun FilaTotal(titulo: String, monto: Monto, destacado: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(titulo, style = if (destacado) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            Monto(kotlin.math.abs(monto.centimos)).formatear(),
            style = if (destacado) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun AvisoPrivacidad() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Iconos.Candado, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = "No se envía a SUNAT. Revísalo con tu contador antes de declarar.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Lista(contenido: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column { contenido() }
    }
}
