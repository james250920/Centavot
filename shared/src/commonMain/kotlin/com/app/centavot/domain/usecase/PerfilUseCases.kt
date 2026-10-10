package com.app.centavot.domain.usecase

import com.app.centavot.core.util.Reloj
import com.app.centavot.domain.model.Perfil
import com.app.centavot.domain.repository.ActividadRepository
import com.app.centavot.domain.repository.PerfilRepository
import kotlinx.coroutines.flow.Flow

class ObservarPerfilUseCase(private val repositorio: PerfilRepository) {
    operator fun invoke(): Flow<Perfil?> = repositorio.observarPerfil()
}

class GuardarPerfilUseCase(
    private val repositorio: PerfilRepository,
    private val actividades: ActividadRepository,
    private val reloj: Reloj,
) {
    sealed interface Resultado {
        data class Guardado(val perfil: Perfil) : Resultado
        data object NombreVacio : Resultado
    }

    suspend operator fun invoke(perfil: Perfil): Resultado {
        val nombre = perfil.nombre.trim()
        if (nombre.isEmpty()) return Resultado.NombreVacio
        val limpio = perfil.copy(nombre = nombre)
        repositorio.guardar(limpio)
        actividades.registrar("Actualizaste tu perfil y ajustes financieros.", reloj.ahora())
        return Resultado.Guardado(limpio)
    }
}
