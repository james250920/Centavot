package com.app.centavot.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "contactos")
data class ContactoEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val telefono: String?,
)

@Entity(
    tableName = "cobros",
    indices = [Index("contactoId"), Index("fecha")],
    // RESTRICT: un contacto con cobros no se puede borrar, para no perder historial.
    foreignKeys = [
        ForeignKey(
            entity = ContactoEntity::class,
            parentColumns = ["id"],
            childColumns = ["contactoId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
)
data class CobroEntity(
    @PrimaryKey val id: String,
    val contactoId: String,
    val motivo: String,
    val montoCentimos: Long,
    /** Fecha ISO (AAAA-MM-DD). */
    val fecha: String,
    val estado: String,
    val fechaCobrado: String?,
    /** v3: FIADO o PEDIDO. Los cobros de la v2 quedan como FIADO. */
    @ColumnInfo(defaultValue = "FIADO") val tipo: String = "FIADO",
    /** v3: lo que ya pagó por adelantado. */
    @ColumnInfo(defaultValue = "0") val adelantoCentimos: Long = 0,
    /** v3: suma de los abonos (pagos parciales). */
    @ColumnInfo(defaultValue = "0") val abonadoCentimos: Long = 0,
    /** v4: NEGOCIO (fiado o pedido) o PERSONAL (préstamo). Los cobros anteriores son del negocio. */
    @ColumnInfo(defaultValue = "NEGOCIO") val categoria: String = "NEGOCIO",
)

data class CobroConContacto(
    @Embedded val cobro: CobroEntity,
    @Relation(parentColumn = "contactoId", entityColumn = "id")
    val contacto: ContactoEntity,
)
