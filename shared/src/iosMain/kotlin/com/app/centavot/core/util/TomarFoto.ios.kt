package com.app.centavot.core.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** En iPhone la lectura de boletas se usa desde la web por ahora. */
@Composable
actual fun rememberTomarFoto(alTenerFoto: (ByteArray?) -> Unit): AccionesFoto =
    remember { AccionesFoto(disponible = false, tomarFoto = {}, elegirDeGaleria = {}) }
