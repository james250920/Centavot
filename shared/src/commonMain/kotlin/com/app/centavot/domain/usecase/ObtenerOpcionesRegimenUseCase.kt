package com.app.centavot.domain.usecase

import com.app.centavot.domain.model.RegimenTributario
import com.app.centavot.domain.repository.RegimenRepository

class ObtenerOpcionesRegimenUseCase(private val repositorio: RegimenRepository) {
    suspend operator fun invoke(): List<RegimenTributario> = repositorio.opcionesDisponibles()
}
