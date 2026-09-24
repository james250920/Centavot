package com.app.centavot.presentation.screens.gasto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.centavot.core.util.Reloj
import com.app.centavot.domain.model.Categoria
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.usecase.EliminarGastoUseCase
import com.app.centavot.domain.usecase.GuardarGastoUseCase
import com.app.centavot.domain.usecase.ObtenerGastoUseCase
import com.app.centavot.presentation.components.comoTextoEditable
import com.app.centavot.presentation.components.esEntradaDeMontoValida
import com.app.centavot.presentation.components.parsearMonto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

private const val MAX_DESCRIPCION = 60

data class GastoUiState(
    val esEdicion: Boolean,
    val hoy: LocalDate,
    val fecha: LocalDate,
    val cargando: Boolean = false,
    val montoTexto: String = "",
    val categoria: Categoria? = null,
    val descripcion: String = "",
    val errorMonto: String? = null,
    val errorCategoria: String? = null,
    val errorFecha: String? = null,
    val guardando: Boolean = false,
    val confirmandoEliminar: Boolean = false,
    val terminado: Boolean = false,
)

/** Registrar un gasto nuevo ([id] null) o editar uno existente. */
class GastoViewModel(
    private val id: String?,
    private val obtenerGasto: ObtenerGastoUseCase,
    private val guardarGasto: GuardarGastoUseCase,
    private val eliminarGasto: EliminarGastoUseCase,
    reloj: Reloj,
) : ViewModel() {

    private val _estado = MutableStateFlow(
        GastoUiState(esEdicion = id != null, cargando = id != null, hoy = reloj.hoy(), fecha = reloj.hoy()),
    )
    val estado = _estado.asStateFlow()

    init {
        if (id != null) cargar(id)
    }

    private fun cargar(id: String) = viewModelScope.launch {
        val gasto = obtenerGasto(id)
        if (gasto == null) {
            _estado.update { it.copy(cargando = false, terminado = true) }
            return@launch
        }
        _estado.update {
            it.copy(
                cargando = false,
                montoTexto = gasto.monto.comoTextoEditable(),
                categoria = gasto.categoria,
                descripcion = gasto.descripcion.orEmpty(),
                fecha = gasto.fecha,
            )
        }
    }

    fun onMontoCambiado(texto: String) {
        if (esEntradaDeMontoValida(texto)) _estado.update { it.copy(montoTexto = texto, errorMonto = null) }
    }

    fun onCategoriaElegida(categoria: Categoria) =
        _estado.update { it.copy(categoria = categoria, errorCategoria = null) }

    fun onDescripcionCambiada(texto: String) =
        _estado.update { it.copy(descripcion = texto.take(MAX_DESCRIPCION)) }

    fun onFechaElegida(fecha: LocalDate) = _estado.update { it.copy(fecha = fecha, errorFecha = null) }

    fun guardar() {
        val actual = _estado.value
        val monto = parsearMonto(actual.montoTexto)?.takeIf { it > Monto.CERO }
        val categoria = actual.categoria
        if (monto == null || categoria == null) {
            _estado.update {
                it.copy(
                    errorMonto = if (monto == null) "Ingresa un monto mayor a cero" else null,
                    errorCategoria = if (categoria == null) "Elige si es de tu negocio o personal" else null,
                )
            }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(guardando = true) }
            val resultado = guardarGasto(id, monto, categoria, actual.fecha, actual.descripcion)
            _estado.update {
                when (resultado) {
                    is GuardarGastoUseCase.Resultado.Guardado,
                    GuardarGastoUseCase.Resultado.NoEncontrado -> it.copy(guardando = false, terminado = true)
                    GuardarGastoUseCase.Resultado.MontoInvalido ->
                        it.copy(guardando = false, errorMonto = "Ingresa un monto mayor a cero")
                    GuardarGastoUseCase.Resultado.FechaFutura ->
                        it.copy(guardando = false, errorFecha = "La fecha no puede ser futura")
                }
            }
        }
    }

    fun pedirEliminar() = _estado.update { it.copy(confirmandoEliminar = true) }

    fun cancelarEliminar() = _estado.update { it.copy(confirmandoEliminar = false) }

    fun confirmarEliminar() {
        val id = id ?: return
        viewModelScope.launch {
            eliminarGasto(id)
            _estado.update { it.copy(confirmandoEliminar = false, terminado = true) }
        }
    }
}
