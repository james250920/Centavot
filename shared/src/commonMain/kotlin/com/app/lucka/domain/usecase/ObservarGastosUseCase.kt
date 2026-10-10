package com.app.lucka.domain.usecase

import com.app.lucka.domain.model.Gasto
import com.app.lucka.domain.repository.GastoRepository
import kotlinx.coroutines.flow.Flow

class ObservarGastosUseCase(private val repositorio: GastoRepository) {
    operator fun invoke(): Flow<List<Gasto>> = repositorio.observarGastos()
}
