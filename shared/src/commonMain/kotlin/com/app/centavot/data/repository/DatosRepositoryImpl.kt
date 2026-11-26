package com.app.centavot.data.repository

import com.app.centavot.data.sync.LimpiadorLocal
import com.app.centavot.data.sync.Sincronizador
import com.app.centavot.domain.repository.DatosRepository

class DatosRepositoryImpl(
    private val limpiador: LimpiadorLocal,
    private val sincronizador: Sincronizador? = null,
) : DatosRepository {
    override suspend fun borrarTodo() {
        // Ley 29733: primero la cuenta en el servidor; si no se puede (sin señal), no se borra nada y se avisa.
        if (sincronizador != null && !sincronizador.borrarCuentaEnServidor()) {
            throw IllegalStateException("No se pudo borrar tu cuenta en el servidor. Revisa tu conexión e inténtalo otra vez.")
        }
        // Lo de este teléfono, sin avisar borrados al servidor (la cuenta ya no existe).
        limpiador.limpiar()
    }
}
