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
    Paso(Iconos.Venta, "Anota cada venta", "Ponle nombre y la próxima vez es un toque."),
    Paso(Iconos.Gasto, "Anota tus gastos", "Mercadería, alquiler, pasajes del negocio."),
    Paso(Iconos.Inicio, "Mira tu caja", "Lo que vendiste, gastaste y te quedó. Si sacas para la casa, anótalo."),
    Paso(Iconos.Cobros, "Lo que te deben", "Fiados y pedidos, con recordatorio por WhatsApp."),
    Paso(Iconos.Aviso, "Tu tope", "Te avisamos antes de que llegues."),
    Paso(Iconos.Reporte, "Para tu contador", "Mándale el resumen del mes en Excel."),
)

private val PASOS_PERSONAL = listOf(
    Paso(Iconos.Venta, "Anota lo que te entra", "Sueldo, propina o cachuelo."),
    Paso(Iconos.Gasto, "Anota tus gastos", "Pasajes, comida, luz."),
    Paso(Iconos.Inicio, "Mira tu plata", "Lo que te entró, gastaste y te quedó."),
    Paso(Iconos.Cobros, "Lo que te deben", "La plata que prestaste, con recordatorio por WhatsApp."),
    Paso(Iconos.Personal, "Tu ahorro", "En «Mi mes», según tu meta de Ajustes."),
    Paso(Iconos.Reporte, "Tus reportes", "Lo que entró y gastaste cada mes, en Excel."),
)

/** El último paso, en los dos modos: cómo pasar al otro sin perder nada. */
private fun pasoCambiarModo(modo: Modo) = Paso(
    Modo.entries.first { it != modo }.icono,
    if (modo == Modo.NEGOCIO) "¿También tu plata personal?" else "¿También tienes negocio?",
    "Toca «${modo.etiqueta}» arriba en Inicio. No se borra nada.",
)

/** Guía corta para empezar sin manual. Pensada para leerse junto a quien enseña a usar la app. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AyudaScreen(onCerrar: () -> Unit) {
    val observarModo = koinInject<ObservarModoUseCase>()
    val modo by remember(observarModo) { observarModo() }.collectAsStateWithLifecycle(Modo.NEGOCIO)
    val pasos = (if (modo == Modo.NEGOCIO) PASOS_NEGOCIO else PASOS_PERSONAL) + pasoCambiarModo(modo)
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
                "Tu cuaderno, pero que suma solo.",
                style = MaterialTheme.typography.bodyLarge,
            )
            // Una lista, no siete tarjetas: título y una línea por paso.
            pasos.forEach { paso ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(paso.icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(paso.titulo, style = MaterialTheme.typography.titleSmall)
                        Text(paso.texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("¿No quieres dejar tu cuaderno?", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Al cerrar el día, pasa aquí los totales.",
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
                            "Sin banco ni SUNAT. Todo queda en tu celular.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}
