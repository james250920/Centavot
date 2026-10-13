package com.app.centavot.domain.usecase

import com.app.centavot.core.util.Reloj
import com.app.centavot.domain.model.AbonoMeta
import com.app.centavot.domain.model.Meta
import com.app.centavot.domain.model.Modo
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.Notificacion
import com.app.centavot.domain.model.ProgresoMeta
import com.app.centavot.domain.model.enSoles
import com.app.centavot.domain.model.sumar
import com.app.centavot.domain.repository.ActividadRepository
import com.app.centavot.domain.repository.MetaRepository
import com.app.centavot.domain.repository.NotificacionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate

private const val MAX_NOMBRE_META = 40

private fun progresos(metas: List<Meta>, abonos: List<AbonoMeta>): List<ProgresoMeta> = metas.map { meta ->
    ProgresoMeta(meta, abonos.filter { it.metaId == meta.id }.map { it.monto }.sumar())
}

/** Las metas del modo con su avance: primero las que faltan, luego las cumplidas. */
class ObservarMetasUseCase(private val repositorio: MetaRepository) {
    operator fun invoke(modo: Modo): Flow<List<ProgresoMeta>> =
        combine(repositorio.observarMetas(), repositorio.observarAbonos()) { metas, abonos ->
            progresos(metas.filter { it.categoria == modo.categoria }, abonos).sortedBy { it.cumplida }
        }
}

/** Crea una meta nueva ([idExistente] null) o la edita. */
class GuardarMetaUseCase(
    private val repositorio: MetaRepository,
    private val actividades: ActividadRepository,
    private val reloj: Reloj,
    private val generarId: () -> String,
) {
    sealed interface Resultado {
        data class Guardada(val meta: Meta) : Resultado
        data object NombreVacio : Resultado
        data object MontoInvalido : Resultado
        data object FechaPasada : Resultado
    }

    suspend operator fun invoke(idExistente: String?, modo: Modo, nombre: String, objetivo: Monto, fechaLimite: LocalDate?): Resultado {
        val limpio = nombre.trim().take(MAX_NOMBRE_META)
        if (limpio.isEmpty()) return Resultado.NombreVacio
        if (objetivo <= Monto.CERO) return Resultado.MontoInvalido
        if (fechaLimite != null && fechaLimite <= reloj.hoy()) return Resultado.FechaPasada
        val anterior = idExistente?.let { id -> repositorio.observarMetas().first().firstOrNull { it.id == id } }
        val meta = Meta(
            id = anterior?.id ?: generarId(),
            nombre = limpio,
            objetivo = objetivo,
            categoria = anterior?.categoria ?: modo.categoria,
            creada = anterior?.creada ?: reloj.hoy(),
            fechaLimite = fechaLimite,
        )
        repositorio.guardarMeta(meta)
        val accion = if (anterior == null) "Te pusiste la meta" else "Cambiaste la meta"
        actividades.registrar("$accion \"$limpio\" de ${objetivo.enSoles()}.", reloj.ahora())
        return Resultado.Guardada(meta)
    }
}

/**
 * Separa plata para una meta. Al cruzar el 25, 50, 75 o 100 % deja un aviso en Notificaciones, una sola vez
 * por hito, como pidieron en la reunión ("que te avise cuánto llevas cada vez que le abonas").
 */
class AbonarMetaUseCase(
    private val repositorio: MetaRepository,
    private val notificaciones: NotificacionRepository,
    private val actividades: ActividadRepository,
    private val reloj: Reloj,
    private val generarId: () -> String,
) {
    sealed interface Resultado {
        /** [hito]: el porcentaje que se acaba de cruzar, si cruzó alguno. */
        data class Abonado(val abono: AbonoMeta, val progreso: ProgresoMeta, val hito: Int?) : Resultado
        data object MontoInvalido : Resultado
        data object NoEncontrada : Resultado
    }

    suspend operator fun invoke(metaId: String, monto: Monto): Resultado {
        if (monto <= Monto.CERO) return Resultado.MontoInvalido
        val meta = repositorio.observarMetas().first().firstOrNull { it.id == metaId } ?: return Resultado.NoEncontrada
        val antes = progresos(listOf(meta), repositorio.observarAbonos().first()).single()
        val abono = AbonoMeta(generarId(), metaId, monto, reloj.hoy())
        repositorio.agregarAbono(abono)
        val despues = ProgresoMeta(meta, antes.ahorrado + monto)
        val hito = despues.hitoDesde(antes)
        actividades.registrar("Separaste ${monto.enSoles()} para \"${meta.nombre}\". Llevas ${despues.porcentaje} %.", reloj.ahora())
        if (hito != null) {
            notificaciones.agregarSiNoExiste(
                Notificacion(
                    id = "meta-${meta.id}-$hito",
                    asunto = if (hito == 100) "¡Cumpliste tu meta \"${meta.nombre}\"!" else "Vas $hito % de \"${meta.nombre}\"",
                    mensaje = "Llevas ${despues.ahorrado.enSoles()} de ${meta.objetivo.enSoles()}." +
                        if (hito == 100) "" else " Te faltan ${despues.falta.enSoles()}.",
                    fechaHora = reloj.ahora(),
                ),
            )
        }
        return Resultado.Abonado(abono, despues, hito)
    }
}

/** Para "Deshacer" un abono hecho por error. */
class QuitarAbonoMetaUseCase(private val repositorio: MetaRepository) {
    suspend operator fun invoke(abonoId: String) = repositorio.eliminarAbono(abonoId)
}

class EliminarMetaUseCase(
    private val repositorio: MetaRepository,
    private val actividades: ActividadRepository,
    private val reloj: Reloj,
) {
    suspend operator fun invoke(meta: Meta) {
        repositorio.eliminarMeta(meta.id)
        actividades.registrar("Quitaste la meta \"${meta.nombre}\".", reloj.ahora())
    }
}
