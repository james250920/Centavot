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
import com.app.centavot.domain.model.Modo
import com.app.centavot.domain.model.TipoEntrada
import com.app.centavot.domain.model.tipo
import com.app.centavot.domain.usecase.ObservarModoUseCase
import com.app.centavot.presentation.components.formatear
import kotlinx.coroutines.flow.first
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

/** Cómo se nombra cada entrada en los textos: "la venta", "el ingreso", "el retiro". */
val TipoEntrada.nombre: String
    get() = when (this) {
        TipoEntrada.VENTA -> "venta"
        TipoEntrada.INGRESO -> "ingreso"
        TipoEntrada.RETIRO -> "retiro"
    }

/**
 * Resumen de los detalles plegados: lo que se guardará si no se tocan. Si es venta o ingreso lo
 * dice el modo, así que aquí solo van la fecha y el nombre. Ej. "Hoy" o "Ayer · Pan".
 */
fun resumenDetallesVenta(fecha: LocalDate, hoy: LocalDate, descripcion: String): String =
    buildList {
        add(fecha.formatearRelativo(hoy))
        descripcion.trim().takeIf { it.isNotEmpty() }?.let(::add)
    }.joinToString(" · ")

/** Los detalles empiezan abiertos solo si ya tienen algo distinto de lo habitual. */
fun VentaUiState.detallesAbiertosAlInicio(): Boolean =
    esEdicion || fecha != hoy || descripcion.isNotBlank() || errorFecha != null

data class VentaUiState(
    val esEdicion: Boolean,
    val hoy: LocalDate,
    val fecha: LocalDate,
    val cargando: Boolean = true,
    /** Venta en modo negocio, ingreso en modo personal, o retiro para la casa. */
    val tipo: TipoEntrada = TipoEntrada.VENTA,
    val montoTexto: String = "",
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
 * Registrar ventas (o ingresos personales) una tras otra ([id] null) o editar uno existente.
 * Pensado para que una venta tome uno o dos toques: una frecuente se guarda con un toque, un monto
 * rápido con dos. Con [retiro] anota la plata que se sacó de la caja para la casa.
 */
class VentaViewModel(
    private val id: String?,
    private val retiro: Boolean,
    private val obtenerIngreso: ObtenerIngresoUseCase,
    private val guardarIngreso: GuardarIngresoUseCase,
    private val eliminarIngreso: EliminarIngresoUseCase,
    private val avisos: Avisos,
    private val observarFrecuentes: ObservarVentasFrecuentesUseCase,
    private val observarModo: ObservarModoUseCase,
    reloj: Reloj,
) : ViewModel() {

    private val _estado = MutableStateFlow(
        VentaUiState(esEdicion = id != null, hoy = reloj.hoy(), fecha = reloj.hoy()),
    )
    val estado = _estado.asStateFlow()

    init {
        if (id != null) cargar(id) else empezar()
    }

    /** Una entrada nueva es del modo actual: venta en el negocio, ingreso en lo personal. */
    private fun empezar() = viewModelScope.launch {
        val tipo = when {
            retiro -> TipoEntrada.RETIRO
            observarModo().first() == Modo.PERSONAL -> TipoEntrada.INGRESO
            else -> TipoEntrada.VENTA
        }
        _estado.update { it.copy(cargando = false, tipo = tipo) }
        if (tipo != TipoEntrada.RETIRO) {
            observarFrecuentes(categoriaDe(tipo)).collect { frecuentes -> _estado.update { it.copy(frecuentes = frecuentes) } }
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
                tipo = ingreso.tipo,
                descripcion = ingreso.descripcion.orEmpty(),
                fecha = ingreso.fecha,
            )
        }
    }

    fun onMontoCambiado(texto: String) {
        if (esEntradaDeMontoValida(texto)) _estado.update { it.copy(montoTexto = texto, errorMonto = null) }
    }

    fun onMontoRapido(soles: Long) = _estado.update { it.copy(montoTexto = soles.toString(), errorMonto = null) }

    fun onDescripcionCambiada(texto: String) = _estado.update { it.copy(descripcion = texto.take(MAX_DESCRIPCION)) }

    fun onFechaElegida(fecha: LocalDate) = _estado.update { it.copy(fecha = fecha, errorFecha = null) }

    /** Un toque: guarda de inmediato la venta frecuente con la fecha elegida. */
    fun registrarFrecuente(venta: VentaFrecuente) = guardarCon(venta.monto, venta.descripcion)

    fun guardar() {
        val actual = _estado.value
        val monto = parsearMonto(actual.montoTexto)?.takeIf { it > Monto.CERO }
        if (monto == null) {
            _estado.update { it.copy(errorMonto = "Ingresa un monto mayor a cero") }
            return
        }
        guardarCon(monto, actual.descripcion)
    }

    private fun guardarCon(monto: Monto, descripcion: String) {
        val actual = _estado.value
        if (actual.guardando) return
        val tipo = actual.tipo
        viewModelScope.launch {
            _estado.update { it.copy(guardando = true) }
            val resultado = guardarIngreso(
                id, monto, categoriaDe(tipo), actual.fecha, descripcion, retiroDelNegocio = tipo == TipoEntrada.RETIRO,
            )
            _estado.update {
                when (resultado) {
                    is GuardarIngresoUseCase.Resultado.Guardado ->
                        if (id != null) {
                            avisos.mostrar(Aviso("Cambios guardados"))
                            it.copy(guardando = false, terminado = true)
                        } else if (tipo == TipoEntrada.RETIRO) {
                            // Un retiro es uno solo: se cierra y se puede deshacer desde el aviso.
                            val guardado = resultado.ingreso
                            avisos.mostrar(Aviso("Sacaste ${guardado.monto.formatear()} para la casa") { eliminarIngreso(guardado.id) })
                            it.copy(guardando = false, terminado = true)
                        } else {
                            // Se queda abierta y limpia para registrar la siguiente venta.
                            it.copy(
                                guardando = false,
                                montoTexto = "",
                                descripcion = "",
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
            avisos.mostrar(Aviso(if (_estado.value.tipo == TipoEntrada.VENTA) "Venta eliminada" else "${_estado.value.tipo.nombre.replaceFirstChar { it.uppercase() }} eliminado"))
            _estado.update { it.copy(confirmandoEliminar = false, terminado = true) }
        }
    }
}

/** Las ventas son del negocio; el ingreso y el retiro, de la plata personal. */
private fun categoriaDe(tipo: TipoEntrada): Categoria = if (tipo == TipoEntrada.VENTA) Categoria.NEGOCIO else Categoria.PERSONAL
