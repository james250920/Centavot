package com.app.centavot.domain.repository

import com.app.centavot.domain.model.Gasto
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface GastoRepository {
    /** Todos los gastos, del más reciente al más antiguo. */
    fun observarGastos(): Flow<List<Gasto>>

    /** Gastos con fecha entre [desde] y [hasta], ambos incluidos. */
    fun observarGastosEntre(desde: LocalDate, hasta: LocalDate): Flow<List<Gasto>>

    suspend fun obtener(id: String): Gasto?

    suspend fun guardar(gasto: Gasto)

    suspend fun eliminar(id: String)
}
