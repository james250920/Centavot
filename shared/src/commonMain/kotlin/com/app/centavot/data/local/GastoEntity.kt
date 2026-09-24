package com.app.centavot.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "gastos", indices = [Index("fecha")])
data class GastoEntity(
    @PrimaryKey val id: String,
    val montoCentimos: Long,
    /** Fecha ISO (AAAA-MM-DD): se ordena y filtra bien como texto. */
    val fecha: String,
    val origen: String,
    val estado: String,
    val categoria: String?,
    val proveedor: String?,
    val descripcion: String?,
    val corregidoManualmente: Boolean,
)
