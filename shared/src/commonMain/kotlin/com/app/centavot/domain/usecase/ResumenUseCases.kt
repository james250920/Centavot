package com.app.centavot.domain.usecase

import com.app.centavot.core.util.Reloj
import com.app.centavot.domain.model.Gasto
import com.app.centavot.domain.model.Historial
import com.app.centavot.domain.model.Periodo
import com.app.centavot.domain.model.ResumenPeriodo
import com.app.centavot.domain.model.rango
import com.app.centavot.domain.repository.GastoRepository
import com.app.centavot.domain.repository.IngresoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minusMonth

/** "Cierre de caja": vendido, gastado y ganancia del día, la semana o el mes. */
class ObservarResumenPeriodoUseCase(
    private val ingresos: IngresoRepository,
    private val gastos: GastoRepository,
    private val reloj: Reloj,
) {
    operator fun invoke(periodo: Periodo): Flow<ResumenPeriodo> {
        val rango = periodo.rango(reloj.hoy())
        return combine(
            ingresos.observarIngresosEntre(rango.start, rango.endInclusive),
            gastos.observarGastosEntre(rango.start, rango.endInclusive),
        ) { listaIngresos, listaGastos -> ResumenPeriodo.de(listaIngresos, listaGastos) }
    }
}

/** Gastos (de negocio y personales) de un mes. */
class ObservarGastosDelMesUseCase(private val gastos: GastoRepository) {
    operator fun invoke(mes: YearMonth): Flow<List<Gasto>> = gastos.observarGastosEntre(mes.firstDay, mes.lastDay)
}

/** Ventas y gastos de negocio de los últimos [cantidadMeses] meses hasta [hasta]. */
class ObservarHistorialUseCase(
    private val ingresos: IngresoRepository,
    private val gastos: GastoRepository,
) {
    operator fun invoke(hasta: YearMonth, cantidadMeses: Int = MESES_HISTORIAL): Flow<Historial> {
        var desde = hasta
        repeat(cantidadMeses - 1) { desde = desde.minusMonth() }
        return combine(
            ingresos.observarIngresosEntre(desde.firstDay, hasta.lastDay),
            gastos.observarGastosEntre(desde.firstDay, hasta.lastDay),
        ) { listaIngresos, listaGastos -> Historial.de(hasta, cantidadMeses, listaIngresos, listaGastos) }
    }

    companion object {
        const val MESES_HISTORIAL = 6
    }
}
