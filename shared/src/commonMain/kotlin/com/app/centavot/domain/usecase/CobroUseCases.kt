package com.app.centavot.domain.usecase

import com.app.centavot.core.util.Reloj
import com.app.centavot.domain.model.Cobro
import com.app.centavot.domain.model.Contacto
import com.app.centavot.domain.model.EstadoCobro
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.ResumenCobros
import com.app.centavot.domain.model.enSoles
import com.app.centavot.domain.repository.ActividadRepository
import com.app.centavot.domain.repository.CobroRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class ObservarContactosUseCase(private val repositorio: CobroRepository) {
    operator fun invoke(): Flow<List<Contacto>> = repositorio.observarContactos()
}

class ObservarCobrosUseCase(private val repositorio: CobroRepository) {
    operator fun invoke(): Flow<List<Cobro>> = repositorio.observarCobros()
}

class ObservarResumenCobrosUseCase(private val repositorio: CobroRepository) {
    operator fun invoke(): Flow<ResumenCobros> = repositorio.observarCobros().map(ResumenCobros::de)
}

class AgregarContactoUseCase(
    private val repositorio: CobroRepository,
    private val actividades: ActividadRepository,
    private val reloj: Reloj,
    private val generarId: () -> String,
) {
    sealed interface Resultado {
        data class Agregado(val contacto: Contacto) : Resultado
        data object NombreVacio : Resultado
        data object Repetido : Resultado
    }

    suspend operator fun invoke(nombre: String, telefono: String?): Resultado {
        val nombreLimpio = nombre.trim()
        if (nombreLimpio.isEmpty()) return Resultado.NombreVacio
        val existentes = repositorio.observarContactos().first()
        if (existentes.any { it.nombre.equals(nombreLimpio, ignoreCase = true) }) return Resultado.Repetido
        val contacto = Contacto(generarId(), nombreLimpio, telefono?.trim()?.takeIf { it.isNotEmpty() })
        repositorio.guardarContacto(contacto)
        actividades.registrar("Agregaste a ${contacto.nombre} a tus contactos.", reloj.ahora())
        return Resultado.Agregado(contacto)
    }
}

class EliminarContactoUseCase(
    private val repositorio: CobroRepository,
    private val actividades: ActividadRepository,
    private val reloj: Reloj,
) {
    sealed interface Resultado {
        data object Eliminado : Resultado

        /** Tiene cobros registrados: se conservan para no perder el historial. */
        data object TieneCobros : Resultado
    }

    suspend operator fun invoke(contacto: Contacto): Resultado {
        if (repositorio.observarCobros().first().any { it.contacto.id == contacto.id }) return Resultado.TieneCobros
        repositorio.eliminarContacto(contacto.id)
        actividades.registrar("Eliminaste a ${contacto.nombre} de tus contactos.", reloj.ahora())
        return Resultado.Eliminado
    }
}

class RegistrarCobroUseCase(
    private val repositorio: CobroRepository,
    private val actividades: ActividadRepository,
    private val reloj: Reloj,
    private val generarId: () -> String,
) {
    sealed interface Resultado {
        data class Registrado(val cobro: Cobro) : Resultado
        data object MontoInvalido : Resultado
        data object MotivoVacio : Resultado
        data object FechaFutura : Resultado
    }

    suspend operator fun invoke(contacto: Contacto, motivo: String, monto: Monto, fecha: LocalDate): Resultado {
        val motivoLimpio = motivo.trim()
        if (motivoLimpio.isEmpty()) return Resultado.MotivoVacio
        if (monto <= Monto.CERO) return Resultado.MontoInvalido
        if (fecha > reloj.hoy()) return Resultado.FechaFutura
        val cobro = Cobro(generarId(), contacto, motivoLimpio, monto, fecha)
        repositorio.guardarCobro(cobro)
        actividades.registrar(
            "Registraste un cobro a ${contacto.nombre} por ${monto.enSoles()} (\"$motivoLimpio\").",
            reloj.ahora(),
        )
        return Resultado.Registrado(cobro)
    }
}

/** Botón "Cobrar": marca el cobro como pagado. */
class MarcarCobradoUseCase(
    private val repositorio: CobroRepository,
    private val actividades: ActividadRepository,
    private val reloj: Reloj,
) {
    suspend operator fun invoke(id: String) {
        val cobro = repositorio.obtenerCobro(id) ?: return
        if (!cobro.estaPendiente) return
        repositorio.guardarCobro(cobro.copy(estado = EstadoCobro.COBRADO, fechaCobrado = reloj.hoy()))
        actividades.registrar(
            "${cobro.contacto.nombre} te pagó ${cobro.monto.enSoles()} (\"${cobro.motivo}\").",
            reloj.ahora(),
        )
    }
}

/** Solo para corregir un cobro registrado por error; queda anotado en Actividad. */
class EliminarCobroUseCase(
    private val repositorio: CobroRepository,
    private val actividades: ActividadRepository,
    private val reloj: Reloj,
) {
    suspend operator fun invoke(id: String) {
        val cobro = repositorio.obtenerCobro(id) ?: return
        repositorio.eliminarCobro(id)
        actividades.registrar(
            "Eliminaste el cobro \"${cobro.motivo}\" a ${cobro.contacto.nombre} de ${cobro.monto.enSoles()}.",
            reloj.ahora(),
        )
    }
}
