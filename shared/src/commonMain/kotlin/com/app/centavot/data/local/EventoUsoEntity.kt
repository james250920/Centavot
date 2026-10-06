package com.app.centavot.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "eventos_uso", indices = [Index("fechaHora")])
data class EventoUsoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tipo: String,
    /** ISO (AAAA-MM-DDTHH:MM:SS). */
    val fechaHora: String,
    val valor: String?,
)

@Dao
interface EventoUsoDao {
    @Query("SELECT * FROM eventos_uso ORDER BY fechaHora")
    fun observarTodos(): Flow<List<EventoUsoEntity>>

    @Insert
    suspend fun insertar(evento: EventoUsoEntity)
}
