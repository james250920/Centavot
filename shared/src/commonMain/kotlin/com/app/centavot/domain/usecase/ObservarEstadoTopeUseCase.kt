package com.app.centavot.domain.usecase

import com.app.centavot.core.util.Reloj
import com.app.centavot.domain.model.EstadoTope
import com.app.centavot.domain.model.ProximidadTope
import com.app.centavot.domain.model.rangoQueContiene
import com.app.centavot.domain.model.sumar
import com.app.centavot.domain.repository.GastoRepository
import com.app.centavot.domain.repository.IngresoRepository
import com.app.centavot.domain.repository.RegimenRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * Compara las ventas del negocio del periodo actual (mes o año, según el régimen) con el tope.
 * En el Nuevo RUS también compara las compras y gastos del negocio del mes.
 * Emite null si el usuario no eligió régimen.
 */
class ObservarEstadoTopeUseCase(
    private val ingresos: IngresoRepository,
    private val gastos: GastoRepository,
    private val regimenes: RegimenRepository,
    private val reloj: Reloj,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<EstadoTope?> =
        regimenes.observarRegimen().flatMapLatest { regimen ->
            if (regimen == null) return@flatMapLatest flowOf(null)
            val rango = regimen.periodo.rangoQueContiene(reloj.hoy())
            combine(
                ingresos.observarIngresosEntre(rango.start, rango.endInclusive),
                gastos.observarGastosEntre(rango.start, rango.endInclusive),
            ) { listaIngresos, listaGastos ->
                EstadoTope(
                    regimen = regimen,
                    ventas = ProximidadTope(listaIngresos.filter { it.esDeNegocio }.map { it.monto }.sumar(), regimen.tope),
                    compras = if (regimen.controlaCompras) {
                        ProximidadTope(listaGastos.filter { it.esDeNegocio }.map { it.monto }.sumar(), regimen.tope)
                    } else {
                        null
                    },
                )
            }
        }
}
