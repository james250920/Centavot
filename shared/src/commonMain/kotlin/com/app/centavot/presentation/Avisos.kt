package com.app.centavot.presentation

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Mensaje breve tras guardar algo ("Gasto de S/ 12.00 guardado"), con un [deshacer] opcional.
 * Se muestra abajo como snackbar, también si la pantalla que lo pidió ya se cerró.
 */
data class Aviso(val mensaje: String, val deshacer: (suspend () -> Unit)? = null)

/** Canal único de avisos de la app: cada aviso se muestra una sola vez. */
class Avisos {
    private val canal = Channel<Aviso>(Channel.BUFFERED)
    val flujo: Flow<Aviso> = canal.receiveAsFlow()

    fun mostrar(aviso: Aviso) {
        canal.trySend(aviso)
    }
}
