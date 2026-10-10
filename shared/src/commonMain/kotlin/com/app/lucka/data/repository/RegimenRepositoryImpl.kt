package com.app.lucka.data.repository

import com.app.lucka.data.local.RegimenDao
import com.app.lucka.data.local.TOPES_REFERENCIALES
import com.app.lucka.data.mapper.toDomain
import com.app.lucka.data.mapper.toEntity
import com.app.lucka.domain.model.RegimenTributario
import com.app.lucka.domain.repository.RegimenRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RegimenRepositoryImpl(private val dao: RegimenDao) : RegimenRepository {

    override fun observarRegimen(): Flow<RegimenTributario?> = dao.observar().map { it?.toDomain() }

    override suspend fun guardar(regimen: RegimenTributario) = dao.guardar(regimen.toEntity())

    override suspend fun opcionesDisponibles(): List<RegimenTributario> = TOPES_REFERENCIALES
}
