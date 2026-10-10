package com.app.centavot.presentation.screens.cobros

import com.app.centavot.domain.model.Cobro
import com.app.centavot.domain.usecase.EliminarIngresoUseCase
import com.app.centavot.domain.usecase.EliminarCobroUseCase
import com.app.centavot.presentation.Avisos
import com.app.centavot.presentation.Aviso
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.centavot.core.util.Reloj
import com.app.centavot.domain.model.Contacto
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.TipoCobro
import com.app.centavot.domain.usecase.AgregarContactoUseCase
import com.app.centavot.domain.usecase.ObtenerCobroUseCase
import com.app.centavot.domain.usecase.ObservarContactosUseCase
import com.app.centavot.domain.usecase.RegistrarCobroUseCase
import com.app.centavot.domain.usecase.RegistrarCobroYVentaUseCase
import com.app.centavot.presentation.components.comoTextoEditable
import com.app.centavot.presentation.components.esEntradaDeMontoValida
import com.app.centavot.presentation.components.formatear
import com.app.centavot.presentation.components.parsearMonto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

private const val MAX_MOTIVO = 60

data class CobroUiState(
    val esEdicion: Boolean = false,
    /** Abonos ya registrados (solo al editar): el nuevo monto debe superarlos. */
    val abonado: Monto = Monto.CERO,
    val hoy: LocalDate,
    val fecha: LocalDate,
    val cargando: Boolean = true,
    val contactos: List<Contacto> = emptyList(),
    val contacto: Contacto? = null,
    val motivo: String = "",
    val montoTexto: String = "",
    val tipo: TipoCobro = TipoCobro.FIADO,
    /** Regla D1: por defecto el fiado o pedido cuenta como venta al registrarlo. */
    val contarComoVenta: Boolean = true,
    val adelantoTexto: String = "",
    val errorAdelanto: String? = null,
    val errorContacto: String? = null,
    val errorMotivo: String? = null,
    val errorMonto: String? = null,
    val errorFecha: String? = null,
    val agregandoContacto: Boolean = false,
    val errorNuevoContacto: String? = null,
    val guardando: Boolean = false,
    val terminado: Boolean = false,
)

