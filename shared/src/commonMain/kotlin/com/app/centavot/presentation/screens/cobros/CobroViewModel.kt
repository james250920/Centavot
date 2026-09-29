package com.app.centavot.presentation.screens.cobros

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.centavot.core.util.Reloj
import com.app.centavot.domain.model.Contacto
import com.app.centavot.domain.usecase.AgregarContactoUseCase
import com.app.centavot.domain.usecase.ObservarContactosUseCase
import com.app.centavot.domain.usecase.RegistrarCobroUseCase
import com.app.centavot.presentation.components.esEntradaDeMontoValida
import com.app.centavot.presentation.components.parsearMonto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

private const val MAX_MOTIVO = 60

data class CobroUiState(
    val hoy: LocalDate,
    val fecha: LocalDate,
    val cargando: Boolean = true,
    val contactos: List<Contacto> = emptyList(),
    val contacto: Contacto? = null,
    val motivo: String = "",
    val montoTexto: String = "",
    val errorContacto: String? = null,
    val errorMotivo: String? = null,
    val errorMonto: String? = null,
    val errorFecha: String? = null,
    val agregandoContacto: Boolean = false,
    val errorNuevoContacto: String? = null,
    val guardando: Boolean = false,
    val terminado: Boolean = false,
)

/** Registrar lo que un contacto le debe al usuario. */
class CobroViewModel(
    observarContactos: ObservarContactosUseCase,
    private val agregarContacto: AgregarContactoUseCase,
    private val registrarCobro: RegistrarCobroUseCase,
    reloj: Reloj,
) : ViewModel() {

    private val _estado = MutableStateFlow(CobroUiState(hoy = reloj.hoy(), fecha = reloj.hoy()))
    val estado = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            observarContactos().collect { contactos ->
                _estado.update { actual ->
                    actual.copy(
                        cargando = false,
                        contactos = contactos,
                        // Si solo hay un contacto, lo dejamos elegido.
                        contacto = actual.contacto?.takeIf { it in contactos } ?: contactos.singleOrNull(),
                    )
                }
            }
        }
    }

    fun onContactoElegido(contacto: Contacto?) = _estado.update { it.copy(contacto = contacto, errorContacto = null) }

    fun onMotivoCambiado(texto: String) = _estado.update { it.copy(motivo = texto.take(MAX_MOTIVO), errorMotivo = null) }

    fun onMontoCambiado(texto: String) {
        if (esEntradaDeMontoValida(texto)) _estado.update { it.copy(montoTexto = texto, errorMonto = null) }
    }

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
        if (contacto == null || monto == null || actual.motivo.isBlank()) {
            _estado.update {
                it.copy(
                    errorContacto = if (contacto == null) "Elige quién te debe" else null,
                    errorMonto = if (monto == null) "Ingresa un monto mayor a cero" else null,
                    errorMotivo = if (actual.motivo.isBlank()) "Cuéntanos por qué te debe" else null,
                )
            }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(guardando = true) }
            val resultado = registrarCobro(contacto, actual.motivo, monto, actual.fecha)
            _estado.update {
                when (resultado) {
                    is RegistrarCobroUseCase.Resultado.Registrado -> it.copy(guardando = false, terminado = true)
                    RegistrarCobroUseCase.Resultado.MontoInvalido ->
                        it.copy(guardando = false, errorMonto = "Ingresa un monto mayor a cero")
                    RegistrarCobroUseCase.Resultado.MotivoVacio ->
                        it.copy(guardando = false, errorMotivo = "Cuéntanos por qué te debe")
                    RegistrarCobroUseCase.Resultado.FechaFutura ->
                        it.copy(guardando = false, errorFecha = "La fecha no puede ser futura")
                }
            }
        }
    }
}
