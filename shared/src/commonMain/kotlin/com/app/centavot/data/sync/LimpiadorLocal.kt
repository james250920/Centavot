package com.app.centavot.data.sync

import com.app.centavot.data.local.DatosDao
import com.app.centavot.data.local.SyncDao

/** Borra todo lo guardado en el teléfono (datos, uso y marcas de sincronización) sin avisar nada al servidor. */
class LimpiadorLocal(private val dao: DatosDao, private val sync: SyncDao) {
    suspend fun limpiar() {
        dao.borrarCobros()
        dao.borrarContactos()
        dao.borrarIngresos()
        dao.borrarGastos()
        dao.borrarActividades()
        dao.borrarNotificaciones()
        dao.borrarMontosPresupuesto()
        dao.borrarPresupuesto()
        dao.borrarAbonosMetas()
        dao.borrarMetas()
        // Primero el régimen y el perfil: así la app vuelve al inicio aunque algo falle después.
        dao.borrarRegimen()
        dao.borrarPerfil()
        // Al final el uso: borra también el consentimiento, y la app vuelve a mostrar el aviso.
        dao.borrarEventosUso()
        // Los borrados anotados por los disparadores no se avisan al servidor: los datos siguen allá.
        sync.vaciarPendientes()
        sync.borrarAjustes()
    }
}
