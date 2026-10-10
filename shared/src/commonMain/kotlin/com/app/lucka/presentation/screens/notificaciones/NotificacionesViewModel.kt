package com.app.lucka.presentation.screens.notificaciones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.lucka.core.util.Reloj
import com.app.lucka.domain.model.Notificacion
import com.app.lucka.domain.usecase.MarcarNotificacionesLeidasUseCase
import com.app.lucka.domain.usecase.ObservarNotificacionesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

data class NotificacionesUiState(
    val hoy: LocalDate,
    val cargando: Boolean = true,
    val notificaciones: List<Notificacion> = emptyList(),
    /** Las que no estaban leídas al abrir la pantalla, para resaltarlas. */
    val nuevas: Set<String> = emptySet(),
)

/** Al abrir la bandeja, las notificaciones se marcan como leídas. */
class NotificacionesViewModel(
    observarNotificaciones: ObservarNotificacionesUseCase,
    private val marcarLeidas: MarcarNotificacionesLeidasUseCase,
    reloj: Reloj,
) : ViewModel() {

    private val _estado = MutableStateFlow(NotificacionesUiState(hoy = reloj.hoy()))
    val estado = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            var primeraVez = true
            observarNotificaciones().collect { lista ->
                _estado.update { actual ->
                    actual.copy(
                        cargando = false,
                        notificaciones = lista,
                        nuevas = if (primeraVez) lista.filterNot { it.leida }.map { it.id }.toSet() else actual.nuevas,
                    )
                }
                if (primeraVez) {
                    primeraVez = false
                    marcarLeidas()
                }
            }
        }
    }
}
