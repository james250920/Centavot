package com.app.centavot

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

        setContent {
            App()
        }
    }
}
