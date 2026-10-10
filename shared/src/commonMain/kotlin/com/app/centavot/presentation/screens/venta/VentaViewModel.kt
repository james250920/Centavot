package com.app.centavot.presentation.screens.venta

import com.app.centavot.presentation.Avisos
import com.app.centavot.presentation.Aviso
import com.app.centavot.presentation.components.formatearRelativo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.centavot.core.util.Reloj
import com.app.centavot.domain.model.Categoria
import com.app.centavot.domain.model.Ingreso
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.VentaFrecuente
import com.app.centavot.domain.usecase.EliminarIngresoUseCase
import com.app.centavot.domain.usecase.GuardarIngresoUseCase
import com.app.centavot.domain.usecase.ObservarVentasFrecuentesUseCase
import com.app.centavot.domain.usecase.ObtenerIngresoUseCase
import com.app.centavot.presentation.components.comoTextoEditable
import com.app.centavot.presentation.components.esEntradaDeMontoValida
import com.app.centavot.presentation.components.parsearMonto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

private const val MAX_DESCRIPCION = 40

/** Montos de un toque para las ventas más comunes de una bodega o un puesto. */
val MONTOS_RAPIDOS = listOf(1L, 2L, 5L, 10L, 20L, 50L)

/**
 * Resumen de los detalles plegados de una venta: lo que se guardará si no se tocan.
 * Ej. "Venta del negocio · Hoy" o "Ingreso personal · Ayer · Pan".
 */
fun resumenDetallesVenta(categoria: Categoria, fecha: LocalDate, hoy: LocalDate, descripcion: String): String =
    buildList {
        add(if (categoria == Categoria.PERSONAL) "Ingreso personal" else "Venta del negocio")
        add(fecha.formatearRelativo(hoy))
        descripcion.trim().takeIf { it.isNotEmpty() }?.let(::add)
    }.joinToString(" · ")

/** Los detalles empiezan abiertos solo si ya tienen algo distinto de lo habitual. */
fun VentaUiState.detallesAbiertosAlInicio(): Boolean =
    esEdicion || categoria != Categoria.NEGOCIO || fecha != hoy || descripcion.isNotBlank() || errorFecha != null

data class VentaUiState(
    val esEdicion: Boolean,
    val hoy: LocalDate,
    val fecha: LocalDate,
    val cargando: Boolean = false,
    val montoTexto: String = "",
    val categoria: Categoria = Categoria.NEGOCIO,
    val descripcion: String = "",
    val frecuentes: List<VentaFrecuente> = emptyList(),
    val errorMonto: String? = null,
    val errorFecha: String? = null,
    val guardando: Boolean = false,
    /** Última venta guardada en esta pantalla, para confirmarla sin cerrar (se registra la siguiente). */
    val ultimaGuardada: Ingreso? = null,
    /** Monto de la última venta que se deshizo, para decirlo en la misma pantalla. */
    val ventaQuitada: Monto? = null,
    val ventasEnEstaSesion: Int = 0,
    val confirmandoEliminar: Boolean = false,
    val terminado: Boolean = false,
)

/**
 * Registrar ventas una tras otra ([id] null) o editar una existente. Pensado para que una venta
 * tome uno o dos toques: una venta frecuente se guarda con un toque, un monto rápido con dos.
 */
