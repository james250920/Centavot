package com.app.centavot.data.local

import androidx.room.Dao
import androidx.room.Query

/** Borrado completo. Los cobros se borran antes que los contactos por la llave foránea. */
@Dao
interface DatosDao {
    @Query("DELETE FROM cobros")
    suspend fun borrarCobros()

    @Query("DELETE FROM contactos")
    suspend fun borrarContactos()

    @Query("DELETE FROM ingresos")
    suspend fun borrarIngresos()

    @Query("DELETE FROM gastos")
    suspend fun borrarGastos()

    @Query("DELETE FROM actividades")
    suspend fun borrarActividades()

    @Query("DELETE FROM notificaciones")
    suspend fun borrarNotificaciones()

    @Query("DELETE FROM eventos_uso")
    suspend fun borrarEventosUso()

    @Query("DELETE FROM regimen")
    suspend fun borrarRegimen()

    @Query("DELETE FROM perfil")
    suspend fun borrarPerfil()
}
