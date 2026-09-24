package com.app.centavot.fakes

import com.app.centavot.domain.model.Gasto
import com.app.centavot.domain.model.RegimenTributario
import com.app.centavot.domain.repository.GastoRepository
import com.app.centavot.domain.repository.RegimenRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate

class FakeGastoRepository(iniciales: List<Gasto> = emptyList()) : GastoRepository {
    val gastos = MutableStateFlow(iniciales)

    override fun observarGastos(): Flow<List<Gasto>> = gastos.map { it.sortedByDescending(Gasto::fecha) }

    override fun observarGastosEntre(desde: LocalDate, hasta: LocalDate): Flow<List<Gasto>> =
        observarGastos().map { lista -> lista.filter { it.fecha in desde..hasta } }

    override suspend fun obtener(id: String): Gasto? = gastos.value.firstOrNull { it.id == id }

    override suspend fun guardar(gasto: Gasto) = gastos.update { lista -> lista.filterNot { it.id == gasto.id } + gasto }

    override suspend fun eliminar(id: String) = gastos.update { lista -> lista.filterNot { it.id == id } }
}

class FakeRegimenRepository(inicial: RegimenTributario? = null) : RegimenRepository {
    val regimen = MutableStateFlow(inicial)

    override fun observarRegimen(): Flow<RegimenTributario?> = regimen

    override suspend fun guardar(regimen: RegimenTributario) {
        this.regimen.value = regimen
    }

    override suspend fun opcionesDisponibles(): List<RegimenTributario> = emptyList()
}
