package com.app.centavot.domain.usecase

import com.app.centavot.core.util.Reloj
import com.app.centavot.domain.model.NivelAlerta
import com.app.centavot.domain.model.Notificacion
import com.app.centavot.domain.model.PeriodoTope
import com.app.centavot.domain.model.enSoles
import com.app.centavot.domain.repository.NotificacionRepository
import com.app.centavot.domain.repository.RegimenRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ObservarNotificacionesUseCase(private val repositorio: NotificacionRepository) {
    operator fun invoke(): Flow<List<Notificacion>> = repositorio.observarTodas()
}

class ObservarNoLeidasUseCase(private val repositorio: NotificacionRepository) {
    operator fun invoke(): Flow<Int> = repositorio.observarNoLeidas()
}

class MarcarNotificacionesLeidasUseCase(private val repositorio: NotificacionRepository) {
    suspend operator fun invoke() = repositorio.marcarTodasLeidas()
}

/**
 * Deja un aviso en Notificaciones cuando los gastos de negocio cruzan el 80, 90 o
 * 100 % del tope (RF-07). Cada umbral se avisa una sola vez por periodo.
 */
class RevisarAlertaTopeUseCase(
    private val observarProximidad: ObservarProximidadTopeUseCase,
    private val regimenes: RegimenRepository,
    private val notificaciones: NotificacionRepository,
    private val reloj: Reloj,
) {
    suspend operator fun invoke() {
        val regimen = regimenes.observarRegimen().first() ?: return
        val proximidad = observarProximidad().first() ?: return
        val nivel = proximidad.nivelAlerta
        if (nivel == NivelAlerta.NINGUNA) return

        val hoy = reloj.hoy()
        val (periodo, cuando) = when (regimen.periodo) {
            PeriodoTope.MENSUAL -> "${hoy.year}-${hoy.month.ordinal + 1}" to "este mes"
            PeriodoTope.ANUAL -> "${hoy.year}" to "este año"
        }
        val llevas = "Llevas ${proximidad.acumulado.enSoles()} en gastos de negocio de un tope de " +
            "${proximidad.tope.enSoles()} (${regimen.nombre})."
        val (asunto, consejo) = when (nivel) {
            NivelAlerta.AVISO_80 -> "Pasaste el 80 % de tu tope" to
                "Te quedan ${proximidad.restante.enSoles()} $cuando."
            NivelAlerta.AVISO_90 -> "Estás al 90 % de tu tope" to
                "Te quedan ${proximidad.restante.enSoles()}. Consulta con tu contador."
            else -> "Llegaste al tope de tu régimen" to
                "Habla con tu contador para evitar una multa."
        }
        notificaciones.agregarSiNoExiste(
            Notificacion(
                id = "tope-${regimen.tipo}-${regimen.tope.centimos}-$periodo-${nivel.umbralPorcentaje}",
                asunto = asunto,
                mensaje = "$llevas $consejo",
                fechaHora = reloj.ahora(),
            ),
        )
    }
}
