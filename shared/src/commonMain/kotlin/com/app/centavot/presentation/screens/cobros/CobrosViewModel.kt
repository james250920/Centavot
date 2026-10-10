package com.app.centavot.presentation.screens.cobros

import com.app.centavot.domain.usecase.RestaurarCobroUseCase
import com.app.centavot.presentation.Avisos
import com.app.centavot.presentation.Aviso
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.centavot.core.util.AbridorEnlaces
import com.app.centavot.core.util.Reloj
import com.app.centavot.core.util.enlaceWhatsApp
import com.app.centavot.domain.model.Cobro
import com.app.centavot.domain.model.ResumenCobros
import com.app.centavot.domain.model.mensajeRecordatorio
import com.app.centavot.domain.usecase.EliminarCobroUseCase
import com.app.centavot.domain.usecase.MarcarCobradoUseCase
import com.app.centavot.domain.usecase.ObservarCobrosUseCase
import com.app.centavot.domain.usecase.ObservarPerfilUseCase
import com.app.centavot.domain.usecase.RegistrarAbonoUseCase
import com.app.centavot.presentation.components.esEntradaDeMontoValida
import com.app.centavot.presentation.components.formatear
import com.app.centavot.presentation.components.parsearMonto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.app.centavot.domain.model.Modo
import com.app.centavot.domain.model.delModo
import com.app.centavot.domain.usecase.ObservarModoUseCase
import kotlinx.datetime.LocalDate

data class CobrosUiState(
    val hoy: LocalDate,
    val cargando: Boolean = true,
    val modo: Modo = Modo.NEGOCIO,
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
    private val restaurarCobro: RestaurarCobroUseCase,
    private val avisos: Avisos,
    observarPerfil: ObservarPerfilUseCase,
    private val abridor: AbridorEnlaces,
    observarModo: ObservarModoUseCase,
    reloj: Reloj,
) : ViewModel() {

    private val porEliminar = MutableStateFlow<Cobro?>(null)
    private val abono = MutableStateFlow<DialogoAbono?>(null)

    /** Solo los del modo actual: los fiados del negocio y los préstamos personales no se mezclan. */
    private val cobrosDelModo = combine(observarModo(), observarCobros()) { modo, cobros -> modo to cobros.delModo(modo) }

    val estado: StateFlow<CobrosUiState> = combine(
        cobrosDelModo,
        porEliminar,
        observarPerfil(),
        abono,
    ) { (modo, cobros), eliminando, perfil, dialogoAbono ->
        CobrosUiState(
            hoy = reloj.hoy(),
            cargando = false,
            modo = modo,
            cobros = cobros,
            resumen = ResumenCobros.de(cobros),
            porEliminar = eliminando,
            nombreUsuario = perfil?.nombre,
            abono = dialogoAbono,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CobrosUiState(hoy = reloj.hoy()))

    fun cobrar(cobro: Cobro) {
        viewModelScope.launch {
            marcarCobrado(cobro.id)
            val pago = cobro.saldo.formatear()
            avisos.mostrar(Aviso("${cobro.contacto.nombre} te pagó $pago") { restaurarCobro(cobro, "el pago de $pago") })
        }
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
            val resultado = registrarAbono(dialogo.cobro.id, monto)
            if (resultado is RegistrarAbonoUseCase.Resultado.Abonado) {
                val abono = monto.formatear()
                avisos.mostrar(
                    Aviso("Abono de $abono de ${dialogo.cobro.contacto.nombre} guardado") {
                        restaurarCobro(dialogo.cobro, "el abono de $abono")
                    },
                )
            }
            val error = when (resultado) {
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
        viewModelScope.launch {
            eliminarCobro(cobro.id)
            avisos.mostrar(Aviso("Cobro eliminado"))
        }
    }
}
