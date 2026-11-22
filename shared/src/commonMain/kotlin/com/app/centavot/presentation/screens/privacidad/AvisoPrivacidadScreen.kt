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
        "Cómo entras",
        "Con tu cuenta de Google, a través de Firebase (de Google). Solo sirve para saber que eres tú: " +
            "Centavot recibe tu nombre y tu correo, nunca tu contraseña. Tu cuenta (nombre y correo) queda " +
            "guardada en servidores de Google fuera del Perú.",
    ),
    Apartado(
        "Qué datos guarda",
        "Tu nombre y si usas la app para tu plata personal o tu negocio (y, si es negocio, su rubro y tu régimen); " +
            "tus ventas, ingresos, gastos y cobros; los nombres y " +
            "celulares de los contactos que agregues; y cuántas veces usas la app.",
    ),
    Apartado(
        "Dónde se guardan",
        "Primero en este celular, así funciona sin internet. Cuando hay conexión, se copian al servidor de " +
            "Centavot, protegidos con tu cuenta de Google: así no los pierdes si cambias de celular y los ves también " +
            "en la web. Cada persona solo puede ver sus propios datos. No se envían a bancos, a SUNAT ni a nadie más, " +
            "y no se venden.",
    ),
    Apartado(
        "Datos sensibles",
        "Lo que ganas y vendes es un dato sensible según la Ley 29733. Al tocar «Entiendo y acepto» das tu " +
            "consentimiento expreso para guardarlo y copiarlo al servidor con los fines de este aviso.",
    ),
    Apartado(
        "Para qué se usan",
        "Para sumar tu caja, avisarte del tope de tu régimen, armar tus reportes y recordarte lo que " +
            "te deben. Nada más.",
    ),
    Apartado(
        "Lectura de boletas y constancias",
        "Si le tomas foto a una boleta o factura, o compartes con Centavot una constancia de Yape, Plin o una " +
            "transferencia, la imagen se envía a Google (Gemini, en EE. UU.) solo para leer el monto, la fecha y el " +
            "nombre de quien emitió o de la otra persona. Centavot no guarda la imagen: tú revisas lo leído y decides " +
            "si lo guardas. Es opcional; puedes anotar todo a mano.",
    ),
    Apartado(
        "Mensajes por WhatsApp",
        "Solo si pones tu celular y marcas aparte «Acepto que el equipo de Centavot me escriba por WhatsApp», el " +
            "equipo podrá escribirte para avisos y novedades del piloto. Es opcional y puedes quitarlo cuando quieras " +
            "en Ajustes; sin ese permiso nadie te escribe.",
    ),
    Apartado(
        "Datos de otras personas",
        "Si guardas el celular de un cliente, hazlo con su permiso. Solo se usa para abrir WhatsApp " +
            "cuando tú decides enviarle un recordatorio.",
    ),
    Apartado(
        "Tus derechos (Ley 29733)",
        "Puedes ver y llevarte tus datos (Ajustes → Exportar todos mis datos), corregirlos (editando " +
            "cada registro) y borrarlos (Ajustes → Borrar todos mis datos). Borrar también los elimina del " +
            "servidor de Centavot.",
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
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(48.dp),
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
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 16.dp),
                    ancho = 200.dp,
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
