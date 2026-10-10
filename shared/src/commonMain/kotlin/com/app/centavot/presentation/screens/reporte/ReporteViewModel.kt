package com.app.centavot.presentation.screens.reporte

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.centavot.core.util.CompartidorArchivos
import com.app.centavot.core.util.Reloj
import com.app.centavot.domain.model.Categoria
import com.app.centavot.domain.model.Cobro
import com.app.centavot.domain.model.Gasto
import com.app.centavot.domain.model.Historial
import com.app.centavot.domain.model.ReporteSunat
import com.app.centavot.domain.model.ResumenCobros
import com.app.centavot.domain.model.aCsv
import com.app.centavot.domain.model.reporteGastosPersonalesCsv
import com.app.centavot.domain.model.reporteMeDebenCsv
import com.app.centavot.domain.usecase.ObservarCobrosUseCase
import com.app.centavot.domain.usecase.ObservarGastosDelMesUseCase
import com.app.centavot.domain.usecase.ObservarHistorialUseCase
import com.app.centavot.domain.usecase.ObservarReporteUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minusMonth
import kotlinx.datetime.plusMonth
import kotlinx.datetime.yearMonth

enum class PestanaReporte(val etiqueta: String) {
    NEGOCIO("Negocio"),
    PERSONAL("Personal"),
    ME_DEBEN("Me deben"),
}

data class ReporteUiState(
    val hoy: LocalDate,
    val periodo: YearMonth,
    val pestana: PestanaReporte = PestanaReporte.NEGOCIO,
    val reporte: ReporteSunat? = null,
    val historial: Historial? = null,
    val gastosPersonales: List<Gasto> = emptyList(),
    val cobrosPendientes: List<Cobro> = emptyList(),
    val cargando: Boolean = true,
) {
    /** No se puede avanzar a meses futuros. */
    val puedeAvanzar: Boolean get() = periodo < hoy.yearMonth

    val resumenCobros: ResumenCobros get() = ResumenCobros.de(cobrosPendientes)

    val puedeExportar: Boolean
        get() = when (pestana) {
            PestanaReporte.NEGOCIO -> reporte?.estaVacio == false
            PestanaReporte.PERSONAL -> gastosPersonales.isNotEmpty()
            PestanaReporte.ME_DEBEN -> cobrosPendientes.isNotEmpty()
        }
}

private data class DatosMes(val reporte: ReporteSunat?, val historial: Historial, val personales: List<Gasto>)

class ReporteViewModel(
    observarReporte: ObservarReporteUseCase,
    observarHistorial: ObservarHistorialUseCase,
    observarGastosDelMes: ObservarGastosDelMesUseCase,
    observarCobros: ObservarCobrosUseCase,
    private val reloj: Reloj,
    private val compartidor: CompartidorArchivos,
) : ViewModel() {

    private val periodo = MutableStateFlow(reloj.hoy().yearMonth)
    private val pestana = MutableStateFlow(PestanaReporte.NEGOCIO)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val datosMes = periodo.flatMapLatest { mes ->
        combine(observarReporte(mes), observarHistorial(mes), observarGastosDelMes(mes)) { reporte, historial, gastos ->
            mes to DatosMes(reporte, historial, gastos.filter { it.categoria == Categoria.PERSONAL })
        }
    }

    val estado: StateFlow<ReporteUiState> = combine(datosMes, pestana, observarCobros()) { (mes, datos), pestanaActual, cobros ->
        ReporteUiState(
            hoy = reloj.hoy(),
            periodo = mes,
            pestana = pestanaActual,
            reporte = datos.reporte,
            historial = datos.historial,
            gastosPersonales = datos.personales,
            cobrosPendientes = cobros.filter { it.estaPendiente },
            cargando = false,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ReporteUiState(hoy = reloj.hoy(), periodo = reloj.hoy().yearMonth),
    )

    fun onPestana(nueva: PestanaReporte) {
        pestana.value = nueva
    }

    /** Exporta en CSV (se abre en Excel) lo que se ve en la pestaña actual. */
    fun exportar() {
        val actual = estado.value
        when (actual.pestana) {
            PestanaReporte.NEGOCIO -> actual.reporte?.let {
                compartidor.compartir("lucka-negocio-${it.periodo}.csv", it.aCsv(), "text/csv")
            }
            PestanaReporte.PERSONAL -> compartidor.compartir(
                "lucka-personal-${actual.periodo}.csv",
                reporteGastosPersonalesCsv(actual.periodo, actual.gastosPersonales),
                "text/csv",
            )
            PestanaReporte.ME_DEBEN -> compartidor.compartir(
                "lucka-me-deben-${actual.hoy}.csv",
                reporteMeDebenCsv(actual.cobrosPendientes),
                "text/csv",
            )
        }
    }

    fun mesAnterior() = periodo.update { it.minusMonth() }

    fun mesSiguiente() = periodo.update { mes -> if (mes < reloj.hoy().yearMonth) mes.plusMonth() else mes }
}
