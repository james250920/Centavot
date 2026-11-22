package com.app.centavot.core.util

import androidx.compose.runtime.Composable

/** Cómo conseguir la foto de una boleta en cada plataforma. [disponible] false = la plataforma aún no lo soporta. */
class AccionesFoto(val disponible: Boolean, val tomarFoto: () -> Unit, val elegirDeGaleria: () -> Unit)

/**
 * Abre la cámara o la galería y entrega la foto ya lista para leer: JPEG, girada según la cámara y reducida
 * (lado mayor de 1600 px) para que pese poco. [alTenerFoto] recibe null si se canceló o no se pudo abrir.
 */
@Composable
expect fun rememberTomarFoto(alTenerFoto: (ByteArray?) -> Unit): AccionesFoto
