package com.app.centavot.presentation.screens.regimen

import com.app.centavot.presentation.components.formatear
import com.app.centavot.domain.model.PeriodoTope
import com.app.centavot.presentation.Avisos
import com.app.centavot.presentation.Aviso
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

/** "Ahora estás en RUS · Categoría 2. Tu tope es S/ 8,000.00 al mes." */
fun mensajeRegimenGuardado(regimen: RegimenTributario): String {
    val periodo = if (regimen.periodo == PeriodoTope.MENSUAL) "al mes" else "al año"
    return "Ahora estás en ${regimen.nombre}. Tu tope es ${regimen.tope.formatear()} $periodo."
}

class RegimenViewModel(
    private val obtenerOpciones: ObtenerOpcionesRegimenUseCase,
    private val observarRegimen: ObservarRegimenUseCase,
    private val guardarRegimen: GuardarRegimenUseCase,
    private val avisos: Avisos,
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

    /** [avisar] es false en el registro inicial: ahí la app avanza sola a Inicio. */
    fun guardar(avisar: Boolean = true) {
        val regimen = _estado.value.seleccionado ?: return
        viewModelScope.launch {
            _estado.update { it.copy(guardando = true) }
            guardarRegimen(regimen)
            if (avisar) avisos.mostrar(Aviso(mensajeRegimenGuardado(regimen)))
            _estado.update { it.copy(guardando = false, terminado = true) }
        }
    }
}
