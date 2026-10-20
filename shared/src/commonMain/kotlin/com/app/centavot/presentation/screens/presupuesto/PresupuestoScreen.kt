package com.app.centavot.presentation.screens.presupuesto

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.app.centavot.domain.model.Bloque503020
import com.app.centavot.domain.model.EstadoLinea
import com.app.centavot.domain.model.LIMITE_DEUDAS_PORCENTAJE
import com.app.centavot.domain.model.LineaPresupuesto
import com.app.centavot.domain.model.Modo
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.Partida
import com.app.centavot.domain.model.Reparto503020
import com.app.centavot.domain.model.TipoGasto
import com.app.centavot.domain.model.TipoPartida
import com.app.centavot.domain.model.sumar
import com.app.centavot.presentation.components.BarraAvance
import com.app.centavot.presentation.components.EstadoVacio
import com.app.centavot.presentation.components.Iconos
import com.app.centavot.presentation.components.color
import com.app.centavot.presentation.components.comoTextoEditable
import com.app.centavot.presentation.components.esEntradaDeMontoValida
import com.app.centavot.presentation.components.formatear
import kotlinx.datetime.plusMonth
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
            // RP-03 y RP-10: si el plan alcanza y si las deudas pesan demasiado.
            presupuesto.balance?.let { balance -> item { FilaBalance(balance, presupuesto.porcentajeDeudas, presupuesto.deudasAltas) } }
            // RP-05 y RP-06: la referencia 50/30/20 y el avance frente al mes anterior (solo en lo personal).
            if (estado.modo == Modo.PERSONAL) {
                presupuesto.reparto?.let { reparto -> item { Tarjeta503020(reparto, estado.anterior?.reparto) } }
            }
            if (presupuesto.ingresos.isNotEmpty()) {
                seccion(if (estado.modo == Modo.PERSONAL) "Lo que te entra" else "Ventas", presupuesto.ingresos, viewModel::editar)
                if (presupuesto.ingresoSinFuente > Monto.CERO) item { FilaExtra("Sin fuente", presupuesto.ingresoSinFuente) }
                if (presupuesto.deTuNegocio > Monto.CERO) item { FilaExtra("De tu negocio", presupuesto.deTuNegocio) }
            }
            // RP-04: el ahorro va primero; lo real es lo que separaste para tus metas.
            seccion("Ahorro", presupuesto.ahorro, viewModel::editar)
            if (presupuesto.separadoSinPartida > Monto.CERO) item { FilaExtra("Separado para metas", presupuesto.separadoSinPartida) }
            // RP-02: los gastos agrupados por tipo, con su subtotal.
            presupuesto.grupos.forEach { (tipo, lineas) -> seccion(tipo.etiqueta, lineas, viewModel::editar) }
            if (presupuesto.gastoSinPartida > Monto.CERO) item { FilaExtra("Sin partida", presupuesto.gastoSinPartida) }
            item {
                Text(
                    "Toca una partida para cambiarla.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = viewModel::nueva) {
                        Icon(Iconos.Agregar, contentDescription = null)
                        Text("Agregar partida", modifier = Modifier.padding(start = 8.dp))
                    }
                    TextButton(onClick = onArmar) { Text("Ver la plantilla") }
                }
            }
            // RP-07: cierre de mes.
            if (estado.puedeCerrar) {
                item {
                    OutlinedButton(onClick = viewModel::pedirCierre, modifier = Modifier.fillMaxWidth()) {
                        Text("Usar lo real en ${estado.mes.plusMonth().formatear().lowercase()}")
                    }
                }
            }
        }
    }

    if (estado.confirmandoCierre) {
        val siguiente = estado.mes.plusMonth().formatear().lowercase()
        AlertDialog(
            onDismissRequest = viewModel::cancelarCierre,
            title = { Text("¿Usar lo real en $siguiente?") },
            text = { Text("Cada partida con gastos queda en $siguiente con lo que gastaste en ${estado.mes.formatear().lowercase()}.") },
            confirmButton = { TextButton(onClick = viewModel::usarRealComoPresupuesto) { Text("Usar lo real") } },
            dismissButton = { TextButton(onClick = viewModel::cancelarCierre) { Text("Cancelar") } },
        )
    }

    when (val actual = edicion) {
        is Edicion.Existente -> DialogoPartida(
            titulo = "Cambiar partida",
            partida = actual.partida,
            montoInicial = actual.monto.takeIf { it > Monto.CERO }?.comoTextoEditable().orEmpty(),
            desde = estado.mes.formatear().lowercase(),
            onGuardar = { nombre, tipo, monto, semanal, deuda -> viewModel.guardarPartida(actual.partida, nombre, monto, tipo, semanal, deuda) },
            onQuitar = { viewModel.quitar(actual.partida) },
            onCancelar = viewModel::cerrarEdicion,
        )
        Edicion.Nueva -> DialogoPartida(
            titulo = "Agregar partida",
            partida = null,
            montoInicial = "",
            desde = estado.mes.formatear().lowercase(),
            onGuardar = viewModel::agregar,
            onQuitar = null,
            onCancelar = viewModel::cerrarEdicion,
        )
        null -> Unit
    }
}

