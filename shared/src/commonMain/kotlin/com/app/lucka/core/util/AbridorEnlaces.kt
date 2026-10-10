package com.app.lucka.core.util

/** Abre un enlace en la app que corresponda (p. ej. WhatsApp). Lo implementa cada plataforma. */
fun interface AbridorEnlaces {
    fun abrir(url: String)
}

private const val CODIGO_PERU = "51"
private const val DIGITOS_CELULAR_PERU = 9

/**
 * Enlace `wa.me` con el mensaje ya escrito. Lo envía el propio usuario desde su WhatsApp,
 * así que no usa la API de WhatsApp Business ni tiene costo por mensaje.
 * Un celular peruano de 9 dígitos recibe el código de país 51. Devuelve null sin número válido.
 */
fun enlaceWhatsApp(telefono: String, mensaje: String): String? {
    val digitos = telefono.filter { it.isDigit() }
    val numero = when {
        digitos.length == DIGITOS_CELULAR_PERU -> CODIGO_PERU + digitos
        digitos.length > DIGITOS_CELULAR_PERU -> digitos
        else -> return null
    }
    return "https://wa.me/$numero?text=${codificarParaUrl(mensaje)}"
}

/** Codificación de porcentaje (UTF-8) para el texto de un enlace. */
fun codificarParaUrl(texto: String): String = buildString {
    texto.encodeToByteArray().forEach { byte ->
        val c = byte.toInt().toChar()
        if (c.isLetterOrDigit() && c.code < 128 || c in "-_.~") {
            append(c)
        } else {
            append('%')
            append((byte.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0'))
        }
    }
}
