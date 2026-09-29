package com.app.centavot.presentation.screens.cobros

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.centavot.core.util.Reloj
import com.app.centavot.domain.model.Cobro
import com.app.centavot.domain.model.ResumenCobros
import com.app.centavot.domain.usecase.EliminarCobroUseCase
import com.app.centavot.domain.usecase.MarcarCobradoUseCase
import com.app.centavot.domain.usecase.ObservarCobrosUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

data class CobrosUiState(
    val hoy: LocalDate,
    val cargando: Boolean = true,
    val cobros: List<Cobro> = emptyList(),
    val resumen: ResumenCobros? = null,
    val porEliminar: Cobro? = null,
)

/** Préstamos y cuentas por cobrar ("fiado"). */
class CobrosViewModel(
    observarCobros: ObservarCobrosUseCase,
    private val marcarCobrado: MarcarCobradoUseCase,
    private val eliminarCobro: EliminarCobroUseCase,
    reloj: Reloj,
) : ViewModel() {

    private val porEliminar = MutableStateFlow<Cobro?>(null)

    val estado: StateFlow<CobrosUiState> = combine(observarCobros(), porEliminar) { cobros, eliminando ->
        CobrosUiState(
            hoy = reloj.hoy(),
            cargando = false,
            cobros = cobros,
            resumen = ResumenCobros.de(cobros),
            porEliminar = eliminando,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CobrosUiState(hoy = reloj.hoy()))

    fun cobrar(cobro: Cobro) {
        viewModelScope.launch { marcarCobrado(cobro.id) }
    }

    fun pedirEliminar(cobro: Cobro) {
        porEliminar.value = cobro
    }

    fun cancelarEliminar() {
        porEliminar.value = null
    }

    fun confirmarEliminar() {
        val cobro = porEliminar.value ?: return
        porEliminar.value = null
        viewModelScope.launch { eliminarCobro(cobro.id) }
    }
}
