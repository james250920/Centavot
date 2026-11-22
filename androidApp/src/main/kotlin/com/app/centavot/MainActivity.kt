package com.app.centavot

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.lifecycle.lifecycleScope
import com.app.centavot.core.util.comprimirFoto
import com.app.centavot.presentation.screens.compartido.BandejaCompartidos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onResume() {
        super.onResume()
        ActividadActual.fijar(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // La app siempre es clara: los íconos de las barras del sistema van oscuros también con el teléfono en
        // modo oscuro; si no, quedarían blancos sobre fondo blanco.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        ActividadActual.fijar(this)

        recibirCompartido(intent)
        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        recibirCompartido(intent)
    }

    /**
     * "Compartir" desde otra app (la constancia de Yape o Plin, una foto de boleta): la imagen se prepara y se deja en
     * la bandeja; la app abre la pantalla que la lee y luego Nuevo gasto o Nueva venta ya llenos.
     */
    private fun recibirCompartido(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND || intent.type?.startsWith("image/") != true) return
        val uri: Uri = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }) ?: return
        // Que no se vuelva a procesar al rotar la pantalla o volver a la app.
        intent.action = null
        lifecycleScope.launch {
            val jpeg = withContext(Dispatchers.IO) { runCatching { comprimirFoto(this@MainActivity, uri) }.getOrNull() }
            if (jpeg != null) GlobalContext.get().get<BandejaCompartidos>().imagen.value = jpeg
        }
    }
}
