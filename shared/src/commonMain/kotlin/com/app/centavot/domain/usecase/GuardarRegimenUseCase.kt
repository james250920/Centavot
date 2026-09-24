package com.app.centavot.domain.usecase

import com.app.centavot.domain.model.RegimenTributario
import com.app.centavot.domain.repository.RegimenRepository

class GuardarRegimenUseCase(private val repositorio: RegimenRepository) {
    suspend operator fun invoke(regimen: RegimenTributario) = repositorio.guardar(regimen)
}