class VentaViewModel(
    private val id: String?,
    private val obtenerIngreso: ObtenerIngresoUseCase,
    private val guardarIngreso: GuardarIngresoUseCase,
    private val eliminarIngreso: EliminarIngresoUseCase,
    private val avisos: Avisos,
    observarFrecuentes: ObservarVentasFrecuentesUseCase,
    reloj: Reloj,
) : ViewModel() {

    private val _estado = MutableStateFlow(
        VentaUiState(esEdicion = id != null, cargando = id != null, hoy = reloj.hoy(), fecha = reloj.hoy()),
    )
    val estado = _estado.asStateFlow()

    init {
        if (id != null) cargar(id)
        viewModelScope.launch {
            observarFrecuentes().collect { frecuentes -> _estado.update { it.copy(frecuentes = frecuentes) } }
        }
    }

    private fun cargar(id: String) = viewModelScope.launch {
        val ingreso = obtenerIngreso(id)
        if (ingreso == null) {
            _estado.update { it.copy(cargando = false, terminado = true) }
            return@launch
        }
        _estado.update {
            it.copy(
                cargando = false,
                montoTexto = ingreso.monto.comoTextoEditable(),
                categoria = ingreso.categoria,
                descripcion = ingreso.descripcion.orEmpty(),
                fecha = ingreso.fecha,
            )
        }
    }

    fun onMontoCambiado(texto: String) {
        if (esEntradaDeMontoValida(texto)) _estado.update { it.copy(montoTexto = texto, errorMonto = null) }
    }

    fun onMontoRapido(soles: Long) = _estado.update { it.copy(montoTexto = soles.toString(), errorMonto = null) }

    fun onCategoriaElegida(categoria: Categoria) = _estado.update { it.copy(categoria = categoria) }

    fun onDescripcionCambiada(texto: String) = _estado.update { it.copy(descripcion = texto.take(MAX_DESCRIPCION)) }

    fun onFechaElegida(fecha: LocalDate) = _estado.update { it.copy(fecha = fecha, errorFecha = null) }

    /** Un toque: guarda de inmediato la venta frecuente con la fecha elegida. */
    fun registrarFrecuente(venta: VentaFrecuente) =
        guardarCon(venta.monto, Categoria.NEGOCIO, venta.descripcion)

    fun guardar() {
        val actual = _estado.value
        val monto = parsearMonto(actual.montoTexto)?.takeIf { it > Monto.CERO }
        if (monto == null) {
            _estado.update { it.copy(errorMonto = "Ingresa un monto mayor a cero") }
            return
        }
        guardarCon(monto, actual.categoria, actual.descripcion)
    }

    private fun guardarCon(monto: Monto, categoria: Categoria, descripcion: String) {
        val actual = _estado.value
        if (actual.guardando) return
        viewModelScope.launch {
            _estado.update { it.copy(guardando = true) }
            val resultado = guardarIngreso(id, monto, categoria, actual.fecha, descripcion)
            _estado.update {
                when (resultado) {
                    is GuardarIngresoUseCase.Resultado.Guardado ->
                        if (id != null) {
                            avisos.mostrar(Aviso("Cambios guardados"))
                            it.copy(guardando = false, terminado = true)
                        } else {
                            // Se queda abierta y limpia para registrar la siguiente venta.
                            it.copy(
                                guardando = false,
                                montoTexto = "",
                                descripcion = "",
                                categoria = Categoria.NEGOCIO,
                                ultimaGuardada = resultado.ingreso,
                                ventaQuitada = null,
                                ventasEnEstaSesion = it.ventasEnEstaSesion + 1,
                            )
                        }
                    GuardarIngresoUseCase.Resultado.NoEncontrado -> it.copy(guardando = false, terminado = true)
                    GuardarIngresoUseCase.Resultado.MontoInvalido ->
                        it.copy(guardando = false, errorMonto = "Ingresa un monto mayor a cero")
                    GuardarIngresoUseCase.Resultado.FechaFutura ->
                        it.copy(guardando = false, errorFecha = "La fecha no puede ser futura")
                }
            }
        }
    }

    /** "Deshacer" de la confirmación: quita la última venta guardada en esta pantalla. */
    fun deshacerUltima() {
        val venta = _estado.value.ultimaGuardada ?: return
        _estado.update {
            it.copy(ultimaGuardada = null, ventaQuitada = venta.monto, ventasEnEstaSesion = (it.ventasEnEstaSesion - 1).coerceAtLeast(0))
        }
        viewModelScope.launch { eliminarIngreso(venta.id) }
    }

    fun pedirEliminar() = _estado.update { it.copy(confirmandoEliminar = true) }

    fun cancelarEliminar() = _estado.update { it.copy(confirmandoEliminar = false) }

    fun confirmarEliminar() {
        val id = id ?: return
        viewModelScope.launch {
            eliminarIngreso(id)
            avisos.mostrar(Aviso("Venta eliminada"))
            _estado.update { it.copy(confirmandoEliminar = false, terminado = true) }
        }
    }
}
