package com.app.centavot.presentation.screens.presupuesto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.centavot.core.util.Reloj
import com.app.centavot.domain.model.Modo
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.Partida
import com.app.centavot.domain.model.PresupuestoMes
import com.app.centavot.domain.model.TipoPartida
import com.app.centavot.domain.usecase.AgregarPartidaUseCase
import com.app.centavot.domain.usecase.EliminarPartidaUseCase
import com.app.centavot.domain.usecase.FijarMontoPartidaUseCase
import com.app.centavot.domain.usecase.ObservarModoUseCase
import com.app.centavot.domain.usecase.ObservarPresupuestoMesUseCase
import com.app.centavot.domain.usecase.RenombrarPartidaUseCase
import com.app.centavot.presentation.Aviso
import com.app.centavot.presentation.Avisos
import com.app.centavot.presentation.components.formatear
import com.app.centavot.presentation.components.parsearMonto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minusMonth
import kotlinx.datetime.plusMonth
import kotlinx.datetime.yearMonth

data class PresupuestoUiState(
    val cargando: Boolean = true,
    val modo: Modo = Modo.PERSONAL,
    val mes: YearMonth,
    val hoy: YearMonth,
    val presupuesto: PresupuestoMes? = null,
) {
    /** Se puede mirar hasta el mes que viene, para preparar el presupuesto antes de que empiece. */
    val puedeAvanzar: Boolean get() = mes < hoy.plusMonth()
}

/** Qué partida se está editando (o si se está agregando una). */
sealed interface Edicion {
    data class Existente(val partida: Partida, val monto: Monto) : Edicion
    data object Nueva : Edicion
}

class PresupuestoViewModel(
    observarModo: ObservarModoUseCase,
    observarPresupuestoMes: ObservarPresupuestoMesUseCase,
    private val fijarMonto: FijarMontoPartidaUseCase,
    private val renombrar: RenombrarPartidaUseCase,
    private val eliminarPartida: EliminarPartidaUseCase,
    private val agregarPartida: AgregarPartidaUseCase,
    private val avisos: Avisos,
    reloj: Reloj,
) : ViewModel() {

    private val hoy = reloj.hoy().yearMonth
    private val mes = MutableStateFlow(hoy)
    val edicion = MutableStateFlow<Edicion?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val estado: StateFlow<PresupuestoUiState> = combine(observarModo(), mes, ::Pair)
        .flatMapLatest { (modo, elegido) ->
            observarPresupuestoMes(elegido, modo).map { PresupuestoUiState(false, modo, elegido, hoy, it) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PresupuestoUiState(mes = hoy, hoy = hoy))

    fun mesAnterior() = mes.update { it.minusMonth() }

    fun mesSiguiente() = mes.update { if (it < hoy.plusMonth()) it.plusMonth() else it }

    fun editar(partida: Partida, monto: Monto) = edicion.update { Edicion.Existente(partida, monto) }

    fun nueva() = edicion.update { Edicion.Nueva }

    fun cerrarEdicion() = edicion.update { null }

    /** Guarda el nombre y el monto desde el mes que se está viendo; los meses anteriores no cambian. */
    fun guardarPartida(partida: Partida, nombre: String, montoTexto: String) {
        val monto = parsearMonto(montoTexto.ifBlank { "0" }) ?: return
        viewModelScope.launch {
            if (nombre.trim() != partida.nombre && !renombrar(partida, nombre)) {
                avisos.mostrar(Aviso("Ese nombre ya está en tu presupuesto"))
                return@launch
            }
            fijarMonto(partida, mes.value, monto)
            avisos.mostrar(Aviso("${nombre.trim()}: ${monto.formatear()} desde ${nombreMes(mes.value)}"))
            cerrarEdicion()
        }
    }

    fun quitar(partida: Partida) {
        viewModelScope.launch {
            eliminarPartida(partida)
            avisos.mostrar(Aviso("Quitaste ${partida.nombre}. Lo que anotaste en ella se queda."))
            cerrarEdicion()
        }
    }

    fun agregar(nombre: String, tipo: TipoPartida, montoTexto: String) {
        val monto = parsearMonto(montoTexto.ifBlank { "0" }) ?: return
        viewModelScope.launch {
            when (agregarPartida(estado.value.modo, nombre, tipo, monto, mes.value)) {
                is AgregarPartidaUseCase.Resultado.Agregada -> {
                    avisos.mostrar(Aviso("Agregaste ${nombre.trim()}"))
                    cerrarEdicion()
                }
                AgregarPartidaUseCase.Resultado.Repetida -> avisos.mostrar(Aviso("Ese nombre ya está en tu presupuesto"))
                AgregarPartidaUseCase.Resultado.NombreVacio -> Unit
            }
        }
    }

    private fun nombreMes(mes: YearMonth) = mes.formatear().lowercase()
}
