package com.app.lucka.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "notificaciones")
data class NotificacionEntity(
    @PrimaryKey val id: String,
    val asunto: String?,
    val mensaje: String,
    /** ISO (AAAA-MM-DDTHH:MM:SS). */
    val fechaHora: String,
    val leida: Boolean,
)

@Dao
interface NotificacionDao {
    @Query("SELECT * FROM notificaciones ORDER BY fechaHora DESC")
    fun observarTodas(): Flow<List<NotificacionEntity>>

    @Query("SELECT COUNT(*) FROM notificaciones WHERE leida = 0")
    fun observarNoLeidas(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarSiNoExiste(notificacion: NotificacionEntity)

    @Query("UPDATE notificaciones SET leida = 1 WHERE leida = 0")
    suspend fun marcarTodasLeidas()
}
