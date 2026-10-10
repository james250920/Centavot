package com.app.centavot.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "ingresos", indices = [Index("fecha")])
data class IngresoEntity(
    @PrimaryKey val id: String,
    val montoCentimos: Long,
    /** Fecha ISO (AAAA-MM-DD): se ordena y filtra bien como texto. */
    val fecha: String,
    val categoria: String,
    val descripcion: String?,
    /** v4: plata sacada de la caja del negocio para la casa. */
    @ColumnInfo(defaultValue = "0") val retiroDelNegocio: Boolean = false,
)

@Dao
interface IngresoDao {
    @Query("SELECT * FROM ingresos ORDER BY fecha DESC, rowid DESC")
    fun observarTodos(): Flow<List<IngresoEntity>>

    @Query("SELECT * FROM ingresos WHERE fecha BETWEEN :desde AND :hasta ORDER BY fecha DESC, rowid DESC")
    fun observarEntre(desde: String, hasta: String): Flow<List<IngresoEntity>>

    @Query("SELECT * FROM ingresos WHERE id = :id")
    suspend fun obtener(id: String): IngresoEntity?

    @Upsert
    suspend fun guardar(ingreso: IngresoEntity)

    @Query("DELETE FROM ingresos WHERE id = :id")
    suspend fun eliminar(id: String)
}
