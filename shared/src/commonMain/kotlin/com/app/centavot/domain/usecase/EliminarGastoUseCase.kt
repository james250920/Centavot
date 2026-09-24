package com.app.centavot.domain.usecase

import com.app.centavot.domain.repository.GastoRepository

class EliminarGastoUseCase(private val repositorio: GastoRepository) {
    suspend operator fun invoke(id: String) = repositorio.eliminar(id)
}
