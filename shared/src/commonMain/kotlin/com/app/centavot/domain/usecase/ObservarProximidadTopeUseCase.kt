package com.app.centavot.domain.usecase

import com.app.centavot.core.util.Reloj
import com.app.centavot.domain.model.ProximidadTope
import com.app.centavot.domain.model.rangoQueContiene
import com.app.centavot.domain.model.sumar
import com.app.centavot.domain.repository.GastoRepository
import com.app.centavot.domain.repository.RegimenRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Suma los gastos de negocio del periodo actual (mes o año, según el régimen)
 * y los compara con el tope. Emite null si el usuario no eligió régimen.
 */
class ObservarProximidadTopeUseCase(
    private val gastos: GastoRepository,
    private val regimenes: RegimenRepository,
    private val reloj: Reloj,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<ProximidadTope?> =
        regimenes.observarRegimen().flatMapLatest { regimen ->
            if (regimen == null) return@flatMapLatest flowOf(null)
            val rango = regimen.periodo.rangoQueContiene(reloj.hoy())
            gastos.observarGastosEntre(rango.start, rango.endInclusive).map { lista ->
                ProximidadTope(
                    acumulado = lista.filter { it.esDeNegocio }.map { it.monto }.sumar(),
                    tope = regimen.tope,
                )
            }
        }
}
