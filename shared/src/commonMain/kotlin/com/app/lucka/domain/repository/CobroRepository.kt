package com.app.lucka.domain.repository

import com.app.lucka.domain.model.Cobro
import com.app.lucka.domain.model.Contacto
import kotlinx.coroutines.flow.Flow

interface CobroRepository {
    /** Contactos ordenados por nombre. */
    fun observarContactos(): Flow<List<Contacto>>

    suspend fun guardarContacto(contacto: Contacto)

    suspend fun eliminarContacto(id: String)

    /** Cobros con su contacto: primero los pendientes, luego del más reciente al más antiguo. */
    fun observarCobros(): Flow<List<Cobro>>

    suspend fun obtenerCobro(id: String): Cobro?

    suspend fun guardarCobro(cobro: Cobro)

    suspend fun eliminarCobro(id: String)
}