/** Registrar lo que un contacto le debe al usuario: un fiado, un préstamo o un pedido con adelanto. */
class CobroViewModel(
    private val id: String?,
    observarContactos: ObservarContactosUseCase,
    private val obtenerCobro: ObtenerCobroUseCase,
    private val agregarContacto: AgregarContactoUseCase,
    private val registrarCobro: RegistrarCobroYVentaUseCase,
    private val eliminarCobro: EliminarCobroUseCase,
    private val eliminarIngreso: EliminarIngresoUseCase,
    private val avisos: Avisos,
    reloj: Reloj,
) : ViewModel() {

    private val _estado = MutableStateFlow(CobroUiState(esEdicion = id != null, hoy = reloj.hoy(), fecha = reloj.hoy()))
    val estado = _estado.asStateFlow()

    init {
        if (id != null) cargar(id)
        viewModelScope.launch {
            observarContactos().collect { contactos ->
                _estado.update { actual ->
                    actual.copy(
                        cargando = false,
                        contactos = contactos,
                        // Si solo hay un contacto, lo dejamos elegido.
                        contacto = actual.contacto?.let { elegido -> contactos.firstOrNull { it.id == elegido.id } }
                            ?: contactos.singleOrNull().takeIf { actual.contacto == null },
                    )
                }
            }
        }
    }

    private fun cargar(id: String) = viewModelScope.launch {
        val cobro = obtenerCobro(id)?.takeIf { it.estaPendiente }
        if (cobro == null) {
            _estado.update { it.copy(terminado = true) }
            return@launch
        }
        _estado.update {
            it.copy(
                contacto = cobro.contacto,
                tipo = cobro.tipo,
                montoTexto = cobro.monto.comoTextoEditable(),
                adelantoTexto = cobro.adelanto.takeIf { a -> a > Monto.CERO }?.comoTextoEditable().orEmpty(),
                abonado = cobro.abonado,
                motivo = cobro.motivo,
                fecha = cobro.fecha,
            )
        }
    }

    fun onContactoElegido(contacto: Contacto?) = _estado.update { it.copy(contacto = contacto, errorContacto = null) }

    fun onMotivoCambiado(texto: String) = _estado.update { it.copy(motivo = texto.take(MAX_MOTIVO), errorMotivo = null) }

    fun onMontoCambiado(texto: String) {
        if (esEntradaDeMontoValida(texto)) _estado.update { it.copy(montoTexto = texto, errorMonto = null) }
    }

    fun onTipoElegido(tipo: TipoCobro?) =
        _estado.update { it.copy(tipo = tipo ?: TipoCobro.FIADO, errorAdelanto = null) }

    fun onAdelantoCambiado(texto: String) {
        if (esEntradaDeMontoValida(texto)) _estado.update { it.copy(adelantoTexto = texto, errorAdelanto = null) }
    }

    fun onContarComoVenta(contar: Boolean) = _estado.update { it.copy(contarComoVenta = contar) }

    fun onFechaElegida(fecha: LocalDate) = _estado.update { it.copy(fecha = fecha, errorFecha = null) }

    fun pedirNuevoContacto() = _estado.update { it.copy(agregandoContacto = true, errorNuevoContacto = null) }

    fun cancelarNuevoContacto() = _estado.update { it.copy(agregandoContacto = false) }

    fun guardarNuevoContacto(nombre: String, telefono: String) {
        viewModelScope.launch {
            val resultado = agregarContacto(nombre, telefono)
            _estado.update {
                when (resultado) {
                    is AgregarContactoUseCase.Resultado.Agregado ->
                        it.copy(agregandoContacto = false, contacto = resultado.contacto, errorContacto = null)
                    AgregarContactoUseCase.Resultado.NombreVacio -> it.copy(errorNuevoContacto = "Escribe su nombre")
                    AgregarContactoUseCase.Resultado.Repetido -> it.copy(errorNuevoContacto = "Ya tienes un contacto con ese nombre")
                }
            }
        }
    }

    fun guardar() {
        val actual = _estado.value
        val contacto = actual.contacto
        val monto = parsearMonto(actual.montoTexto)?.takeIf { it.centimos > 0 }
        // El adelanto solo aplica a pedidos.
        val adelanto = if (actual.tipo == TipoCobro.PEDIDO && actual.adelantoTexto.isNotBlank()) {
            parsearMonto(actual.adelantoTexto)
        } else {
            Monto.CERO
        }
        if (contacto == null || monto == null || actual.motivo.isBlank() || adelanto == null) {
            _estado.update {
                it.copy(
                    errorContacto = if (contacto == null) "Elige quién te debe" else null,
                    errorMonto = if (monto == null) "Ingresa un monto mayor a cero" else null,
                    errorMotivo = if (actual.motivo.isBlank()) "Cuéntanos por qué te debe" else null,
                    errorAdelanto = if (adelanto == null) "Revisa el monto" else null,
                )
            }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(guardando = true) }
            val registro = registrarCobro(contacto, actual.motivo, monto, actual.fecha, actual.tipo, adelanto, id, actual.contarComoVenta)
            val resultado = registro.resultado
            if (resultado is RegistrarCobroUseCase.Resultado.Registrado) avisarGuardado(resultado.cobro, registro.ventaId)
            _estado.update {
                when (resultado) {
                    is RegistrarCobroUseCase.Resultado.Registrado -> it.copy(guardando = false, terminado = true)
                    RegistrarCobroUseCase.Resultado.MontoInvalido ->
                        it.copy(guardando = false, errorMonto = "Ingresa un monto mayor a cero")
                    RegistrarCobroUseCase.Resultado.MotivoVacio ->
                        it.copy(guardando = false, errorMotivo = "Cuéntanos por qué te debe")
                    RegistrarCobroUseCase.Resultado.FechaFutura ->
                        it.copy(guardando = false, errorFecha = "La fecha no puede ser futura")
                    RegistrarCobroUseCase.Resultado.AdelantoInvalido ->
                        it.copy(guardando = false, errorAdelanto = "El adelanto debe ser menor que el total")
                    RegistrarCobroUseCase.Resultado.MontoMenorQueLoPagado ->
                        it.copy(guardando = false, errorMonto = "Debe ser mayor que lo que ya te pagó (${(adelanto + it.abonado).formatear()})")
                    RegistrarCobroUseCase.Resultado.NoEditable -> it.copy(guardando = false, terminado = true)
                }
            }
        }
    }

    private fun avisarGuardado(cobro: Cobro, ventaId: String?) {
        if (id != null) {
            avisos.mostrar(Aviso("Cambios guardados"))
            return
        }
        val mensaje = "Cobro a ${cobro.contacto.nombre} guardado" + if (ventaId != null) " y sumado a tus ventas" else ""
        avisos.mostrar(
            Aviso(mensaje) {
                eliminarCobro(cobro.id)
                ventaId?.let { eliminarIngreso(it) }
            },
        )
    }
}
