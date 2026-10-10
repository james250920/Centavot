package com.app.centavot.domain.usecase

import com.app.centavot.domain.model.RegimenTributario
import com.app.centavot.domain.repository.RegimenRepository
import kotlinx.coroutines.flow.Flow

class ObservarRegimenUseCase(private val repositorio: RegimenRepository) {
    operator fun invoke(): Flow<RegimenTributario?> = repositorio.observarRegimen()
}
