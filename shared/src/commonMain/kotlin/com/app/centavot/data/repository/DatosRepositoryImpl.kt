package com.app.centavot.data.repository

import com.app.centavot.data.sync.Sincronizador
import com.app.centavot.data.local.DatosDao
import com.app.centavot.domain.repository.DatosRepository

class DatosRepositoryImpl(
    private val dao: DatosDao,
    private val sincronizador: Sincronizador? = null,
) : DatosRepository {
    override suspend fun borrarTodo() {
        // Ley 29733: primero la cuenta en el servidor; si no se puede (sin señal), no se borra nada y se avisa.
        if (sincronizador != null && !sincronizador.borrarCuentaEnServidor()) {
            throw IllegalStateException("No se pudo borrar tu cuenta en el servidor. Revisa tu conexión e inténtalo otra vez.")
        }
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
        // Lo borrado no se avisa al servidor (la cuenta ya no existe) y la próxima sincronización empieza de cero.
        sincronizador?.reiniciar()
    }
}
