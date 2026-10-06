package com.app.centavot.domain.repository

import com.app.centavot.domain.model.Ingreso
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface IngresoRepository {
    /** Todos los ingresos, del más reciente al más antiguo. */
    fun observarIngresos(): Flow<List<Ingreso>>

    /** Ingresos con fecha entre [desde] y [hasta], ambos incluidos. */
    fun observarIngresosEntre(desde: LocalDate, hasta: LocalDate): Flow<List<Ingreso>>

    suspend fun obtener(id: String): Ingreso?

    suspend fun guardar(ingreso: Ingreso)

    suspend fun eliminar(id: String)
}
