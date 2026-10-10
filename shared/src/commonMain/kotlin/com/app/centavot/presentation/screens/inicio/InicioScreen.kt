package com.app.centavot.presentation.screens.inicio

import com.app.centavot.domain.model.NivelAlerta
import com.app.centavot.domain.model.EstadoTope
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.centavot.domain.model.Modo
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.Periodo
import com.app.centavot.domain.model.ResumenPeriodo
import com.app.centavot.domain.model.ResumenUso
import com.app.centavot.presentation.components.FilaMovimiento
import com.app.centavot.presentation.components.GraficoDistribucion
import com.app.centavot.presentation.components.Iconos
import com.app.centavot.presentation.components.Movimiento
import com.app.centavot.presentation.components.ParteGrafico
import com.app.centavot.presentation.components.SelectorModo
import com.app.centavot.presentation.components.TarjetaTope
import com.app.centavot.presentation.components.icono
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.semantics.heading
import com.app.centavot.presentation.components.formatear
import org.koin.compose.viewmodel.koinViewModel

private const val MAX_DEUDORES = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InicioScreen(
    onRegistrarVenta: () -> Unit,
    onRegistrarGasto: () -> Unit,
    onRegistrarRetiro: () -> Unit,
    onAbrirMovimiento: (Movimiento) -> Unit,
    onVerMovimientos: () -> Unit,
    onVerCobros: () -> Unit,
    onAbrirNotificaciones: () -> Unit,
    onAbrirAjustes: () -> Unit,
    onAbrirAyuda: () -> Unit,
    viewModel: InicioViewModel = koinViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = estado.perfil?.nombre?.let { "Hola, ${it.substringBefore(' ')}" } ?: "Centavot",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                actions = {
                    IconButton(onClick = onAbrirNotificaciones) {
                        BadgedBox(
                            badge = {
                                if (estado.noLeidas > 0) Badge { Text(if (estado.noLeidas > 9) "9+" else "${estado.noLeidas}") }
                            },
                        ) {
                            val descripcion = if (estado.noLeidas > 0) {
                                "Notificaciones, ${estado.noLeidas} sin leer"
                            } else {
                                "Notificaciones"
                            }
                            Icon(Iconos.Notificaciones, contentDescription = descripcion)
                        }
                    }
                    IconButton(onClick = onAbrirAyuda) { Icon(Iconos.Ayuda, contentDescription = "Cómo usar Centavot") }
                    IconButton(onClick = onAbrirAjustes) { Icon(Iconos.Ajustes, contentDescription = "Ajustes") }
                },
            )
        },
    ) { padding ->
        if (estado.cargando) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        var pestana by rememberSaveable { mutableStateOf(PestanaInicio.HOY) }
        var eligiendoModo by rememberSaveable { mutableStateOf(false) }
        val alertaTope = estado.tope?.takeIf { it.nivelAlerta != NivelAlerta.NINGUNA }
        val modo = estado.modo

        if (eligiendoModo) {
            HojaModo(
                actual = modo,
                onElegir = { nuevo ->
                    eligiendoModo = false
                    if (nuevo != modo) {
                        pestana = PestanaInicio.HOY
                        viewModel.onCambiarModo(nuevo)
                    }
                },
                onCerrar = { eligiendoModo = false },
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { ChipModo(modo, onClick = { eligiendoModo = true }) }

            item { BotonesRegistro(modo, onRegistrarVenta, onRegistrarGasto) }

            item { PestanasInicio(modo, pestana, alerta = alertaTope?.nivelAlerta, onCambiar = { pestana = it }) }

            when (pestana) {
                PestanaInicio.HOY -> {
                    estado.uso?.takeIf { it.debePreguntarCuaderno }?.let {
                        item { TarjetaPreguntaCuaderno(onResponder = viewModel::responderCuaderno) }
                    }

                    if (!estado.hayMovimientos) {
                        item { PrimerosPasos(modo, onRegistrarVenta, onRegistrarGasto, onAbrirAyuda) }
                    }

                    alertaTope?.let { tope ->
                        item { AvisoTopeCorto(tope, onVer = { pestana = PestanaInicio.RESUMEN }) }
                    }

                    item {
                        TarjetaCaja(
                            modo = modo,
                            periodo = estado.periodo,
                            caja = estado.caja,
                            uso = estado.uso?.takeIf { it.primerUso != null && estado.hayMovimientos },
                            onPeriodo = viewModel::onPeriodo,
                            onRegistrarRetiro = onRegistrarRetiro,
                        )
                    }

                    if (estado.recientes.isNotEmpty()) {
                        item {
                            Seccion("Últimos movimientos", accion = "Ver todos", onAccion = onVerMovimientos) {
                                Column {
                                    estado.recientes.forEach { movimiento ->
                                        FilaMovimiento(movimiento, estado.hoy, onClick = { onAbrirMovimiento(movimiento) }, modo = modo)
                                    }
                                }
                            }
                        }
                    }
                }

                PestanaInicio.RESUMEN -> {
                    estado.tope?.let { tope -> item { TarjetaTope(tope) } }

                    estado.cobros?.takeIf { it.porContacto.isNotEmpty() }?.let { cobros ->
                        item {
                            Seccion(
                                titulo = "Te deben ${cobros.totalPendiente.formatear()}",
                                accion = "Ver cobros",
                                onAccion = onVerCobros,
                            ) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    cobros.porContacto.take(MAX_DEUDORES).forEach { deuda ->
                                        Row(Modifier.fillMaxWidth()) {
                                            Text(deuda.contacto.nombre, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(deuda.total.formatear(), fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    val otros = cobros.porContacto.size - MAX_DEUDORES
                                    if (otros > 0) {
                                        Text(
                                            text = "y $otros ${if (otros == 1) "persona más" else "personas más"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item { TarjetaAhorro(estado, onAbrirAjustes) }

                    estado.gastosMes?.takeIf { it.porSubcategoria.isNotEmpty() }?.let { resumen ->
                        item {
                            Seccion("En qué se fue tu plata este mes") {
                                GraficoDistribucion(
                                    partes = resumen.porSubcategoria.map { (subcategoria, monto) ->
                                        ParteGrafico(subcategoria?.etiqueta ?: "Sin categoría", monto)
                                    },
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Inicio separa lo del día de la foto del mes (del negocio o de la plata personal), para no mostrar todo a la vez. */
private enum class PestanaInicio { HOY, RESUMEN }

private fun PestanaInicio.titulo(modo: Modo): String = when (this) {
    PestanaInicio.HOY -> "Hoy"
    PestanaInicio.RESUMEN -> if (modo == Modo.NEGOCIO) "Mi negocio" else "Mi mes"
}

/** Dice en qué modo está y lo cambia: lo personal y el negocio no se mezclan en pantalla. */
@Composable
private fun ChipModo(modo: Modo, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(modo.etiqueta) },
        leadingIcon = { Icon(modo.icono, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize)) },
        trailingIcon = { Icon(Iconos.Desplegar, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize)) },
        modifier = Modifier.semantics { contentDescription = "Estás en ${modo.etiqueta}. Tocar para cambiar de modo" },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HojaModo(actual: Modo, onElegir: (Modo) -> Unit, onCerrar: () -> Unit) {
    // Abierta del todo: a media altura la segunda opción quedaba cortada.
    ModalBottomSheet(onDismissRequest = onCerrar, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("¿Qué quieres ver?", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            Text(
                "Al cambiar no se borra nada: lo del otro modo vuelve a aparecer cuando regreses a él.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SelectorModo(actual, onElegir)
        }
    }
}

@Composable
private fun PestanasInicio(modo: Modo, actual: PestanaInicio, alerta: NivelAlerta?, onCambiar: (PestanaInicio) -> Unit) {
    val conAlerta = alerta != null
    val colorPunto = if (alerta == NivelAlerta.TOPE_ALCANZADO) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
    PrimaryTabRow(selectedTabIndex = actual.ordinal, containerColor = Color.Transparent) {
        PestanaInicio.entries.forEach { pestana ->
            Tab(
                selected = pestana == actual,
                onClick = { onCambiar(pestana) },
                text = {
                    if (pestana == PestanaInicio.RESUMEN && conAlerta) {
                        BadgedBox(badge = { Badge(containerColor = colorPunto) }) { Text(pestana.titulo(modo)) }
                    } else {
                        Text(pestana.titulo(modo))
                    }
                },
                modifier = Modifier.semantics {
                    if (pestana == PestanaInicio.RESUMEN && conAlerta) contentDescription = "Mi negocio, tienes un aviso de tope"
                },
            )
        }
    }
}

/** En "Hoy", el aviso del tope cabe en una línea y lleva a "Mi negocio". */
@Composable
private fun AvisoTopeCorto(tope: EstadoTope, onVer: () -> Unit) {
    val (medida, proximidad) = tope.principal
    val alcanzado = proximidad.nivelAlerta == NivelAlerta.TOPE_ALCANZADO
    Card(
        onClick = onVer,
        colors = CardDefaults.cardColors(
            containerColor = if (alcanzado) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = if (alcanzado) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Iconos.Aviso, contentDescription = null)
            Text(
                "Tus ${medida.etiqueta} van en ${proximidad.porcentaje} % de tu tope",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text("Ver", fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Las dos acciones más frecuentes, grandes y arriba: una decisión, un toque. */
@Composable
private fun BotonesRegistro(modo: Modo, onRegistrarEntrada: () -> Unit, onRegistrarGasto: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = onRegistrarEntrada, modifier = Modifier.weight(1f).height(64.dp)) {
            Icon(Iconos.Venta, contentDescription = null)
            Text(if (modo == Modo.NEGOCIO) "Venta" else "Ingreso", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp))
        }
        FilledTonalButton(onClick = onRegistrarGasto, modifier = Modifier.weight(1f).height(64.dp)) {
            Icon(Iconos.Gasto, contentDescription = null)
            Text("Gasto", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

/** Etiquetas cortas para que el selector entre en una línea, incluso con texto grande. */
private val Periodo.etiquetaCorta: String
    get() = when (this) {
        Periodo.HOY -> "Hoy"
        Periodo.SEMANA -> "Semana"
        Periodo.MES -> "Mes"
    }

/**
 * Cierre de caja por día, semana o mes. En el negocio: lo vendido, lo gastado, la ganancia y lo
 * que se sacó para la casa. En lo personal: lo que entró, lo que se gastó y lo que quedó.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TarjetaCaja(
    modo: Modo,
    periodo: Periodo,
    caja: ResumenPeriodo,
    uso: ResumenUso?,
    onPeriodo: (Periodo) -> Unit,
    onRegistrarRetiro: () -> Unit,
) {
    val colores = MaterialTheme.colorScheme
    Card(colors = CardDefaults.cardColors(containerColor = colores.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (modo == Modo.NEGOCIO) "Tu caja" else "Tu plata", style = MaterialTheme.typography.titleMedium)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Periodo.entries.forEachIndexed { i, opcion ->
                    SegmentedButton(
                        selected = opcion == periodo,
                        onClick = { onPeriodo(opcion) },
                        shape = SegmentedButtonDefaults.itemShape(index = i, count = Periodo.entries.size),
                    ) { Text(opcion.etiquetaCorta, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
            }
            when (modo) {
                Modo.NEGOCIO -> {
                    FilaCaja("Vendiste", caja.ventas, colores.primary)
                    FilaCaja("Gastaste en el negocio", caja.gastosNegocio, colores.onSurface)
                    HorizontalDivider(color = colores.outlineVariant)
                    FilaResultado(caja.ganancia, positivo = "Te quedó (ganancia)", negativo = "Perdiste")
                    if (caja.retiros > Monto.CERO) {
                        FilaCaja("Sacaste para la casa", caja.retiros, colores.onSurface)
                        FilaResultado(caja.quedaEnCaja, positivo = "Queda en caja", negativo = "Falta en caja")
                    }
                    OutlinedButton(onClick = onRegistrarRetiro, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Icon(Iconos.Inicio, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Saqué para la casa", modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Modo.PERSONAL -> {
                    FilaCaja("Te entró", caja.ingresosPersonales, colores.primary)
                    FilaCaja("Gastaste", caja.gastosPersonales, colores.onSurface)
                    HorizontalDivider(color = colores.outlineVariant)
                    FilaResultado(caja.saldoPersonal, positivo = "Te quedó", negativo = "Gastaste de más")
                }
            }
            uso?.let { LineaConstancia(it) }
        }
    }
}

/** El resultado destacado: en rojo y sin signo cuando es negativo, con su propia etiqueta. */
@Composable
private fun FilaResultado(monto: Monto, positivo: String, negativo: String) {
    val colores = MaterialTheme.colorScheme
    FilaCaja(
        titulo = if (monto < Monto.CERO) negativo else positivo,
        monto = Monto(kotlin.math.abs(monto.centimos)),
        color = if (monto < Monto.CERO) colores.error else colores.onSurface,
        destacado = true,
    )
}

@Composable
private fun FilaCaja(titulo: String, monto: Monto, color: Color, destacado: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            titulo,
            style = if (destacado) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            monto.formatear(),
            style = if (destacado) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

/** Refuerza el hábito: el valor de la app aparece con el uso diario, no en un día. */
@Composable
private fun LineaConstancia(uso: ResumenUso) {
    val dias = uso.diasConRegistroUltimos7
    val mensaje = when {
        dias >= 5 -> "¡Bien! Anotaste $dias de los últimos 7 días."
        dias > 0 -> "Anotaste $dias de los últimos 7 días. Anotar cada día hace que tus cuentas cuadren."
        else -> "Esta semana no anotaste nada. Un minuto al cerrar el día basta."
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Iconos.Calendario, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        Text(mensaje, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** "¿Te resulta más fácil que tu cuaderno?": la prueba de fondo de si la app sirve. */
@Composable
private fun TarjetaPreguntaCuaderno(onResponder: (Boolean) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Una pregunta rápida", style = MaterialTheme.typography.titleMedium)
            Text(
                "¿Te resulta más fácil llevar tus cuentas con Centavot que con tu cuaderno y la calculadora?",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { onResponder(true) }, modifier = Modifier.weight(1f).height(48.dp)) { Text("Sí, más fácil") }
                OutlinedButton(onClick = { onResponder(false) }, modifier = Modifier.weight(1f).height(48.dp)) { Text("Todavía no") }
            }
        }
    }
}

private fun pasosDe(modo: Modo): List<String> = when (modo) {
    Modo.NEGOCIO -> listOf(
        "1. Cada vez que vendas algo, toca Venta y pon el monto.",
        "2. Cuando compres mercadería o pagues algo, toca Gasto.",
        "3. Al cerrar el día, mira tu caja: cuánto vendiste y cuánto te quedó.",
    )
    Modo.PERSONAL -> listOf(
        "1. Cuando te paguen o te entre plata, toca Ingreso y pon el monto.",
        "2. Cuando pagues algo (pasaje, comida, luz), toca Gasto.",
        "3. Al final del día, mira tu plata: cuánto entró y cuánto te quedó.",
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrimerosPasos(modo: Modo, onRegistrarEntrada: () -> Unit, onRegistrarGasto: () -> Unit, onAbrirAyuda: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Empieza en 3 pasos", style = MaterialTheme.typography.titleMedium)
            pasosDe(modo).forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
            Text(
                "¿Sigues usando tu cuaderno? No pasa nada: al final del día pasa aquí los totales.",
                style = MaterialTheme.typography.bodySmall,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onRegistrarEntrada) { Text(if (modo == Modo.NEGOCIO) "Mi primera venta" else "Mi primer ingreso") }
                TextButton(onClick = onRegistrarGasto) { Text("Mi primer gasto") }
                TextButton(onClick = onAbrirAyuda) { Text("Ver ayuda") }
            }
        }
    }
}

/** La meta de ahorro del mes; lo que te deben ya está en su propia sección. */
@Composable
private fun TarjetaAhorro(estado: InicioUiState, onAbrirAjustes: () -> Unit) {
    val perfil = estado.perfil
    val tasa = perfil?.tasaAhorro?.formatear() ?: "0 %"
    val sinMeta = estado.baseAhorro <= Monto.CERO || perfil?.tasaAhorro?.decimas == 0
    Card(
        onClick = onAbrirAjustes,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                when {
                    !estado.baseAhorroEsGanancia -> "Meta de ahorro del mes ($tasa)"
                    estado.modo == Modo.NEGOCIO -> "Ahorro del mes ($tasa de tu ganancia)"
                    else -> "Ahorro del mes ($tasa de lo que te entró)"
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                estado.metaAhorro.formatear(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            if (sinMeta) {
                Text(
                    "Toca aquí para definir cuánto quieres ahorrar",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Seccion(
    titulo: String,
    accion: String? = null,
    onAccion: () -> Unit = {},
    contenido: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (accion != null) TextButton(onClick = onAccion) { Text(accion) }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) { contenido() }
    }
}
