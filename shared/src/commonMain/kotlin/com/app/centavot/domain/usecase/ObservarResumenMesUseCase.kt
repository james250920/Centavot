package com.app.centavot.domain.usecase

import com.app.centavot.core.util.Reloj
import com.app.centavot.domain.model.Categoria
import com.app.centavot.domain.model.ResumenMes
import com.app.centavot.domain.model.sumar
import com.app.centavot.domain.repository.GastoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.yearMonth

/** Totales de negocio y personal del mes en curso. */
class ObservarResumenMesUseCase(
    private val gastos: GastoRepository,
    private val reloj: Reloj,
) {
    operator fun invoke(): Flow<ResumenMes> {
        val mes = reloj.hoy().yearMonth
        return gastos.observarGastosEntre(mes.firstDay, mes.lastDay).map { lista ->
            ResumenMes(
                totalNegocio = lista.filter { it.categoria == Categoria.NEGOCIO }.map { it.monto }.sumar(),
                totalPersonal = lista.filter { it.categoria == Categoria.PERSONAL }.map { it.monto }.sumar(),
                cantidadGastos = lista.size,
            )
        }
    }
}
