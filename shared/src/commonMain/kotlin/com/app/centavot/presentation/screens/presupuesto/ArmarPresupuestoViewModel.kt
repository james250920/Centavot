package com.app.centavot.presentation.screens.presupuesto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.centavot.domain.model.ItemPlantilla
import com.app.centavot.domain.model.Modo
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.TipoPartida
import com.app.centavot.domain.model.plantillaPresupuesto
import com.app.centavot.domain.usecase.CrearPresupuestoUseCase
import com.app.centavot.domain.usecase.ObservarModoUseCase
import com.app.centavot.domain.usecase.ObservarPartidasUseCase
import com.app.centavot.presentation.Aviso
import com.app.centavot.presentation.Avisos
import com.app.centavot.presentation.components.esEntradaDeMontoValida
import com.app.centavot.presentation.components.parsearMonto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Un renglón de la plantilla: marcado o no, con su monto escrito (opcional). */
data class Renglon(val item: ItemPlantilla, val marcado: Boolean, val montoTexto: String = "")

data class ArmarPresupuestoUiState(
    val cargando: Boolean = true,
    val modo: Modo = Modo.PERSONAL,
    val renglones: List<Renglon> = emptyList(),
    val agregandoOtra: Boolean = false,
    val guardando: Boolean = false,
    val terminado: Boolean = false,
) {
    val marcados: Int get() = renglones.count { it.marcado }
}

/**
 * Arma el presupuesto con la plantilla: el usuario marca con check lo que usa y, si quiere, pone cuánto.
 * Lo que ya tiene en su presupuesto no se vuelve a ofrecer.
 */
class ArmarPresupuestoViewModel(
    private val observarModo: ObservarModoUseCase,
    private val observarPartidas: ObservarPartidasUseCase,
    private val crearPresupuesto: CrearPresupuestoUseCase,
    private val avisos: Avisos,
) : ViewModel() {

    private val _estado = MutableStateFlow(ArmarPresupuestoUiState())
    val estado = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val modo = observarModo().first()
            val tiene = observarPartidas(modo).first().map { it.nombre.lowercase() }.toSet()
            val renglones = plantillaPresupuesto(modo)
                .filterNot { it.nombre.lowercase() in tiene }
                .map { Renglon(it, marcado = it.marcado && tiene.isEmpty()) }
            _estado.update { it.copy(cargando = false, modo = modo, renglones = renglones) }
        }
    }

    fun onMarcar(item: ItemPlantilla, marcado: Boolean) = cambiar(item) { it.copy(marcado = marcado) }

    fun onMonto(item: ItemPlantilla, texto: String) {
        if (esEntradaDeMontoValida(texto)) cambiar(item) { it.copy(montoTexto = texto, marcado = it.marcado || texto.isNotEmpty()) }
    }

    private fun cambiar(item: ItemPlantilla, cambio: (Renglon) -> Renglon) =
        _estado.update { estado -> estado.copy(renglones = estado.renglones.map { if (it.item == item) cambio(it) else it }) }

    fun pedirOtra() = _estado.update { it.copy(agregandoOtra = true) }

    fun cancelarOtra() = _estado.update { it.copy(agregandoOtra = false) }

    /** Una partida propia ("Pensión de inglés"): entra marcada al final de su grupo. */
    fun agregarOtra(nombre: String, tipo: TipoPartida) {
        val limpio = nombre.trim().take(40)
        if (limpio.isEmpty()) return
        _estado.update { estado ->
            if (estado.renglones.any { it.item.nombre.equals(limpio, ignoreCase = true) }) return@update estado.copy(agregandoOtra = false)
            val nuevo = Renglon(ItemPlantilla(limpio, tipo, null, marcado = true), marcado = true)
            estado.copy(renglones = estado.renglones + nuevo, agregandoOtra = false)
        }
    }

    fun guardar() {
        val actual = _estado.value
        if (actual.guardando || actual.marcados == 0) return
        viewModelScope.launch {
            _estado.update { it.copy(guardando = true) }
            val elecciones = actual.renglones.filter { it.marcado }.map { renglon ->
                CrearPresupuestoUseCase.Eleccion(renglon.item, parsearMonto(renglon.montoTexto) ?: Monto.CERO)
            }
            crearPresupuesto(actual.modo, elecciones)
            avisos.mostrar(Aviso("Presupuesto listo: ${elecciones.size} partidas"))
            _estado.update { it.copy(guardando = false, terminado = true) }
        }
    }
}
