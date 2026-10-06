package com.app.centavot.data.repository

import com.app.centavot.data.local.EventoUsoDao
import com.app.centavot.data.local.IngresoDao
import com.app.centavot.data.mapper.toDomain
import com.app.centavot.data.mapper.toEntity
import com.app.centavot.domain.model.EventoUso
import com.app.centavot.domain.model.Ingreso
import com.app.centavot.domain.repository.IngresoRepository
import com.app.centavot.domain.repository.UsoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class IngresoRepositoryImpl(private val dao: IngresoDao) : IngresoRepository {

    override fun observarIngresos(): Flow<List<Ingreso>> =
        dao.observarTodos().map { lista -> lista.map { it.toDomain() } }

    override fun observarIngresosEntre(desde: LocalDate, hasta: LocalDate): Flow<List<Ingreso>> =
        dao.observarEntre(desde.toString(), hasta.toString()).map { lista -> lista.map { it.toDomain() } }

    override suspend fun obtener(id: String): Ingreso? = dao.obtener(id)?.toDomain()

    override suspend fun guardar(ingreso: Ingreso) = dao.guardar(ingreso.toEntity())

    override suspend fun eliminar(id: String) = dao.eliminar(id)
}

class UsoRepositoryImpl(private val dao: EventoUsoDao) : UsoRepository {
    override fun observar(): Flow<List<EventoUso>> = dao.observarTodos().map { lista -> lista.map { it.toDomain() } }

    override suspend fun registrar(evento: EventoUso) = dao.insertar(evento.toEntity())
}
