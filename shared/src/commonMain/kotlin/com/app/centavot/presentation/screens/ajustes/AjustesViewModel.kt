package com.app.centavot.presentation.screens.ajustes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.Perfil
import com.app.centavot.domain.model.RegimenTributario
import com.app.centavot.domain.model.Rubro
import com.app.centavot.domain.usecase.GuardarPerfilUseCase
import com.app.centavot.domain.usecase.ObservarPerfilUseCase
import com.app.centavot.domain.usecase.ObservarRegimenUseCase
import com.app.centavot.presentation.components.comoTextoEditable
import com.app.centavot.presentation.components.esEntradaDeMontoValida
import com.app.centavot.presentation.components.esEntradaDeTasaValida
import com.app.centavot.presentation.components.parsearMonto
import com.app.centavot.presentation.components.parsearTasa
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MAX_NOMBRE = 40

data class AjustesUiState(
    val cargando: Boolean = true,
    val nombre: String = "",
    val rubro: Rubro? = null,
    val ingresoTexto: String = "",
    val tasaTexto: String = "",
    val regimen: RegimenTributario? = null,
    val errorNombre: String? = null,
    val errorRubro: String? = null,
    val errorIngreso: String? = null,
    val errorTasa: String? = null,
    val guardando: Boolean = false,
    val terminado: Boolean = false,
)

/** Perfil (nombre y rubro) y ajustes financieros (ingreso mensual y tasa de ahorro). */
class AjustesViewModel(
    private val observarPerfil: ObservarPerfilUseCase,
    private val observarRegimen: ObservarRegimenUseCase,
    private val guardarPerfil: GuardarPerfilUseCase,
) : ViewModel() {

    private val _estado = MutableStateFlow(AjustesUiState())
    val estado = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val perfil = observarPerfil().first()
            _estado.update {
                it.copy(
                    cargando = false,
                    nombre = perfil?.nombre.orEmpty(),
                    rubro = perfil?.rubro,
                    ingresoTexto = perfil?.ingresoMensual?.takeIf { m -> m > Monto.CERO }?.comoTextoEditable().orEmpty(),
                    tasaTexto = perfil?.tasaAhorro?.takeIf { t -> t.decimas > 0 }?.comoTextoEditable().orEmpty(),
                )
            }
        }
        // El régimen puede cambiar mientras la pantalla está abierta (pantalla "Tu régimen").
        viewModelScope.launch {
            observarRegimen().collect { regimen -> _estado.update { it.copy(regimen = regimen) } }
        }
    }

    fun onNombreCambiado(texto: String) = _estado.update { it.copy(nombre = texto.take(MAX_NOMBRE), errorNombre = null) }

    fun onRubroElegido(rubro: Rubro?) = _estado.update { it.copy(rubro = rubro, errorRubro = null) }

    fun onIngresoCambiado(texto: String) {
        if (esEntradaDeMontoValida(texto)) _estado.update { it.copy(ingresoTexto = texto, errorIngreso = null) }
    }

    fun onTasaCambiada(texto: String) {
        if (esEntradaDeTasaValida(texto)) _estado.update { it.copy(tasaTexto = texto, errorTasa = null) }
    }

    fun guardar() {
        val actual = _estado.value
        val ingreso = if (actual.ingresoTexto.isBlank()) Monto.CERO else parsearMonto(actual.ingresoTexto)
        val tasa = parsearTasa(actual.tasaTexto)
        val errores = actual.copy(
            errorNombre = if (actual.nombre.isBlank()) "Escribe cómo quieres que te llamemos" else null,
            errorRubro = if (actual.rubro == null) "Elige a qué se dedica tu negocio" else null,
            errorIngreso = if (ingreso == null) "Revisa el monto" else null,
            errorTasa = if (tasa == null) "Debe ser un número entre 0 y 100" else null,
        )
        if (ingreso == null || tasa == null || errores.errorNombre != null || errores.errorRubro != null) {
            _estado.value = errores
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(guardando = true) }
            val perfil = Perfil(actual.nombre, actual.rubro, ingreso, tasa)
            _estado.update {
                when (guardarPerfil(perfil)) {
                    is GuardarPerfilUseCase.Resultado.Guardado -> it.copy(guardando = false, terminado = true)
                    GuardarPerfilUseCase.Resultado.NombreVacio ->
                        it.copy(guardando = false, errorNombre = "Escribe cómo quieres que te llamemos")
                }
            }
        }
    }
}
