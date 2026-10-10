package com.app.lucka.domain.repository

import com.app.lucka.domain.model.Perfil
import kotlinx.coroutines.flow.Flow

interface PerfilRepository {
    /** Perfil del usuario; null si todavía no completa el onboarding. */
    fun observarPerfil(): Flow<Perfil?>

    suspend fun guardar(perfil: Perfil)
}
