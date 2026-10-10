package com.app.lucka.domain.repository

import com.app.lucka.domain.model.Notificacion
import kotlinx.coroutines.flow.Flow

interface NotificacionRepository {
    /** De la más reciente a la más antigua. */
    fun observarTodas(): Flow<List<Notificacion>>

    fun observarNoLeidas(): Flow<Int>

    /** No hace nada si ya existe una notificación con el mismo id. */
    suspend fun agregarSiNoExiste(notificacion: Notificacion)

    suspend fun marcarTodasLeidas()
}
