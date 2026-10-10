package com.app.lucka.domain.repository

/** Operaciones sobre todos los datos del usuario a la vez. */
fun interface DatosRepository {
    /** Borra todo lo guardado en este teléfono: perfil, régimen, movimientos, cobros, historial y uso. */
    suspend fun borrarTodo()
}
