package com.app.centavot.core.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Fuente de la fecha actual; se inyecta para poder fijarla en los tests. */
fun interface Reloj {
    fun hoy(): LocalDate
}

@OptIn(ExperimentalTime::class)
object RelojSistema : Reloj {
    override fun hoy(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
}
