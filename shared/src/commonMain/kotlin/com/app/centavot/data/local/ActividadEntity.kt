package com.app.centavot.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "actividades", indices = [Index("fechaHora")])
data class ActividadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val descripcion: String,
    /** ISO (AAAA-MM-DDTHH:MM:SS): se ordena bien como texto. */
    val fechaHora: String,
)

@Dao
interface ActividadDao {
    /** Nunca se borra nada: la vista solo pide las más recientes. */
    @Query("SELECT * FROM actividades ORDER BY fechaHora DESC, id DESC LIMIT :limite")
    fun observarRecientes(limite: Int): Flow<List<ActividadEntity>>

    @Query("SELECT * FROM actividades ORDER BY fechaHora DESC, id DESC")
    fun observarTodas(): Flow<List<ActividadEntity>>

    @Insert
    suspend fun insertar(actividad: ActividadEntity)
}
