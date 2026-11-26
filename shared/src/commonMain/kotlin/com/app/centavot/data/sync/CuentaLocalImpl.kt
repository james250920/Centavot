package com.app.centavot.data.sync

import com.app.centavot.core.util.Sesion
import com.app.centavot.data.local.SyncDao
import com.app.centavot.domain.repository.CuentaLocal

class CuentaLocalImpl(
    private val sincronizador: Sincronizador,
    private val limpiador: LimpiadorLocal,
    private val sesion: Sesion,
    private val sync: SyncDao,
) : CuentaLocal {
    override suspend fun subirYContarPendientes(): Int {
        sincronizador.sincronizar()
        return sync.pendientes().size
    }

    override suspend fun cerrarSesionYLimpiar() {
        limpiador.limpiar()
        sesion.cerrarSesion()
    }
}
