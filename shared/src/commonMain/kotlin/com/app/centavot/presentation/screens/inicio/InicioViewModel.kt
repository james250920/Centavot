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
import com.app.centavot.domain.usecase.ObservarCobrosUseCase
import com.app.centavot.domain.usecase.ObservarModoUseCase
import com.app.centavot.domain.usecase.CambiarModoUseCase
import com.app.centavot.domain.model.Modo
import com.app.centavot.domain.model.delModo
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
    val modo: Modo = Modo.NEGOCIO,
    val perfil: Perfil? = null,
    /** Solo en modo negocio: en lo personal no hay régimen ni tope. */
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
    /** Lo que entró en el mes según el modo: las ventas del negocio o el ingreso personal. */
    private val entradasDelMes: Monto get() = if (modo == Modo.NEGOCIO) mes.ventas else mes.ingresosPersonales

    /**
     * Base de la meta de ahorro: lo que de verdad entró en el mes (en el negocio, la ganancia);
     * si todavía no hay nada, el ingreso mensual que puso en Ajustes.
     */
    val baseAhorro: Monto
        get() = when {
            entradasDelMes <= Monto.CERO -> perfil?.ingresoMensual ?: Monto.CERO
            modo == Modo.NEGOCIO -> maxOf(mes.ganancia, Monto.CERO)
            else -> mes.ingresosPersonales
        }

    val baseAhorroEsGanancia: Boolean get() = entradasDelMes > Monto.CERO

    val metaAhorro: Monto
        get() = Monto(baseAhorro.centimos * (perfil?.tasaAhorro?.decimas ?: 0) / TasaAhorro.MAXIMA)
}

private const val CANTIDAD_RECIENTES = 5

private data class Base(
    val modo: Modo,
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
    observarCobros: ObservarCobrosUseCase,
    observarNoLeidas: ObservarNoLeidasUseCase,
    observarResumenUso: ObservarResumenUsoUseCase,
    private val responderEncuesta: ResponderEncuestaCuadernoUseCase,
    observarModo: ObservarModoUseCase,
    private val cambiarModo: CambiarModoUseCase,
    reloj: Reloj,
) : ViewModel() {

    private val periodo = MutableStateFlow(Periodo.HOY)

    private val modo = observarModo()

    /** Solo los movimientos del modo actual: lo del otro modo queda guardado, pero no se ve. */
    private val movimientos = combine(modo, observarIngresos(), observarGastos()) { modoActual, ingresos, gastos ->
        movimientosDe(ingresos.delModo(modoActual), gastos.delModo(modoActual))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val gastosMes = modo.flatMapLatest { observarResumenMes(it.categoria) }

    private val base = combine(
        combine(modo, observarPerfil(), ::Pair),
        observarEstadoTope(),
        gastosMes,
        observarResumenPeriodo(Periodo.MES),
        movimientos,
    ) { (modoActual, perfil), tope, resumenGastos, mes, lista ->
        Base(
            modo = modoActual,
            perfil = perfil,
            tope = tope.takeIf { modoActual == Modo.NEGOCIO },
            gastosMes = resumenGastos,
            mes = mes,
            recientes = lista.take(CANTIDAD_RECIENTES),
            hayMovimientos = lista.isNotEmpty(),
        )
    }

    private val cobros = combine(modo, observarCobros()) { modoActual, lista -> ResumenCobros.de(lista.delModo(modoActual)) }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val caja = periodo.flatMapLatest { elegido -> observarResumenPeriodo(elegido).map { elegido to it } }

    val estado: StateFlow<InicioUiState> = combine(
        base,
        caja,
        cobros,
        observarNoLeidas(),
        observarResumenUso(),
    ) { b, (elegido, resumenCaja), cobros, noLeidas, uso ->
        InicioUiState(
            hoy = reloj.hoy(),
            cargando = false,
            modo = b.modo,
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

    fun onCambiarModo(nuevo: Modo) {
        viewModelScope.launch { cambiarModo(nuevo) }
    }

    fun responderCuaderno(masFacil: Boolean) {
        viewModelScope.launch { responderEncuesta(masFacil) }
    }
}
