package com.app.lucka.domain.repository

import com.app.lucka.domain.model.Actividad
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

interface ActividadRepository {
    /** Las [limite] actividades más recientes; null = todo el historial. */
    fun observar(limite: Int?): Flow<List<Actividad>>

    suspend fun registrar(descripcion: String, fechaHora: LocalDateTime)
}
