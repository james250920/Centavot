package com.app.lucka.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CobroDao {
    @Query("SELECT * FROM contactos ORDER BY nombre COLLATE NOCASE")
    fun observarContactos(): Flow<List<ContactoEntity>>

    @Upsert
    suspend fun guardarContacto(contacto: ContactoEntity)

    @Query("DELETE FROM contactos WHERE id = :id")
    suspend fun eliminarContacto(id: String)

    @Transaction
    @Query("SELECT * FROM cobros ORDER BY estado = 'COBRADO', fecha DESC, rowid DESC")
    fun observarCobros(): Flow<List<CobroConContacto>>

    @Transaction
    @Query("SELECT * FROM cobros WHERE id = :id")
    suspend fun obtenerCobro(id: String): CobroConContacto?

    @Upsert
    suspend fun guardarCobro(cobro: CobroEntity)

    @Query("DELETE FROM cobros WHERE id = :id")
    suspend fun eliminarCobro(id: String)
}
