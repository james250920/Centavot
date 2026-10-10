package com.app.lucka.domain.usecase

import com.app.lucka.core.util.Reloj
import com.app.lucka.domain.model.NivelAlerta
import com.app.lucka.domain.model.Notificacion
import com.app.lucka.domain.model.PeriodoTope
import com.app.lucka.domain.model.enSoles
import com.app.lucka.domain.repository.NotificacionRepository
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
 * Deja un aviso en Notificaciones cuando las ventas (o, en el Nuevo RUS, las compras) del
 * negocio cruzan el 80, 90 o 100 % del tope (RF-07). Cada umbral se avisa una sola vez por
 * periodo y por medida.
 */
class RevisarAlertaTopeUseCase(
    private val observarEstadoTope: ObservarEstadoTopeUseCase,
    private val notificaciones: NotificacionRepository,
    private val reloj: Reloj,
) {
    suspend operator fun invoke() {
        val estado = observarEstadoTope().first() ?: return
        val regimen = estado.regimen
        val hoy = reloj.hoy()
        val (periodo, cuando) = when (regimen.periodo) {
            PeriodoTope.MENSUAL -> "${hoy.year}-${hoy.month.ordinal + 1}" to "este mes"
            PeriodoTope.ANUAL -> "${hoy.year}" to "este año"
        }
        estado.medidas.forEach { (medida, proximidad) ->
            val nivel = proximidad.nivelAlerta
            if (nivel == NivelAlerta.NINGUNA) return@forEach
            val llevas = "Llevas ${proximidad.acumulado.enSoles()} en ${medida.etiqueta} del negocio $cuando, " +
                "de un tope de ${proximidad.tope.enSoles()} (${regimen.nombre})."
            val (asunto, consejo) = when (nivel) {
                NivelAlerta.AVISO_80 -> "Pasaste el 80 % de tu tope" to
                    "Te quedan ${proximidad.restante.enSoles()} $cuando."
                NivelAlerta.AVISO_90 -> "Estás al 90 % de tu tope" to
                    "Te quedan ${proximidad.restante.enSoles()}. Consulta con tu contador."
                else -> "Llegaste al tope de tu régimen" to
                    "Habla con tu contador: puede que te toque cambiar de categoría o de régimen."
            }
            notificaciones.agregarSiNoExiste(
                Notificacion(
                    id = "tope-${regimen.tipo}-${regimen.tope.centimos}-${medida.name}-$periodo-${nivel.umbralPorcentaje}",
                    asunto = asunto,
                    mensaje = "$llevas $consejo",
                    fechaHora = reloj.ahora(),
                ),
            )
        }
    }
}
