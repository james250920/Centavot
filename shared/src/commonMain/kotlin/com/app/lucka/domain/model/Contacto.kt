package com.app.lucka.domain.model

/** Persona a la que el usuario le fía o le presta. */
data class Contacto(
    val id: String,
    val nombre: String,
    val telefono: String? = null,
)
