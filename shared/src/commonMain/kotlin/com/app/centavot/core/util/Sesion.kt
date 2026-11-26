package com.app.centavot.core.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Quién entró. Solo se usa para saber que es la misma persona: los movimientos siguen en el teléfono. */
/** [uid] identifica la cuenta: los datos del teléfono son de una sola cuenta. */
data class UsuarioSesion(val nombre: String?, val correo: String?, val uid: String = "")

sealed interface ResultadoIngreso {
    data object Listo : ResultadoIngreso
    /** La persona cerró la hoja de cuentas sin elegir: no es un error. */
    data object Cancelado : ResultadoIngreso
    data class Error(val mensaje: String) : ResultadoIngreso
}

/**
 * Ingreso con "Continuar con Google" (decidido el 11 nov 2026). En Android lo implementa Firebase
 * Authentication ([disponible] true); en otras plataformas o en pruebas, [SesionLocal] deja la app sin ingreso.
 */
interface Sesion {
    val disponible: Boolean
    val usuario: StateFlow<UsuarioSesion?>
    suspend fun entrarConGoogle(): ResultadoIngreso
    suspend fun cerrarSesion()

    /** Token para llamar al backend (el ID token de Firebase, se renueva solo); null sin sesión. */
    suspend fun token(): String? = null
}

/** Sin ingreso: la app funciona como antes (iOS mientras no tenga Firebase, y las pruebas). */
class SesionLocal : Sesion {
    override val disponible = false
    override val usuario: StateFlow<UsuarioSesion?> = MutableStateFlow(null)
    override suspend fun entrarConGoogle(): ResultadoIngreso = ResultadoIngreso.Error("El ingreso no está disponible.")
    override suspend fun cerrarSesion() = Unit
}
