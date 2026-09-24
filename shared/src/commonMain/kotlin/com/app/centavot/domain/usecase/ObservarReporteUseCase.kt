package com.app.centavot.domain.usecase

import com.app.centavot.domain.model.ReporteSunat
import com.app.centavot.domain.repository.GastoRepository
import com.app.centavot.domain.repository.RegimenRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.YearMonth

/** Reporte de gastos de negocio de un mes. Emite null si no hay régimen elegido. */
class ObservarReporteUseCase(
    private val gastos: GastoRepository,
    private val regimenes: RegimenRepository,
) {
    operator fun invoke(periodo: YearMonth): Flow<ReporteSunat?> =
        combine(
            regimenes.observarRegimen(),
            gastos.observarGastosEntre(periodo.firstDay, periodo.lastDay),
        ) { regimen, lista ->
            regimen?.let { ReporteSunat(periodo, it, lista.filter { gasto -> gasto.esDeNegocio }) }
        }
}
