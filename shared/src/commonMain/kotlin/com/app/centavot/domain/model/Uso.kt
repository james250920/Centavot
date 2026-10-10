package com.app.centavot.domain.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus

enum class TipoEventoUso {
    /** Se abrió la app. */
    APERTURA,

    /** Se registró una venta o un gasto nuevo. */
    REGISTRO,

    /** Respuesta a "¿te resulta más fácil que tu cuaderno?"; el valor es SI o NO. */
    ENCUESTA_CUADERNO,

    /** El usuario aceptó el aviso de privacidad; el valor es la versión del aviso que aceptó. */
    CONSENTIMIENTO_PRIVACIDAD,
}

/**
 * Versión vigente del aviso de privacidad. Si el aviso cambia, se sube la versión y la app
 * vuelve a pedir el consentimiento (Ley 29733).
 */
const val VERSION_AVISO_PRIVACIDAD = "2026-10"

/** Registro anónimo de uso, guardado solo en el teléfono, para medir si la app sirve. */
data class EventoUso(val tipo: TipoEventoUso, val fechaHora: LocalDateTime, val valor: String? = null)

/**
 * Indicadores de uso de la primera fase: uso diario, registros por día, que el uso no se
 * diluya y si la app le resulta más fácil que el cuaderno.
 */
data class ResumenUso(
    val hoy: LocalDate,
    val primerUso: LocalDate?,
    val aperturasHoy: Int,
    val registrosHoy: Int,
    /** Días con al menos un registro dentro de los últimos 7 (incluye hoy). */
    val diasConRegistroUltimos7: Int,
    /** Días con al menos un registro desde el primer uso. */
    val diasConRegistro: Int,
    val totalRegistros: Int,
    /** Respuestas a la pregunta del cuaderno, de la más antigua a la más reciente. */
    val respuestasCuaderno: List<Boolean>,
) {
    val diasDesdePrimerUso: Int get() = primerUso?.daysUntil(hoy) ?: 0

    /** Promedio de registros en los días en que el usuario registró algo. */
    val registrosPorDiaActivo: Int get() = if (diasConRegistro == 0) 0 else totalRegistros / diasConRegistro

    /** Se pregunta a los 7 días de uso y otra vez a los 30. */
    val debePreguntarCuaderno: Boolean
        get() = (diasDesdePrimerUso >= DIA_PRIMERA_PREGUNTA && respuestasCuaderno.isEmpty()) ||
            (diasDesdePrimerUso >= DIA_SEGUNDA_PREGUNTA && respuestasCuaderno.size == 1)

    companion object {
        const val DIA_PRIMERA_PREGUNTA = 7
        const val DIA_SEGUNDA_PREGUNTA = 30

        fun de(eventos: List<EventoUso>, hoy: LocalDate): ResumenUso {
            val registros = eventos.filter { it.tipo == TipoEventoUso.REGISTRO }
            val diasConRegistro = registros.map { it.fechaHora.date }.toSet()
            val haceUnaSemana = hoy.minus(6, DateTimeUnit.DAY)
            return ResumenUso(
                hoy = hoy,
                primerUso = eventos.minOfOrNull { it.fechaHora }?.date,
                aperturasHoy = eventos.count { it.tipo == TipoEventoUso.APERTURA && it.fechaHora.date == hoy },
                registrosHoy = registros.count { it.fechaHora.date == hoy },
                diasConRegistroUltimos7 = diasConRegistro.count { it in haceUnaSemana..hoy },
                diasConRegistro = diasConRegistro.size,
                totalRegistros = registros.size,
                respuestasCuaderno = eventos.filter { it.tipo == TipoEventoUso.ENCUESTA_CUADERNO }
                    .sortedBy { it.fechaHora }
                    .map { it.valor == RESPUESTA_SI },
            )
        }

        const val RESPUESTA_SI = "SI"
        const val RESPUESTA_NO = "NO"
    }
}
