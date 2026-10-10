package com.app.centavot.presentation.screens.regimen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.centavot.domain.model.RegimenTributario
import com.app.centavot.domain.usecase.GuardarRegimenUseCase
import com.app.centavot.domain.usecase.ObservarRegimenUseCase
import com.app.centavot.domain.usecase.ObtenerOpcionesRegimenUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegimenUiState(
    val opciones: List<RegimenTributario> = emptyList(),
    val seleccionado: RegimenTributario? = null,
    val guardando: Boolean = false,
    val terminado: Boolean = false,
)

class RegimenViewModel(
    private val obtenerOpciones: ObtenerOpcionesRegimenUseCase,
    private val observarRegimen: ObservarRegimenUseCase,
    private val guardarRegimen: GuardarRegimenUseCase,
) : ViewModel() {

    private val _estado = MutableStateFlow(RegimenUiState())
    val estado = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val opciones = obtenerOpciones()
            val actual = observarRegimen().first()
            _estado.update { it.copy(opciones = opciones, seleccionado = opciones.firstOrNull { o -> o == actual }) }
        }
    }

    fun seleccionar(regimen: RegimenTributario) = _estado.update { it.copy(seleccionado = regimen) }

    fun guardar() {
        val regimen = _estado.value.seleccionado ?: return
        viewModelScope.launch {
            _estado.update { it.copy(guardando = true) }
            guardarRegimen(regimen)
            _estado.update { it.copy(guardando = false, terminado = true) }
        }
    }
}
