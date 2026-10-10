package com.app.lucka.core.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Abre el enlace con la app que lo maneje (WhatsApp para `wa.me`, o el navegador). */
class AbridorEnlacesAndroid(private val context: Context) : AbridorEnlaces {
    override fun abrir(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // Sin WhatsApp ni navegador no hay nada que abrir; el cobro sigue registrado.
        }
    }
}
