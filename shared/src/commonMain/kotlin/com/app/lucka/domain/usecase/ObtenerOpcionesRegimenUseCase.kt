package com.app.lucka.domain.usecase

import com.app.lucka.domain.model.RegimenTributario
import com.app.lucka.domain.repository.RegimenRepository

class ObtenerOpcionesRegimenUseCase(private val repositorio: RegimenRepository) {
    suspend operator fun invoke(): List<RegimenTributario> = repositorio.opcionesDisponibles()
}
