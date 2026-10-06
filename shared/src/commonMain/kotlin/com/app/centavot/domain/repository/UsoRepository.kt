package com.app.centavot.domain.repository

import com.app.centavot.domain.model.EventoUso
import kotlinx.coroutines.flow.Flow

/** Eventos de uso guardados solo en el teléfono. */
interface UsoRepository {
    fun observar(): Flow<List<EventoUso>>

    suspend fun registrar(evento: EventoUso)
}
