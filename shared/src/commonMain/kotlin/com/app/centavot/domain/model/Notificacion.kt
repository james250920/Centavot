package com.app.centavot.domain.model

import kotlinx.datetime.LocalDateTime

/** Aviso dentro de la app. Sin [asunto] se muestra como "Aviso de Sistema". */
data class Notificacion(
    val id: String,
    val asunto: String?,
    val mensaje: String,
    val fechaHora: LocalDateTime,
    val leida: Boolean = false,
)
