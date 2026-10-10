package com.app.lucka.domain.usecase

import com.app.lucka.domain.model.Gasto
import com.app.lucka.domain.repository.GastoRepository

class ObtenerGastoUseCase(private val repositorio: GastoRepository) {
    suspend operator fun invoke(id: String): Gasto? = repositorio.obtener(id)
}
