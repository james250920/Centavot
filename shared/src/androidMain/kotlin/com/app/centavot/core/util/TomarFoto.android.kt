package com.app.centavot.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

private const val LADO_MAXIMO = 1600
private const val CALIDAD_JPEG = 82

@Composable
actual fun rememberTomarFoto(alTenerFoto: (ByteArray?) -> Unit): AccionesFoto {
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    // La ruta de la foto de la cámara sobrevive si Android recrea la pantalla mientras la cámara está abierta.
    val rutaCamara = rememberSaveable { mutableStateOf<String?>(null) }

    fun procesar(uri: Uri?) {
        if (uri == null) { alTenerFoto(null); return }
        alcance.launch {
            val jpeg = withContext(Dispatchers.IO) { runCatching { comprimirFoto(contexto, uri) }.getOrNull() }
            alTenerFoto(jpeg)
        }
    }

    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val ruta = rutaCamara.value
        if (ok && ruta != null) procesar(Uri.fromFile(File(ruta))) else alTenerFoto(null)
    }
    // El selector de fotos del sistema: no pide permisos.
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> procesar(uri) }

    return remember {
        AccionesFoto(
            disponible = true,
            tomarFoto = {
                val carpeta = File(contexto.cacheDir, "fotos").apply { mkdirs() }
                val archivo = File(carpeta, "boleta.jpg")
                rutaCamara.value = archivo.absolutePath
                val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.archivos", archivo)
                camara.launch(uri)
            },
            elegirDeGaleria = { galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        )
    }
}

/** Abre la foto girada según la cámara (EXIF), la reduce y la pasa a JPEG. También la usa lo que llega por "Compartir". */
fun comprimirFoto(contexto: Context, uri: Uri): ByteArray {
    val original: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val fuente = ImageDecoder.createSource(contexto.contentResolver, uri)
        ImageDecoder.decodeBitmap(fuente) { decodificador, info, _ ->
            val mayor = maxOf(info.size.width, info.size.height)
            if (mayor > LADO_MAXIMO) {
                val escala = LADO_MAXIMO.toFloat() / mayor
                decodificador.setTargetSize((info.size.width * escala).toInt(), (info.size.height * escala).toInt())
            }
            decodificador.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } else {
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contexto.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, limites) }
        var muestra = 1
        while (maxOf(limites.outWidth, limites.outHeight) / (muestra * 2) >= LADO_MAXIMO) muestra *= 2
        contexto.contentResolver.openInputStream(uri).use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = muestra })
        } ?: error("No se pudo abrir la foto")
    }
    val salida = ByteArrayOutputStream()
    original.compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPEG, salida)
    original.recycle()
    return salida.toByteArray()
}
