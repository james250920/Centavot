package com.app.centavot.data.local

import androidx.room.ColumnInfo
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
    /** v4: NEGOCIO o PERSONAL. Quien ya usaba la app sigue en modo negocio. */
    @ColumnInfo(defaultValue = "NEGOCIO") val modo: String = "NEGOCIO",
    /** v8: celular en formato internacional (+51987654321), sin verificar. */
    val celular: String? = null,
    /** v8: aceptó que el equipo le escriba por WhatsApp (consentimiento propio, Ley 29733). */
    @ColumnInfo(defaultValue = "0") val aceptaWhatsapp: Boolean = false,
) {
    companion object {
        const val ID_UNICO = 0
    }
}
