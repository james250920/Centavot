package com.app.lucka.core.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** Guarda el archivo en la caché y abre el menú de compartir (WhatsApp, correo, Drive...). */
class CompartidorArchivosAndroid(private val context: Context) : CompartidorArchivos {
    private val carpeta get() = File(context.cacheDir, "reportes")

    override fun compartir(nombreArchivo: String, contenido: String, tipoMime: String) {
        val archivo = File(carpeta.apply { mkdirs() }, nombreArchivo).apply { writeText(contenido) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.archivos", archivo)
        val enviar = Intent(Intent.ACTION_SEND).apply {
            type = tipoMime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(enviar, "Compartir reporte").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    override fun borrarArchivos() {
        carpeta.deleteRecursively()
    }
}
