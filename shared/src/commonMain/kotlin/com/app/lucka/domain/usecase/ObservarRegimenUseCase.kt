package com.app.lucka.domain.usecase

import com.app.lucka.domain.model.RegimenTributario
import com.app.lucka.domain.repository.RegimenRepository
import kotlinx.coroutines.flow.Flow

class ObservarRegimenUseCase(private val repositorio: RegimenRepository) {
    operator fun invoke(): Flow<RegimenTributario?> = repositorio.observarRegimen()
}
