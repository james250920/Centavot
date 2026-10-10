package com.app.lucka.domain.usecase

import com.app.lucka.domain.model.Actividad
import com.app.lucka.domain.model.MAX_ACTIVIDADES_VISIBLES
import com.app.lucka.domain.repository.ActividadRepository
import kotlinx.coroutines.flow.Flow

/** Solo las más recientes, o todo el historial si [completo]. Nunca se borra nada. */
class ObservarActividadesUseCase(private val repositorio: ActividadRepository) {
    operator fun invoke(completo: Boolean): Flow<List<Actividad>> =
        repositorio.observar(limite = if (completo) null else MAX_ACTIVIDADES_VISIBLES)
}