/** El título del grupo con su subtotal: "Gastos fijos · S/ 820.00 de S/ 1,030.00". */
private fun LazyListScope.seccion(titulo: String, lineas: List<LineaPresupuesto>, onEditar: (Partida, Monto) -> Unit) {
    if (lineas.isEmpty()) return
    item {
        val real = lineas.map { it.real }.sumar()
        val plan = lineas.map { it.presupuesto }.sumar()
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).semantics { heading() })
            Text(
                if (plan > Monto.CERO) "${real.formatear()} de ${plan.formatear()}" else real.formatear(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    lineas.forEach { linea -> item(key = linea.partida.id) { FilaLinea(linea, onEditar) } }
}

@Composable
private fun FilaLinea(linea: LineaPresupuesto, onEditar: (Partida, Monto) -> Unit) {
    val esIngreso = linea.partida.tipo == TipoPartida.INGRESO
    val esAhorro = linea.partida.tipo == TipoPartida.GASTO && linea.partida.clase == TipoGasto.AHORRO
    // Solo lo que pide atención: pasarse en un gasto. Lo demás lo dicen el monto y la barra.
    val pasado = linea.estado == EstadoLinea.PASADO && !esIngreso && !esAhorro
    val nota = when {
        pasado && linea.montoSemanal != null -> "Te pasaste ${Monto(-linea.diferencia.centimos).formatear()} esta semana"
        pasado -> "Te pasaste ${Monto(-linea.diferencia.centimos).formatear()}"
        else -> null
    }
    // En un ingreso o en el ahorro, pasarse es bueno: no se pinta de rojo.
    val color = if ((esIngreso || esAhorro) && linea.estado != EstadoLinea.SIN_MONTO) MaterialTheme.colorScheme.primary else linea.estado.color()
    // RP-08: en una partida semanal se ve la semana, que se reinicia el lunes.
    val montoTexto = when {
        linea.montoSemanal != null && linea.realSemana != null -> "${linea.realSemana.formatear()} de ${linea.montoSemanal.formatear()} esta semana"
        linea.presupuesto > Monto.CERO -> "${linea.real.formatear()} de ${linea.presupuesto.formatear()}"
        else -> linea.real.formatear()
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = "cambiar") { onEditar(linea.partida, linea.montoSemanal ?: linea.presupuesto) }
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(linea.partida.nombre, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(montoTexto, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        }
        BarraAvance(linea.porcentaje, color, "${linea.partida.nombre}: ${linea.porcentaje} por ciento")
        nota?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
    }
}

/** RP-03: lo que sobra o falta en el plan del mes. RP-10: si las cuotas pasan el 30 % de lo que entra. */
@Composable
private fun FilaBalance(balance: Monto, porcentajeDeudas: Int?, deudasAltas: Boolean) {
    val colores = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val (texto, color) = when {
            balance > Monto.CERO -> "Tu plan deja ${balance.formatear()} sin destino: pásalos a ahorro." to colores.onSurface
            balance < Monto.CERO -> "Tu plan gasta ${Monto(-balance.centimos).formatear()} más de lo que entra: recorta prescindibles." to colores.error
            else -> "Cada sol de tu plan tiene destino." to colores.primary
        }
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = color)
        if (deudasAltas && porcentajeDeudas != null) {
            Text(
                "Tus cuotas son el $porcentajeDeudas % de lo que entra. Lo sano es hasta el $LIMITE_DEUDAS_PORCENTAJE %.",
                style = MaterialTheme.typography.bodyMedium,
                color = colores.error,
            )
        }
    }
}

