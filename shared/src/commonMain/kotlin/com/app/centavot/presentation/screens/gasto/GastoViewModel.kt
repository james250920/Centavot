package com.app.centavot.presentation.screens.gasto

import com.app.centavot.presentation.components.formatear
import com.app.centavot.domain.model.Gasto
import com.app.centavot.presentation.Avisos
import com.app.centavot.presentation.Aviso
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.centavot.core.util.Reloj
import com.app.centavot.domain.model.Categoria
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.SubcategoriaGasto
import com.app.centavot.domain.model.aplicaA
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
import kotlinx.coroutines.flow.first
import com.app.centavot.domain.usecase.ObservarModoUseCase
import kotlinx.datetime.LocalDate

private const val MAX_DESCRIPCION = 60

data class GastoUiState(
    val esEdicion: Boolean,
    val hoy: LocalDate,
    val fecha: LocalDate,
    val cargando: Boolean = true,
    val montoTexto: String = "",
    /** La pone el modo (negocio o personal); al editar, la del gasto. */
    val categoria: Categoria = Categoria.NEGOCIO,
    val subcategoria: SubcategoriaGasto? = null,
    val descripcion: String = "",
    val errorMonto: String? = null,
    val errorFecha: String? = null,
    val guardando: Boolean = false,
    val confirmandoEliminar: Boolean = false,
    val terminado: Boolean = false,
)

/** Registrar un gasto nuevo ([id] null) del modo actual, o editar uno existente. */
class GastoViewModel(
    private val id: String?,
    private val obtenerGasto: ObtenerGastoUseCase,
    private val guardarGasto: GuardarGastoUseCase,
    private val eliminarGasto: EliminarGastoUseCase,
    private val avisos: Avisos,
    private val observarModo: ObservarModoUseCase,
    reloj: Reloj,
) : ViewModel() {

    private val _estado = MutableStateFlow(
        GastoUiState(esEdicion = id != null, hoy = reloj.hoy(), fecha = reloj.hoy()),
    )
    val estado = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val categoriaDelModo = observarModo().first().categoria
            if (id != null) cargar(id, categoriaDelModo) else _estado.update { it.copy(cargando = false, categoria = categoriaDelModo) }
        }
    }

    private suspend fun cargar(id: String, categoriaDelModo: Categoria) {
        val gasto = obtenerGasto(id)
        if (gasto == null) {
            _estado.update { it.copy(cargando = false, terminado = true) }
            return
        }
        _estado.update {
            it.copy(
                cargando = false,
                montoTexto = gasto.monto.comoTextoEditable(),
                // Un gasto antiguo sin clasificar toma la categoría del modo en que se abre.
                categoria = gasto.categoria ?: categoriaDelModo,
                subcategoria = gasto.subcategoria,
                descripcion = gasto.descripcion.orEmpty(),
                fecha = gasto.fecha,
            )
        }
    }

    fun onMontoCambiado(texto: String) {
        if (esEntradaDeMontoValida(texto)) _estado.update { it.copy(montoTexto = texto, errorMonto = null) }
    }

    fun onSubcategoriaElegida(subcategoria: SubcategoriaGasto?) = _estado.update { it.copy(subcategoria = subcategoria) }

    fun onDescripcionCambiada(texto: String) =
        _estado.update { it.copy(descripcion = texto.take(MAX_DESCRIPCION)) }

    fun onFechaElegida(fecha: LocalDate) = _estado.update { it.copy(fecha = fecha, errorFecha = null) }

    fun guardar() {
        val actual = _estado.value
        val monto = parsearMonto(actual.montoTexto)?.takeIf { it > Monto.CERO }
        val categoria = actual.categoria
        if (monto == null) {
            _estado.update { it.copy(errorMonto = "Ingresa un monto mayor a cero") }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(guardando = true) }
            val resultado = guardarGasto(id, monto, categoria, actual.fecha, actual.descripcion, actual.subcategoria)
            if (resultado is GuardarGastoUseCase.Resultado.Guardado) avisarGuardado(resultado.gasto)
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

    private fun avisarGuardado(gasto: Gasto) {
        if (id != null) {
            avisos.mostrar(Aviso("Cambios guardados"))
        } else {
            avisos.mostrar(Aviso("Gasto de ${gasto.monto.formatear()} guardado") { eliminarGasto(gasto.id) })
        }
    }

    fun pedirEliminar() = _estado.update { it.copy(confirmandoEliminar = true) }

    fun cancelarEliminar() = _estado.update { it.copy(confirmandoEliminar = false) }

    fun confirmarEliminar() {
        val id = id ?: return
        viewModelScope.launch {
            eliminarGasto(id)
            avisos.mostrar(Aviso("Gasto eliminado"))
            _estado.update { it.copy(confirmandoEliminar = false, terminado = true) }
        }
    }
}
