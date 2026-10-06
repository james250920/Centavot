package com.app.centavot.domain.repository

import com.app.centavot.domain.model.Perfil
import kotlinx.coroutines.flow.Flow

interface PerfilRepository {
    /** Perfil del usuario; null si todavía no completa el onboarding. */
    fun observarPerfil(): Flow<Perfil?>

    suspend fun guardar(perfil: Perfil)
}
