package com.app.centavot.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Una sola fila: el perfil del usuario de este teléfono. */
@Entity(tableName = "perfil")
data class PerfilEntity(
    @PrimaryKey val id: Int = ID_UNICO,
    val nombre: String,
    val rubro: String?,
    val ingresoMensualCentimos: Long,
    /** Décimas de porcentaje: 125 = 12.5 %. */
    val tasaAhorroDecimas: Int,
) {
    companion object {
        const val ID_UNICO = 0
    }
}
