package com.app.lucka.presentation.screens.cobros

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.lucka.domain.model.Contacto
import com.app.lucka.domain.model.Monto
import com.app.lucka.domain.usecase.AgregarContactoUseCase
import com.app.lucka.domain.usecase.EliminarContactoUseCase
import com.app.lucka.domain.usecase.ObservarContactosUseCase
import com.app.lucka.domain.usecase.ObservarResumenCobrosUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ContactoConDeuda(val contacto: Contacto, val debe: Monto)

data class DialogosContactos(
    val agregando: Boolean = false,
    val errorNuevo: String? = null,
    val porEliminar: Contacto? = null,
    /** Mensaje cuando no se puede eliminar un contacto. */
    val aviso: String? = null,
)

data class ContactosUiState(
    val cargando: Boolean = true,
    val contactos: List<ContactoConDeuda> = emptyList(),
    val dialogos: DialogosContactos = DialogosContactos(),
)

class ContactosViewModel(
    observarContactos: ObservarContactosUseCase,
    observarResumenCobros: ObservarResumenCobrosUseCase,
    private val agregarContacto: AgregarContactoUseCase,
    private val eliminarContacto: EliminarContactoUseCase,
) : ViewModel() {

    private val dialogos = MutableStateFlow(DialogosContactos())

    val estado: StateFlow<ContactosUiState> =
        combine(observarContactos(), observarResumenCobros(), dialogos) { contactos, resumen, dialogosActuales ->
            val deudas = resumen.porContacto.associate { it.contacto.id to it.total }
            ContactosUiState(
                cargando = false,
                contactos = contactos.map { ContactoConDeuda(it, deudas[it.id] ?: Monto.CERO) },
                dialogos = dialogosActuales,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContactosUiState())

    fun pedirAgregar() = dialogos.update { it.copy(agregando = true, errorNuevo = null) }

    fun cancelarAgregar() = dialogos.update { it.copy(agregando = false) }

    fun agregar(nombre: String, telefono: String) {
        viewModelScope.launch {
            val resultado = agregarContacto(nombre, telefono)
            dialogos.update {
                when (resultado) {
                    is AgregarContactoUseCase.Resultado.Agregado -> it.copy(agregando = false)
                    AgregarContactoUseCase.Resultado.NombreVacio -> it.copy(errorNuevo = "Escribe su nombre")
                    AgregarContactoUseCase.Resultado.Repetido -> it.copy(errorNuevo = "Ya tienes un contacto con ese nombre")
                }
            }
        }
    }

    fun pedirEliminar(contacto: Contacto) = dialogos.update { it.copy(porEliminar = contacto) }

    fun cancelarEliminar() = dialogos.update { it.copy(porEliminar = null) }

    fun confirmarEliminar() {
        val contacto = dialogos.value.porEliminar ?: return
        viewModelScope.launch {
            val resultado = eliminarContacto(contacto)
            dialogos.update {
                it.copy(
                    porEliminar = null,
                    aviso = if (resultado == EliminarContactoUseCase.Resultado.TieneCobros) {
                        "${contacto.nombre} tiene cobros registrados. Para no perder tu historial, no se puede eliminar."
                    } else {
                        null
                    },
                )
            }
        }
    }

    fun cerrarAviso() = dialogos.update { it.copy(aviso = null) }
}
