package com.app.centavot.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface GastoDao {
    @Query("SELECT * FROM gastos ORDER BY fecha DESC, rowid DESC")
    fun observarTodos(): Flow<List<GastoEntity>>

    @Query("SELECT * FROM gastos WHERE fecha BETWEEN :desde AND :hasta ORDER BY fecha DESC, rowid DESC")
    fun observarEntre(desde: String, hasta: String): Flow<List<GastoEntity>>

    @Query("SELECT * FROM gastos WHERE id = :id")
    suspend fun obtener(id: String): GastoEntity?

    @Upsert
    suspend fun guardar(gasto: GastoEntity)

    @Query("DELETE FROM gastos WHERE id = :id")
    suspend fun eliminar(id: String)
}
