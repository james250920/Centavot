package com.app.centavot.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RegimenDao {
    @Query("SELECT * FROM regimen WHERE id = ${RegimenEntity.ID_UNICO}")
    fun observar(): Flow<RegimenEntity?>

    @Upsert
    suspend fun guardar(regimen: RegimenEntity)
}
