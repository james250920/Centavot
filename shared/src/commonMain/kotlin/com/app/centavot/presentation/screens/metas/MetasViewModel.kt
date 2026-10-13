package com.app.centavot.presentation.screens.metas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.centavot.core.util.Reloj
import com.app.centavot.domain.model.Meta
import com.app.centavot.domain.model.Modo
import com.app.centavot.domain.model.ProgresoMeta
import com.app.centavot.domain.usecase.AbonarMetaUseCase
import com.app.centavot.domain.usecase.EliminarMetaUseCase
import com.app.centavot.domain.usecase.GuardarMetaUseCase
import com.app.centavot.domain.usecase.ObservarMetasUseCase
import com.app.centavot.domain.usecase.ObservarModoUseCase
import com.app.centavot.domain.usecase.QuitarAbonoMetaUseCase
import com.app.centavot.presentation.Aviso
import com.app.centavot.presentation.Avisos
import com.app.centavot.presentation.components.formatear
import com.app.centavot.presentation.components.parsearMonto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.plus

data class MetasUiState(
    val cargando: Boolean = true,
    val modo: Modo = Modo.PERSONAL,
    val hoy: LocalDate,
    val metas: List<ProgresoMeta> = emptyList(),
)

/** Para cuándo: opciones rápidas en vez de un calendario. */
enum class Plazo(val etiqueta: String) {
    SIN_FECHA("Sin fecha"),
    TRES_MESES("En 3 meses"),
    SEIS_MESES("En 6 meses"),
    UN_ANO("En 1 año"),
    FIN_DE_ANO("Fin de año"),
}

fun Plazo.fecha(hoy: LocalDate): LocalDate? = when (this) {
    Plazo.SIN_FECHA -> null
    Plazo.TRES_MESES -> hoy.plus(3, DateTimeUnit.MONTH)
    Plazo.SEIS_MESES -> hoy.plus(6, DateTimeUnit.MONTH)
    Plazo.UN_ANO -> hoy.plus(1, DateTimeUnit.YEAR)
    // Si ya es diciembre, el fin del año que viene.
    Plazo.FIN_DE_ANO -> LocalDate(if (hoy.month == Month.DECEMBER) hoy.year + 1 else hoy.year, 12, 31)
}

sealed interface DialogoMeta {
    data class Editar(val meta: Meta?) : DialogoMeta
    data class Abonar(val meta: Meta) : DialogoMeta
}

class MetasViewModel(
    observarModo: ObservarModoUseCase,
    observarMetas: ObservarMetasUseCase,
    private val guardarMeta: GuardarMetaUseCase,
    private val abonarMeta: AbonarMetaUseCase,
    private val quitarAbono: QuitarAbonoMetaUseCase,
    private val eliminarMeta: EliminarMetaUseCase,
    private val avisos: Avisos,
    private val reloj: Reloj,
) : ViewModel() {

    val dialogo = MutableStateFlow<DialogoMeta?>(null)
    val error = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val estado: StateFlow<MetasUiState> = observarModo()
        .flatMapLatest { modo -> observarMetas(modo).map { MetasUiState(false, modo, reloj.hoy(), it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MetasUiState(hoy = reloj.hoy()))

    fun nueva() = abrir(DialogoMeta.Editar(null))

    fun editar(meta: Meta) = abrir(DialogoMeta.Editar(meta))

    fun abonar(meta: Meta) = abrir(DialogoMeta.Abonar(meta))

    private fun abrir(nuevo: DialogoMeta) {
        error.value = null
        dialogo.value = nuevo
    }

    fun cerrar() {
        error.value = null
        dialogo.value = null
    }

    fun guardar(meta: Meta?, nombre: String, montoTexto: String, fechaLimite: LocalDate?) {
        val monto = parsearMonto(montoTexto) ?: run {
            error.value = "Ingresa cuánto quieres juntar"
            return
        }
        viewModelScope.launch {
            when (guardarMeta(meta?.id, estado.value.modo, nombre, monto, fechaLimite)) {
                is GuardarMetaUseCase.Resultado.Guardada -> {
                    avisos.mostrar(Aviso(if (meta == null) "Meta guardada" else "Cambios guardados"))
                    cerrar()
                }
                GuardarMetaUseCase.Resultado.NombreVacio -> error.value = "Ponle un nombre"
                GuardarMetaUseCase.Resultado.MontoInvalido -> error.value = "Ingresa cuánto quieres juntar"
                GuardarMetaUseCase.Resultado.FechaPasada -> error.value = "La fecha tiene que ser futura"
            }
        }
    }

    /** Separa plata para la meta; el aviso dice cuánto lleva y deja deshacer. */
    fun confirmarAbono(meta: Meta, montoTexto: String) {
        val monto = parsearMonto(montoTexto) ?: run {
            error.value = "Ingresa un monto mayor a cero"
            return
        }
        viewModelScope.launch {
            when (val resultado = abonarMeta(meta.id, monto)) {
                is AbonarMetaUseCase.Resultado.Abonado -> {
                    val texto = if (resultado.progreso.cumplida) {
                        "¡Cumpliste tu meta \"${meta.nombre}\"!"
                    } else {
                        "Separaste ${monto.formatear()}. Llevas ${resultado.progreso.porcentaje} %."
                    }
                    avisos.mostrar(Aviso(texto) { quitarAbono(resultado.abono.id) })
                    cerrar()
                }
                AbonarMetaUseCase.Resultado.MontoInvalido -> error.value = "Ingresa un monto mayor a cero"
                AbonarMetaUseCase.Resultado.NoEncontrada -> cerrar()
            }
        }
    }

    fun quitar(meta: Meta) {
        viewModelScope.launch {
            eliminarMeta(meta)
            avisos.mostrar(Aviso("Quitaste la meta \"${meta.nombre}\""))
            cerrar()
        }
    }

    fun limpiarError() = error.update { null }
}
