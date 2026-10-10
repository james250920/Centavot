package com.app.centavot.domain.repository

import com.app.centavot.domain.model.RegimenTributario
import kotlinx.coroutines.flow.Flow

interface RegimenRepository {
    /** Régimen elegido por el usuario; null si todavía no completa el onboarding. */
    fun observarRegimen(): Flow<RegimenTributario?>

    suspend fun guardar(regimen: RegimenTributario)

    /** Regímenes entre los que puede elegir el usuario, con sus topes vigentes. */
    suspend fun opcionesDisponibles(): List<RegimenTributario>
}
