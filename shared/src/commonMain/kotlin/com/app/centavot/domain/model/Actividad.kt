package com.app.centavot.domain.model

import kotlinx.datetime.LocalDateTime

/**
 * Algo que el usuario hizo en la app. El historial nunca se borra: la pantalla
 * solo muestra las más recientes ([MAX_ACTIVIDADES_VISIBLES]).
 */
data class Actividad(
    val id: Long,
    val descripcion: String,
    val fechaHora: LocalDateTime,
)

const val MAX_ACTIVIDADES_VISIBLES = 15
