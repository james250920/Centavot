package com.app.lucka.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PerfilDao {
    @Query("SELECT * FROM perfil WHERE id = ${PerfilEntity.ID_UNICO}")
    fun observar(): Flow<PerfilEntity?>

    @Upsert
    suspend fun guardar(perfil: PerfilEntity)
}
