package com.app.centavot.data.repository

import com.app.centavot.data.local.DatosDao
import com.app.centavot.domain.repository.DatosRepository

class DatosRepositoryImpl(private val dao: DatosDao) : DatosRepository {
    override suspend fun borrarTodo() {
        dao.borrarCobros()
        dao.borrarContactos()
        dao.borrarIngresos()
        dao.borrarGastos()
        dao.borrarActividades()
        dao.borrarNotificaciones()
        // Primero el régimen y el perfil: así la app vuelve al inicio aunque algo falle después.
        dao.borrarRegimen()
        dao.borrarPerfil()
        // Al final el uso: borra también el consentimiento, y la app vuelve a mostrar el aviso.
        dao.borrarEventosUso()
    }
}