/** RP-05 y RP-06: el reparto del plan frente a 50/30/20, y cuánto avanzó el ahorro desde el mes anterior. */
@Composable
private fun Tarjeta503020(reparto: Reparto503020, anterior: Reparto503020?) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Tu reparto frente a 50/30/20", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            Bloque503020.entries.forEach { bloque ->
                val tuyo = reparto.de(bloque)
                Row(Modifier.fillMaxWidth()) {
                    Text(bloque.etiqueta, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text("$tuyo %", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        " / ${bloque.referencia} %",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // "De menos a más": no hace falta llegar de golpe; se mira el avance del ahorro mes a mes.
            if (reparto.ahorro < Bloque503020.AHORRO.referencia) {
                val texto = when {
                    anterior != null && reparto.ahorro > anterior.ahorro -> "Ahorras ${reparto.ahorro} %; el mes pasado, ${anterior.ahorro} %. Vas bien."
                    else -> "Sube tu ahorro de a pocos hasta el 20 %."
                }
                Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun FilaExtra(etiqueta: String, monto: Monto) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(monto.formatear(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Nueva partida o cambio de una existente: nombre, grupo (ingreso, ahorro, fijo, variable, prescindible),
 * monto, si es por semana (RP-08) y si es una deuda (RP-10).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DialogoPartida(
    titulo: String,
    partida: Partida?,
    montoInicial: String,
    desde: String,
    onGuardar: (nombre: String, tipoGasto: TipoGasto?, monto: String, semanal: Boolean, deuda: Boolean) -> Unit,
    onQuitar: (() -> Unit)?,
    onCancelar: () -> Unit,
) {
    val esIngresoExistente = partida?.tipo == TipoPartida.INGRESO
    var nombre by remember { mutableStateOf(partida?.nombre.orEmpty()) }
    var monto by remember { mutableStateOf(montoInicial) }
    // null = ingreso.
    var grupo by remember { mutableStateOf(if (partida == null) TipoGasto.PRESCINDIBLE else if (esIngresoExistente) null else partida.clase) }
    var semanal by remember { mutableStateOf(partida?.semanal ?: false) }
    var deuda by remember { mutableStateOf(partida?.deuda ?: false) }
    var confirmarQuitar by remember { mutableStateOf(false) }
    if (confirmarQuitar && onQuitar != null) {
        AlertDialog(
            onDismissRequest = { confirmarQuitar = false },
            title = { Text("¿Quitar ${partida?.nombre.orEmpty()}?") },
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
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it.take(40) },
                    label = { Text("Nombre") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                // Una fuente de ingreso no cambia a gasto (ni al revés): sus movimientos ya están anotados así.
                if (!esIngresoExistente) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val opciones = if (partida == null) listOf<TipoGasto?>(null) + TipoGasto.entries else TipoGasto.entries
                        opciones.forEach { opcion ->
                            FilterChip(selected = grupo == opcion, onClick = { grupo = opcion }, label = { Text(opcion?.etiqueta ?: "Ingreso") })
                        }
                    }
                }
                OutlinedTextField(
                    value = monto,
                    onValueChange = { if (esEntradaDeMontoValida(it)) monto = it },
                    label = { Text(if (semanal) "¿Cuánto por semana?" else "¿Cuánto al mes?") },
                    prefix = { Text("S/ ") },
                    placeholder = { Text("0.00") },
                    supportingText = { Text("Desde $desde en adelante") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                if (grupo == TipoGasto.PRESCINDIBLE || grupo == TipoGasto.VARIABLE) {
                    FilaCasilla("Monto por semana", semanal) { semanal = it }
                }
                if (grupo == TipoGasto.FIJO) {
                    FilaCasilla("Es pago de una deuda", deuda) { deuda = it }
                }
                if (onQuitar != null) {
                    TextButton(
                        onClick = { confirmarQuitar = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text("Quitar del presupuesto") }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val esSemanal = semanal && (grupo == TipoGasto.PRESCINDIBLE || grupo == TipoGasto.VARIABLE)
                    onGuardar(nombre, grupo, monto, esSemanal, deuda && grupo == TipoGasto.FIJO)
                },
                enabled = nombre.isNotBlank(),
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    )
}

@Composable
private fun FilaCasilla(texto: String, marcada: Boolean, onCambiar: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(value = marcada, role = Role.Checkbox, onValueChange = onCambiar),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = marcada, onCheckedChange = null)
        Text(texto, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
    }
}
