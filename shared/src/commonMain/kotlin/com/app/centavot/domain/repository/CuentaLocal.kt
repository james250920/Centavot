package com.app.centavot.domain.repository

/**
 * Los datos de este teléfono son de una sola cuenta (Ley 29733). Al cerrar sesión se sube lo pendiente y luego se borra
 * todo lo local, para que quien entre después no vea ni suba datos ajenos.
 */
interface CuentaLocal {
    /** Intenta subir lo pendiente y devuelve cuántos cambios siguen sin llegar al servidor. */
    suspend fun subirYContarPendientes(): Int

    /** Borra todo lo guardado en este teléfono y cierra la sesión. */
    suspend fun cerrarSesionYLimpiar()
}
