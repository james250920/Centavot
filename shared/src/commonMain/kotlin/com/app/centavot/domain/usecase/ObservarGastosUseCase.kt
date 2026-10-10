package com.app.centavot.domain.usecase

import com.app.centavot.domain.model.Gasto
import com.app.centavot.domain.repository.GastoRepository
import kotlinx.coroutines.flow.Flow

class ObservarGastosUseCase(private val repositorio: GastoRepository) {
    operator fun invoke(): Flow<List<Gasto>> = repositorio.observarGastos()
}
