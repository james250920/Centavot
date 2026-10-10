package com.app.centavot.presentation.screens.privacidad

import com.app.centavot.presentation.components.LogoCentavot
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.centavot.domain.model.VERSION_AVISO_PRIVACIDAD
import com.app.centavot.presentation.components.Iconos

private data class Apartado(val titulo: String, val texto: String)

private val APARTADOS = listOf(
    Apartado(
        "Quién es responsable",
        "Centavot es un proyecto del curso Proyecto Startup de la Universidad ESAN. " +
            "El equipo de Centavot es responsable de cómo la app trata tus datos.",
    ),
    Apartado(
        "Qué datos guarda",
        "Tu nombre, el rubro de tu negocio y tu régimen; tus ventas, gastos y cobros; los nombres y " +
            "celulares de los contactos que agregues; y cuántas veces usas la app.",
    ),
    Apartado(
        "Dónde se guardan",
        "Solo en este celular. No se envían a ningún servidor, banco ni a SUNAT, y no se copian " +
            "solos a la nube. Si cambias o pierdes el celular, se pierden, salvo que los hayas exportado.",
    ),
    Apartado(
        "Para qué se usan",
        "Para sumar tu caja, avisarte del tope de tu régimen, armar tus reportes y recordarte lo que " +
            "te deben. Nada más.",
    ),
    Apartado(
        "Datos de otras personas",
        "Si guardas el celular de un cliente, hazlo con su permiso. Solo se usa para abrir WhatsApp " +
            "cuando tú decides enviarle un recordatorio.",
    ),
    Apartado(
        "Tus derechos (Ley 29733)",
        "Puedes ver y llevarte tus datos (Ajustes → Exportar todos mis datos), corregirlos (editando " +
            "cada registro) y borrarlos (Ajustes → Borrar todos mis datos).",
    ),
    Apartado(
        "Si el aviso cambia",
        "Te lo volveremos a mostrar y tendrás que aceptarlo de nuevo antes de seguir.",
    ),
)

/**
 * Aviso de privacidad. Con [onAceptar] se muestra antes de usar la app y pide el consentimiento;
 * sin él, se abre desde Ajustes solo para leerlo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvisoPrivacidadScreen(
    onAceptar: (() -> Unit)? = null,
    onCerrar: () -> Unit = {},
) {
    val pideConsentimiento = onAceptar != null
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aviso de privacidad") },
                navigationIcon = {
                    if (!pideConsentimiento) {
                        IconButton(onClick = onCerrar) { Icon(Iconos.Atras, contentDescription = "Volver") }
                    }
                },
            )
        },
        bottomBar = {
            if (onAceptar != null) {
                Button(
                    onClick = onAceptar,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(56.dp),
                ) { Text("Entiendo y acepto") }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (pideConsentimiento) {
                LogoCentavot(
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 8.dp),
                    tamano = 140.dp,
                    animado = true,
                )
            }
            Text(
                text = if (pideConsentimiento) {
                    "Antes de empezar, lee cómo Centavot cuida tus datos. Es corto."
                } else {
                    "Así cuida Centavot tus datos."
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            APARTADOS.forEach { apartado ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(apartado.titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(apartado.texto, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Text(
                "Versión del aviso: $VERSION_AVISO_PRIVACIDAD",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
