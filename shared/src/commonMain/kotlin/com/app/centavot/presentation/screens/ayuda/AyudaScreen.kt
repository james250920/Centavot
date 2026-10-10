package com.app.centavot.presentation.screens.ayuda

import com.app.centavot.presentation.components.LogoCentavot
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.app.centavot.presentation.components.Iconos
import com.app.centavot.presentation.components.icono
import com.app.centavot.domain.model.Modo
import com.app.centavot.domain.usecase.ObservarModoUseCase
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject

private data class Paso(val icono: ImageVector, val titulo: String, val texto: String)

private val PASOS_NEGOCIO = listOf(
    Paso(Iconos.Venta, "1. Anota cada venta", "Toca Venta y pon el monto. Si le pones nombre (\"gaseosa\"), la próxima vez la registras con un solo toque."),
    Paso(Iconos.Gasto, "2. Anota tus gastos", "Toca Gasto y pon el monto: mercadería, alquiler, pasajes del negocio. Lo de tu casa va en el modo personal."),
    Paso(
        Iconos.Inicio,
        "3. Cierra el día",
        "En Inicio, \"Tu caja\" te dice cuánto vendiste, cuánto gastaste y cuánto te quedó. Si sacas plata para la casa, " +
            "toca \"Saqué para la casa\" y la caja te cuadra.",
    ),
    Paso(Iconos.Cobros, "4. Lo que te deben", "En Cobros anota los fiados y los pedidos con adelanto. Si tienes su celular, recuérdaselo por WhatsApp."),
    Paso(Iconos.Aviso, "5. Tu tope", "Centavot te avisa cuando tus ventas o compras se acercan al tope de tu régimen, antes de que sea un problema."),
    Paso(Iconos.Reporte, "6. Tus reportes", "En Reportes ves cómo te fue mes a mes y puedes mandar el resumen a tu contador en un archivo de Excel."),
)

private val PASOS_PERSONAL = listOf(
    Paso(Iconos.Venta, "1. Anota lo que te entra", "Toca Ingreso y pon el monto: tu sueldo, una propina, un cachuelo. Con nombre, la próxima vez es un toque."),
    Paso(Iconos.Gasto, "2. Anota tus gastos", "Toca Gasto y pon el monto. Si quieres, elige en qué fue: comida, pasajes, servicios."),
    Paso(Iconos.Inicio, "3. Cierra el día", "En Inicio, \"Tu plata\" te dice cuánto te entró, cuánto gastaste y cuánto te quedó: hoy, en la semana o en el mes."),
    Paso(Iconos.Cobros, "4. Lo que te deben", "En Cobros anota la plata que prestaste. Si tienes su celular, recuérdaselo por WhatsApp."),
    Paso(Iconos.Personal, "5. Tu ahorro", "En \"Mi mes\" ves cuánto separar este mes según el porcentaje que elegiste en Ajustes."),
    Paso(Iconos.Reporte, "6. Tus reportes", "En Reportes ves lo que te entró y en qué se fue tu plata cada mes, y puedes guardarlo en Excel."),
)

/** El último paso, en los dos modos: cómo pasar al otro sin perder nada. */
private fun pasoCambiarModo(modo: Modo, numero: Int) = Paso(
    Modo.entries.first { it != modo }.icono,
    "$numero. ¿También ${if (modo == Modo.NEGOCIO) "quieres llevar tu plata personal" else "tienes un negocio"}?",
    "Toca \"${modo.etiqueta}\" arriba en Inicio y cambia de modo. No se borra nada: cada modo guarda lo suyo.",
)

/** Guía corta para empezar sin manual. Pensada para leerse junto a quien enseña a usar la app. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AyudaScreen(onCerrar: () -> Unit) {
    val observarModo = koinInject<ObservarModoUseCase>()
    val modo by remember(observarModo) { observarModo() }.collectAsStateWithLifecycle(Modo.NEGOCIO)
    val pasos = (if (modo == Modo.NEGOCIO) PASOS_NEGOCIO else PASOS_PERSONAL).let { it + pasoCambiarModo(modo, it.size + 1) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cómo usar Centavot") },
                navigationIcon = {
                    IconButton(onClick = onCerrar) { Icon(Iconos.Atras, contentDescription = "Volver") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LogoCentavot(modifier = Modifier.align(Alignment.CenterHorizontally), tamano = 112.dp)
            Text(
                if (modo == Modo.NEGOCIO) {
                    "Centavot es tu cuaderno, pero que suma solo. Úsalo varias veces al día, cada vez que vendas o gastes."
                } else {
                    "Centavot es tu cuaderno, pero que suma solo. Úsalo cada vez que te entre plata o gastes."
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            pasos.forEach { paso ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(paso.icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(paso.titulo, style = MaterialTheme.typography.titleSmall)
                            Text(paso.texto, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("¿No quieres dejar tu cuaderno?", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "No hay problema. Sigue anotando como siempre y, al cerrar el día, pasa aquí los totales de tus " +
                            "${if (modo == Modo.NEGOCIO) "ventas" else "ingresos"} y gastos. Con el tiempo verás que es más fácil anotar directo en el celular.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Iconos.Candado, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Tus datos son tuyos", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "No pedimos tu banco y no le enviamos nada a SUNAT. Todo queda en tu celular.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}
