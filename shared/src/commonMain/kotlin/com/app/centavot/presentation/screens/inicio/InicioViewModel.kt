package com.app.centavot.presentation.screens.inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.centavot.core.util.Reloj
import com.app.centavot.domain.model.EstadoTope
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.Perfil
import com.app.centavot.domain.model.Periodo
import com.app.centavot.domain.model.ResumenCobros
import com.app.centavot.domain.model.ResumenMes
import com.app.centavot.domain.model.ResumenPeriodo
import com.app.centavot.domain.model.ResumenUso
import com.app.centavot.domain.model.TasaAhorro
import com.app.centavot.domain.usecase.ObservarEstadoTopeUseCase
import com.app.centavot.domain.usecase.ObservarGastosUseCase
import com.app.centavot.domain.usecase.ObservarIngresosUseCase
import com.app.centavot.domain.usecase.ObservarNoLeidasUseCase
import com.app.centavot.domain.usecase.ObservarPerfilUseCase
import com.app.centavot.domain.usecase.ObservarResumenCobrosUseCase
import com.app.centavot.domain.usecase.ObservarResumenMesUseCase
import com.app.centavot.domain.usecase.ObservarResumenPeriodoUseCase
import com.app.centavot.domain.usecase.ObservarResumenUsoUseCase
import com.app.centavot.domain.usecase.ResponderEncuestaCuadernoUseCase
import com.app.centavot.presentation.components.Movimiento
import com.app.centavot.presentation.components.movimientosDe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

data class InicioUiState(
    val hoy: LocalDate,
    val cargando: Boolean = true,
    val perfil: Perfil? = null,
    val tope: EstadoTope? = null,
    val periodo: Periodo = Periodo.HOY,
    /** Cierre de caja del periodo elegido. */
    val caja: ResumenPeriodo = ResumenPeriodo.VACIO,
    /** Ventas y gastos del mes, para la meta de ahorro. */
    val mes: ResumenPeriodo = ResumenPeriodo.VACIO,
    val gastosMes: ResumenMes? = null,
    val cobros: ResumenCobros? = null,
    val uso: ResumenUso? = null,
    val noLeidas: Int = 0,
    val recientes: List<Movimiento> = emptyList(),
    val hayMovimientos: Boolean = false,
) {
    /**
     * Base de la meta de ahorro: la ganancia real del mes si ya registra ventas;
     * si no, el ingreso mensual que puso en Ajustes.
     */
    val baseAhorro: Monto
        get() = if (mes.ventas > Monto.CERO) maxOf(mes.ganancia, Monto.CERO) else perfil?.ingresoMensual ?: Monto.CERO

    val baseAhorroEsGanancia: Boolean get() = mes.ventas > Monto.CERO

    val metaAhorro: Monto
        get() = Monto(baseAhorro.centimos * (perfil?.tasaAhorro?.decimas ?: 0) / TasaAhorro.MAXIMA)
}

private const val CANTIDAD_RECIENTES = 5

private data class Base(
    val perfil: Perfil?,
    val tope: EstadoTope?,
    val gastosMes: ResumenMes,
    val mes: ResumenPeriodo,
    val recientes: List<Movimiento>,
    val hayMovimientos: Boolean,
)

class InicioViewModel(
    observarEstadoTope: ObservarEstadoTopeUseCase,
    observarResumenMes: ObservarResumenMesUseCase,
    private val observarResumenPeriodo: ObservarResumenPeriodoUseCase,
    observarGastos: ObservarGastosUseCase,
    observarIngresos: ObservarIngresosUseCase,
    observarPerfil: ObservarPerfilUseCase,
    observarResumenCobros: ObservarResumenCobrosUseCase,
    observarNoLeidas: ObservarNoLeidasUseCase,
    observarResumenUso: ObservarResumenUsoUseCase,
    private val responderEncuesta: ResponderEncuestaCuadernoUseCase,
    reloj: Reloj,
) : ViewModel() {

    private val periodo = MutableStateFlow(Periodo.HOY)

    private val movimientos = combine(observarIngresos(), observarGastos()) { ingresos, gastos ->
        movimientosDe(ingresos, gastos)
    }

    private val base = combine(
        observarPerfil(),
        observarEstadoTope(),
        observarResumenMes(),
        observarResumenPeriodo(Periodo.MES),
        movimientos,
    ) { perfil, tope, gastosMes, mes, lista ->
        Base(perfil, tope, gastosMes, mes, lista.take(CANTIDAD_RECIENTES), lista.isNotEmpty())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val caja = periodo.flatMapLatest { elegido -> observarResumenPeriodo(elegido).map { elegido to it } }

    val estado: StateFlow<InicioUiState> = combine(
        base,
        caja,
        observarResumenCobros(),
        observarNoLeidas(),
        observarResumenUso(),
    ) { b, (elegido, resumenCaja), cobros, noLeidas, uso ->
        InicioUiState(
            hoy = reloj.hoy(),
            cargando = false,
            perfil = b.perfil,
            tope = b.tope,
            periodo = elegido,
            caja = resumenCaja,
            mes = b.mes,
            gastosMes = b.gastosMes,
            cobros = cobros,
            uso = uso,
            noLeidas = noLeidas,
            recientes = b.recientes,
            hayMovimientos = b.hayMovimientos,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InicioUiState(hoy = reloj.hoy()))

    fun onPeriodo(nuevo: Periodo) {
        periodo.value = nuevo
    }

    fun responderCuaderno(masFacil: Boolean) {
        viewModelScope.launch { responderEncuesta(masFacil) }
    }
}
