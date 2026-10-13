package com.app.centavot.presentation.screens.presupuesto

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.centavot.domain.model.EstadoLinea
import com.app.centavot.domain.model.LineaPresupuesto
import com.app.centavot.domain.model.Modo
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.Partida
import com.app.centavot.domain.model.TipoPartida
import com.app.centavot.presentation.components.BarraAvance
import com.app.centavot.presentation.components.EstadoVacio
import com.app.centavot.presentation.components.Iconos
import com.app.centavot.presentation.components.color
import com.app.centavot.presentation.components.comoTextoEditable
import com.app.centavot.presentation.components.esEntradaDeMontoValida
import com.app.centavot.presentation.components.formatear
import org.koin.compose.viewmodel.koinViewModel

/** El presupuesto del mes frente a lo que de verdad entró y se gastó. Tocar una partida cambia su monto. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresupuestoScreen(
    onCerrar: () -> Unit,
    onArmar: () -> Unit,
    viewModel: PresupuestoViewModel = koinViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val edicion by viewModel.edicion.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tu presupuesto") },
                navigationIcon = { IconButton(onClick = onCerrar) { Icon(Iconos.Atras, contentDescription = "Volver") } },
            )
        },
    ) { padding ->
        val presupuesto = estado.presupuesto
        if (estado.cargando || presupuesto == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!presupuesto.hayPartidas) {
                item {
                    EstadoVacio(
                        titulo = "Todavía no tienes presupuesto",
                        mensaje = "Marca en una lista lo que tienes cada mes. Toma un par de minutos.",
                    ) { Button(onClick = onArmar) { Text("Armar mi presupuesto") } }
                }
                return@LazyColumn
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = viewModel::mesAnterior) { Icon(Iconos.Anterior, contentDescription = "Mes anterior") }
                    Text(estado.mes.formatear(), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    IconButton(onClick = viewModel::mesSiguiente, enabled = estado.puedeAvanzar) {
                        Icon(Iconos.Siguiente, contentDescription = "Mes siguiente")
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            Text("Gastaste", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text(
                                if (presupuesto.presupuestoGastos > Monto.CERO) {
                                    "${presupuesto.realGastos.formatear()} de ${presupuesto.presupuestoGastos.formatear()}"
                                } else {
                                    presupuesto.realGastos.formatear()
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        val porcentaje = if (presupuesto.presupuestoGastos > Monto.CERO) {
                            (presupuesto.realGastos.centimos * 100 / presupuesto.presupuestoGastos.centimos).toInt()
                        } else {
                            0
                        }
                        val estadoTotal = when {
                            presupuesto.presupuestoGastos <= Monto.CERO -> EstadoLinea.SIN_MONTO
                            presupuesto.realGastos > presupuesto.presupuestoGastos -> EstadoLinea.PASADO
                            porcentaje >= 90 -> EstadoLinea.CERCA
                            else -> EstadoLinea.BIEN
                        }
                        BarraAvance(porcentaje, estadoTotal.color(), if (presupuesto.presupuestoGastos > Monto.CERO) "Gastaste el $porcentaje por ciento de tu presupuesto" else "Gastaste ${presupuesto.realGastos.formatear()}")
                        presupuesto.lectura?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            if (estado.modo == Modo.PERSONAL && presupuesto.ingresos.isNotEmpty()) {
                seccion("Lo que te entra", presupuesto.ingresos, viewModel::editar)
                if (presupuesto.ingresoSinFuente > Monto.CERO) item { FilaExtra("Sin fuente", presupuesto.ingresoSinFuente) }
                if (presupuesto.deTuNegocio > Monto.CERO) item { FilaExtra("De tu negocio", presupuesto.deTuNegocio) }
            }
            seccion(if (estado.modo == Modo.PERSONAL) "En qué gastas" else "En qué gasta tu negocio", presupuesto.gastos, viewModel::editar)
            if (presupuesto.gastoSinPartida > Monto.CERO) item { FilaExtra("Sin partida", presupuesto.gastoSinPartida) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = viewModel::nueva) {
                        Icon(Iconos.Agregar, contentDescription = null)
                        Text("Agregar partida", modifier = Modifier.padding(start = 8.dp))
                    }
                    TextButton(onClick = onArmar) { Text("Ver la plantilla") }
                }
            }
        }
    }

    when (val actual = edicion) {
        is Edicion.Existente -> DialogoPartida(
            titulo = "Cambiar partida",
            nombreInicial = actual.partida.nombre,
            montoInicial = actual.monto.takeIf { it > Monto.CERO }?.comoTextoEditable().orEmpty(),
            desde = estado.mes.formatear().lowercase(),
            conTipo = false,
            onGuardar = { nombre, _, monto -> viewModel.guardarPartida(actual.partida, nombre, monto) },
            onQuitar = { viewModel.quitar(actual.partida) },
            onCancelar = viewModel::cerrarEdicion,
        )
        Edicion.Nueva -> DialogoPartida(
            titulo = "Agregar partida",
            nombreInicial = "",
            montoInicial = "",
            desde = estado.mes.formatear().lowercase(),
            conTipo = estado.modo == Modo.PERSONAL,
            onGuardar = viewModel::agregar,
            onQuitar = null,
            onCancelar = viewModel::cerrarEdicion,
        )
        null -> Unit
    }
}

private fun LazyListScope.seccion(titulo: String, lineas: List<LineaPresupuesto>, onEditar: (Partida, Monto) -> Unit) {
    if (lineas.isEmpty()) return
    item { Text(titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp).semantics { heading() }) }
    lineas.forEach { linea -> item(key = linea.partida.id) { FilaLinea(linea, onEditar) } }
}

@Composable
private fun FilaLinea(linea: LineaPresupuesto, onEditar: (Partida, Monto) -> Unit) {
    val esIngreso = linea.partida.tipo == TipoPartida.INGRESO
    val nota = when (linea.estado) {
        EstadoLinea.SIN_MONTO -> "Toca para ponerle monto"
        EstadoLinea.PASADO -> if (esIngreso) "Recibiste ${Monto(-linea.diferencia.centimos).formatear()} más" else "Te pasaste ${Monto(-linea.diferencia.centimos).formatear()}"
        else -> if (esIngreso) "Faltan ${linea.diferencia.formatear()}" else "Te quedan ${linea.diferencia.formatear()}"
    }
    // En un ingreso, pasarse es bueno: no se pinta de rojo.
    val color = if (esIngreso && linea.estado != EstadoLinea.SIN_MONTO) MaterialTheme.colorScheme.primary else linea.estado.color()
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = "cambiar") { onEditar(linea.partida, linea.presupuesto) }
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(linea.partida.nombre, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                if (linea.presupuesto > Monto.CERO) "${linea.real.formatear()} de ${linea.presupuesto.formatear()}" else linea.real.formatear(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        BarraAvance(linea.porcentaje, color, "${linea.partida.nombre}: ${linea.porcentaje} por ciento")
        Text(
            nota,
            style = MaterialTheme.typography.bodySmall,
            color = if (linea.estado == EstadoLinea.PASADO && !esIngreso) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun FilaExtra(etiqueta: String, monto: Monto) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(monto.formatear(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoPartida(
    titulo: String,
    nombreInicial: String,
    montoInicial: String,
    desde: String,
    conTipo: Boolean,
    onGuardar: (String, TipoPartida, String) -> Unit,
    onQuitar: (() -> Unit)?,
    onCancelar: () -> Unit,
) {
    var nombre by remember { mutableStateOf(nombreInicial) }
    var monto by remember { mutableStateOf(montoInicial) }
    var tipo by remember { mutableStateOf(TipoPartida.GASTO) }
    var confirmarQuitar by remember { mutableStateOf(false) }
    if (confirmarQuitar && onQuitar != null) {
        AlertDialog(
            onDismissRequest = { confirmarQuitar = false },
            title = { Text("¿Quitar $nombreInicial?") },
            text = { Text("Lo que ya anotaste en ella se queda, sin partida.") },
            confirmButton = {
                TextButton(onClick = onQuitar, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Quitar") }
            },
            dismissButton = { TextButton(onClick = { confirmarQuitar = false }) { Text("Cancelar") } },
        )
        return
    }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (conTipo) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        listOf(TipoPartida.GASTO to "Gasto", TipoPartida.INGRESO to "Ingreso").forEachIndexed { i, (valor, etiqueta) ->
                            SegmentedButton(selected = tipo == valor, onClick = { tipo = valor }, shape = SegmentedButtonDefaults.itemShape(i, 2)) {
                                Text(etiqueta)
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it.take(40) },
                    label = { Text("Nombre") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                OutlinedTextField(
                    value = monto,
                    onValueChange = { if (esEntradaDeMontoValida(it)) monto = it },
                    label = { Text("¿Cuánto al mes?") },
                    prefix = { Text("S/ ") },
                    placeholder = { Text("0.00") },
                    supportingText = { Text("Desde $desde en adelante") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                if (onQuitar != null) {
                    TextButton(
                        onClick = { confirmarQuitar = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text("Quitar del presupuesto") }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onGuardar(nombre, tipo, monto) }, enabled = nombre.isNotBlank()) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    )
}
