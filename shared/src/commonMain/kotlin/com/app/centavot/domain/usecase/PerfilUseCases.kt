package com.app.centavot.domain.usecase

import com.app.centavot.core.util.Reloj
import com.app.centavot.domain.model.Modo
import com.app.centavot.domain.model.Perfil
import com.app.centavot.domain.repository.ActividadRepository
import com.app.centavot.domain.repository.PerfilRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class ObservarPerfilUseCase(private val repositorio: PerfilRepository) {
    operator fun invoke(): Flow<Perfil?> = repositorio.observarPerfil()
}

/** El modo de uso actual. Sin perfil todavía se asume negocio, como antes de que existieran los modos. */
class ObservarModoUseCase(private val repositorio: PerfilRepository) {
    operator fun invoke(): Flow<Modo> = repositorio.observarPerfil().map { it?.modo ?: Modo.NEGOCIO }.distinctUntilChanged()
}

/** Pasa de negocio a personal o al revés. No borra nada: solo cambia lo que se muestra. */
class CambiarModoUseCase(
    private val repositorio: PerfilRepository,
    private val actividades: ActividadRepository,
    private val reloj: Reloj,
) {
    suspend operator fun invoke(modo: Modo) {
        val perfil = repositorio.observarPerfil().first() ?: return
        if (perfil.modo == modo) return
        repositorio.guardar(perfil.copy(modo = modo))
        actividades.registrar("Cambiaste al modo \"${modo.etiqueta}\".", reloj.ahora())
    }
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
