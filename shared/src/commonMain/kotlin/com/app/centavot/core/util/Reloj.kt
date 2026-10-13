package com.app.centavot.core.util

import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

/** Fuente de la fecha actual; se inyecta para poder fijarla en los tests. */
fun interface Reloj {
    fun hoy(): LocalDate

    /** Fecha y hora actual, para el historial de actividad y las notificaciones. */
    fun ahora(): LocalDateTime = hoy().atTime(0, 0)

    /**
     * El día de hoy, y de nuevo cada vez que cambia. Lo que se calcula "para hoy" (la caja, el mes, el
     * tope) lo sigue para no quedarse en el día anterior si la app queda abierta pasada la medianoche.
     */
    fun dias(): Flow<LocalDate> = flowOf(hoy())
}

@OptIn(ExperimentalTime::class)
object RelojSistema : Reloj {
    override fun hoy(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

    override fun ahora(): LocalDateTime {
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        // Sin fracciones de segundo: el texto guardado queda como "2026-09-29T16:13:09".
        return LocalDateTime(ahora.date, kotlinx.datetime.LocalTime(ahora.hour, ahora.minute, ahora.second))
    }

    // Revisa una vez por minuto: así también sigue un cambio de hora o de zona del teléfono.
    override fun dias(): Flow<LocalDate> = flow {
        while (true) {
            emit(hoy())
            delay(1.minutes)
        }
    }.distinctUntilChanged()
}
