package com.app.centavot.presentation.screens.ayuda

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

private data class Paso(val icono: ImageVector, val titulo: String, val texto: String)

private val PASOS = listOf(
    Paso(Iconos.Venta, "1. Anota cada venta", "Toca Venta y pon el monto. Si le pones nombre (\"gaseosa\"), la próxima vez la registras con un solo toque."),
    Paso(Iconos.Gasto, "2. Anota tus gastos", "Toca Gasto y elige si fue para el negocio (mercadería, alquiler) o para tu casa. Así no se mezcla la plata."),
    Paso(Iconos.Inicio, "3. Cierra el día", "En Inicio, \"Tu caja\" te dice cuánto vendiste, cuánto gastaste y cuánto te quedó: hoy, en la semana o en el mes."),
    Paso(Iconos.Cobros, "4. Lo que te deben", "En Cobros anota los fiados, préstamos y pedidos con adelanto. Si tienes su celular, recuérdaselo por WhatsApp."),
    Paso(Iconos.Aviso, "5. Tu tope", "Lucka te avisa cuando tus ventas o compras se acercan al tope de tu régimen, antes de que sea un problema."),
    Paso(Iconos.Reporte, "6. Tus reportes", "En Reportes ves cómo te fue mes a mes y puedes mandar el resumen a tu contador en un archivo de Excel."),
)

/** Guía corta para empezar sin manual. Pensada para leerse junto a quien enseña a usar la app. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AyudaScreen(onCerrar: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cómo usar Lucka") },
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
            Text(
                "Lucka es tu cuaderno, pero que suma solo. Úsalo varias veces al día, cada vez que vendas o gastes.",
                style = MaterialTheme.typography.bodyLarge,
            )
            PASOS.forEach { paso ->
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
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("¿No quieres dejar tu cuaderno?", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "No hay problema. Sigue anotando como siempre y, al cerrar el día, pasa aquí los totales de tus " +
                            "ventas y gastos. Con el tiempo verás que es más fácil anotar directo en el celular.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Iconos.Candado, contentDescription = null)
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
