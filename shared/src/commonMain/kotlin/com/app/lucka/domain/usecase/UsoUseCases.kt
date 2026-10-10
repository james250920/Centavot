package com.app.lucka.domain.usecase

import com.app.lucka.core.util.Reloj
import com.app.lucka.domain.model.EventoUso
import com.app.lucka.domain.model.ResumenUso
import com.app.lucka.domain.model.TipoEventoUso
import com.app.lucka.domain.repository.UsoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * Cuenta un uso de la app cada vez que vuelve al primer plano: al abrirla desde el ícono o al
 * regresar desde otra app (p. ej. WhatsApp). Si la última vez fue hace menos de
 * [MINUTOS_ENTRE_USOS] minutos, no se cuenta de nuevo: así una rotación de pantalla o un vistazo
 * rápido a otra app no inflan el indicador.
 */
class RegistrarAperturaUseCase(private val repositorio: UsoRepository, private val reloj: Reloj) {
    suspend operator fun invoke() {
        val ahora = reloj.ahora()
        val ultima = repositorio.observar().first().lastOrNull { it.tipo == TipoEventoUso.APERTURA }?.fechaHora
        if (ultima != null && minutosEntre(ultima, ahora) < MINUTOS_ENTRE_USOS) return
        repositorio.registrar(EventoUso(TipoEventoUso.APERTURA, ahora))
    }

    private fun minutosEntre(desde: LocalDateTime, hasta: LocalDateTime): Long =
        (hasta.toInstant(TimeZone.UTC) - desde.toInstant(TimeZone.UTC)).inWholeMinutes

    companion object {
        const val MINUTOS_ENTRE_USOS = 5
    }
}

class ObservarResumenUsoUseCase(private val repositorio: UsoRepository, private val reloj: Reloj) {
    operator fun invoke(): Flow<ResumenUso> = repositorio.observar().map { ResumenUso.de(it, reloj.hoy()) }
}

/** Guarda la respuesta a "¿te resulta más fácil Lucka que tu cuaderno?". */
class ResponderEncuestaCuadernoUseCase(private val repositorio: UsoRepository, private val reloj: Reloj) {
    suspend operator fun invoke(masFacil: Boolean) = repositorio.registrar(
        EventoUso(
            tipo = TipoEventoUso.ENCUESTA_CUADERNO,
            fechaHora = reloj.ahora(),
            valor = if (masFacil) ResumenUso.RESPUESTA_SI else ResumenUso.RESPUESTA_NO,
        ),
    )
}
