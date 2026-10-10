package com.app.lucka.domain.usecase

import com.app.lucka.domain.model.ReporteSunat
import com.app.lucka.domain.repository.GastoRepository
import com.app.lucka.domain.repository.IngresoRepository
import com.app.lucka.domain.repository.RegimenRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.YearMonth

/** Ventas y gastos de negocio de un mes. Emite null si no hay régimen elegido. */
class ObservarReporteUseCase(
    private val gastos: GastoRepository,
    private val ingresos: IngresoRepository,
    private val regimenes: RegimenRepository,
) {
    operator fun invoke(periodo: YearMonth): Flow<ReporteSunat?> =
        combine(
            regimenes.observarRegimen(),
            gastos.observarGastosEntre(periodo.firstDay, periodo.lastDay),
            ingresos.observarIngresosEntre(periodo.firstDay, periodo.lastDay),
        ) { regimen, listaGastos, listaIngresos ->
            regimen?.let {
                ReporteSunat(
                    periodo = periodo,
                    regimen = it,
                    gastos = listaGastos.filter { gasto -> gasto.esDeNegocio },
                    ventas = listaIngresos.filter { ingreso -> ingreso.esDeNegocio },
                )
            }
        }
}
