package com.app.lucka.presentation.screens.cobros

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.lucka.core.util.AbridorEnlaces
import com.app.lucka.core.util.Reloj
import com.app.lucka.core.util.enlaceWhatsApp
import com.app.lucka.domain.model.Cobro
import com.app.lucka.domain.model.ResumenCobros
import com.app.lucka.domain.model.mensajeRecordatorio
import com.app.lucka.domain.usecase.EliminarCobroUseCase
import com.app.lucka.domain.usecase.MarcarCobradoUseCase
import com.app.lucka.domain.usecase.ObservarCobrosUseCase
import com.app.lucka.domain.usecase.ObservarPerfilUseCase
import com.app.lucka.domain.usecase.RegistrarAbonoUseCase
import com.app.lucka.presentation.components.esEntradaDeMontoValida
import com.app.lucka.presentation.components.formatear
import com.app.lucka.presentation.components.parsearMonto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

data class CobrosUiState(
    val hoy: LocalDate,
    val cargando: Boolean = true,
    val cobros: List<Cobro> = emptyList(),
    val resumen: ResumenCobros? = null,
    val porEliminar: Cobro? = null,
    /** Para firmar el recordatorio de WhatsApp. */
    val nombreUsuario: String? = null,
    val abono: DialogoAbono? = null,
)

/** Diálogo para registrar un pago parcial de [cobro]. */
data class DialogoAbono(val cobro: Cobro, val montoTexto: String = "", val error: String? = null)

/** Préstamos y cuentas por cobrar ("fiado"). */
class CobrosViewModel(
    observarCobros: ObservarCobrosUseCase,
    private val marcarCobrado: MarcarCobradoUseCase,
    private val eliminarCobro: EliminarCobroUseCase,
    private val registrarAbono: RegistrarAbonoUseCase,
    observarPerfil: ObservarPerfilUseCase,
    private val abridor: AbridorEnlaces,
    reloj: Reloj,
) : ViewModel() {

    private val porEliminar = MutableStateFlow<Cobro?>(null)
    private val abono = MutableStateFlow<DialogoAbono?>(null)

    val estado: StateFlow<CobrosUiState> = combine(
        observarCobros(),
        porEliminar,
        observarPerfil(),
        abono,
    ) { cobros, eliminando, perfil, dialogoAbono ->
        CobrosUiState(
            hoy = reloj.hoy(),
            cargando = false,
            cobros = cobros,
            resumen = ResumenCobros.de(cobros),
            porEliminar = eliminando,
            nombreUsuario = perfil?.nombre,
            abono = dialogoAbono,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CobrosUiState(hoy = reloj.hoy()))

    fun cobrar(cobro: Cobro) {
        viewModelScope.launch { marcarCobrado(cobro.id) }
    }

    fun pedirAbono(cobro: Cobro) {
        abono.value = DialogoAbono(cobro)
    }

    fun onMontoAbono(texto: String) {
        if (esEntradaDeMontoValida(texto)) abono.update { it?.copy(montoTexto = texto, error = null) }
    }

    fun cancelarAbono() {
        abono.value = null
    }

    fun confirmarAbono() {
        val dialogo = abono.value ?: return
        val monto = parsearMonto(dialogo.montoTexto)
        if (monto == null) {
            abono.update { it?.copy(error = "Ingresa un monto mayor a cero") }
            return
        }
        viewModelScope.launch {
            val error = when (registrarAbono(dialogo.cobro.id, monto)) {
                is RegistrarAbonoUseCase.Resultado.Abonado, RegistrarAbonoUseCase.Resultado.NoEncontrado -> null
                RegistrarAbonoUseCase.Resultado.MontoInvalido -> "Ingresa un monto mayor a cero"
                RegistrarAbonoUseCase.Resultado.MayorQueElSaldo -> "No puede ser mayor que lo que falta (${dialogo.cobro.saldo.formatear()})"
            }
            abono.update { if (error == null) null else it?.copy(error = error) }
        }
    }

    /** Abre WhatsApp con un recordatorio ya escrito; lo envía el propio usuario. */
    fun recordar(cobro: Cobro) {
        val telefono = cobro.contacto.telefono ?: return
        enlaceWhatsApp(telefono, cobro.mensajeRecordatorio(estado.value.nombreUsuario))?.let(abridor::abrir)
    }

    fun pedirEliminar(cobro: Cobro) {
        porEliminar.value = cobro
    }

    fun cancelarEliminar() {
        porEliminar.value = null
    }

    fun confirmarEliminar() {
        val cobro = porEliminar.value ?: return
        porEliminar.value = null
        viewModelScope.launch { eliminarCobro(cobro.id) }
    }
}
