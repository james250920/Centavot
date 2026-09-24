package com.app.centavot.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Una sola fila: el régimen del usuario de este teléfono. */
@Entity(tableName = "regimen")
data class RegimenEntity(
    @PrimaryKey val id: Int = ID_UNICO,
    val tipo: String,
    val nombre: String,
    val topeCentimos: Long,
    val periodo: String,
) {
    companion object {
        const val ID_UNICO = 0
    }
}
