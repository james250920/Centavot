package com.app.lucka.data.repository

import com.app.lucka.data.local.GastoDao
import com.app.lucka.data.mapper.toDomain
import com.app.lucka.data.mapper.toEntity
import com.app.lucka.domain.model.Gasto
import com.app.lucka.domain.repository.GastoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class GastoRepositoryImpl(private val dao: GastoDao) : GastoRepository {

    override fun observarGastos(): Flow<List<Gasto>> =
        dao.observarTodos().map { lista -> lista.map { it.toDomain() } }

    override fun observarGastosEntre(desde: LocalDate, hasta: LocalDate): Flow<List<Gasto>> =
        dao.observarEntre(desde.toString(), hasta.toString()).map { lista -> lista.map { it.toDomain() } }

    override suspend fun obtener(id: String): Gasto? = dao.obtener(id)?.toDomain()

    override suspend fun guardar(gasto: Gasto) = dao.guardar(gasto.toEntity())

    override suspend fun eliminar(id: String) = dao.eliminar(id)
}
