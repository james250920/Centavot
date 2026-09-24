package com.app.centavot.domain.usecase

import com.app.centavot.domain.model.Gasto
import com.app.centavot.domain.repository.GastoRepository

class ObtenerGastoUseCase(private val repositorio: GastoRepository) {
    suspend operator fun invoke(id: String): Gasto? = repositorio.obtener(id)
}
