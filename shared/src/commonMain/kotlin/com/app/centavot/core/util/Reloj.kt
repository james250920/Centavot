package com.app.centavot.core.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Fuente de la fecha actual; se inyecta para poder fijarla en los tests. */
fun interface Reloj {
    fun hoy(): LocalDate

    /** Fecha y hora actual, para el historial de actividad y las notificaciones. */
    fun ahora(): LocalDateTime = hoy().atTime(0, 0)
}

@OptIn(ExperimentalTime::class)
object RelojSistema : Reloj {
    override fun hoy(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

    override fun ahora(): LocalDateTime {
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        // Sin fracciones de segundo: el texto guardado queda como "2026-09-29T16:13:09".
        return LocalDateTime(ahora.date, kotlinx.datetime.LocalTime(ahora.hour, ahora.minute, ahora.second))
    }
}
