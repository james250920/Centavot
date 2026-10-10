package com.app.lucka.presentation.screens.actividad

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.lucka.core.util.Reloj
import com.app.lucka.domain.model.Actividad
import com.app.lucka.domain.usecase.ObservarActividadesUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate

data class ActividadUiState(
    val hoy: LocalDate,
    val cargando: Boolean = true,
    val completo: Boolean = false,
    val actividades: List<Actividad> = emptyList(),
)

class ActividadViewModel(
    observarActividades: ObservarActividadesUseCase,
    reloj: Reloj,
) : ViewModel() {

    private val completo = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val estado: StateFlow<ActividadUiState> = completo.flatMapLatest { verTodo ->
        observarActividades(verTodo).map { ActividadUiState(reloj.hoy(), cargando = false, completo = verTodo, actividades = it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActividadUiState(hoy = reloj.hoy()))

    fun verHistorialCompleto() {
        completo.value = true
    }
}
