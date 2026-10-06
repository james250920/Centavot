package com.app.centavot.domain.usecase

import com.app.centavot.core.util.Reloj
import com.app.centavot.domain.model.Categoria
import com.app.centavot.domain.model.Cobro
import com.app.centavot.domain.model.Contacto
import com.app.centavot.domain.model.EstadoCobro
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.ResumenCobros
import com.app.centavot.domain.model.TipoCobro
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

class ObtenerCobroUseCase(private val repositorio: CobroRepository) {
    suspend operator fun invoke(id: String): Cobro? = repositorio.obtenerCobro(id)
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

/** Registra un cobro nuevo o edita uno pendiente ([idExistente]). */
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

        /** El adelanto no puede ser negativo ni cubrir todo el monto (entonces ya no te debe). */
        data object AdelantoInvalido : Resultado

        /** Al editar, el nuevo monto debe ser mayor que lo que ya pagó (adelanto + abonos). */
        data object MontoMenorQueLoPagado : Resultado

        /** No existe o ya está cobrado (un cobro cerrado no se edita). */
        data object NoEditable : Resultado
    }

    suspend operator fun invoke(
        contacto: Contacto,
        motivo: String,
        monto: Monto,
        fecha: LocalDate,
        tipo: TipoCobro = TipoCobro.FIADO,
        adelanto: Monto = Monto.CERO,
        idExistente: String? = null,
    ): Resultado {
        val motivoLimpio = motivo.trim()
        if (motivoLimpio.isEmpty()) return Resultado.MotivoVacio
        if (monto <= Monto.CERO) return Resultado.MontoInvalido
        if (adelanto < Monto.CERO || adelanto >= monto) return Resultado.AdelantoInvalido
        if (fecha > reloj.hoy()) return Resultado.FechaFutura

        val existente = idExistente?.let { id ->
            repositorio.obtenerCobro(id)?.takeIf { it.estaPendiente } ?: return Resultado.NoEditable
        }
        val abonado = existente?.abonado ?: Monto.CERO
        if (adelanto + abonado >= monto) return Resultado.MontoMenorQueLoPagado

        val cobro = existente?.copy(contacto = contacto, motivo = motivoLimpio, monto = monto, fecha = fecha, tipo = tipo, adelanto = adelanto)
            ?: Cobro(generarId(), contacto, motivoLimpio, monto, fecha, tipo = tipo, adelanto = adelanto)
        repositorio.guardarCobro(cobro)

        val queEs = if (tipo == TipoCobro.PEDIDO) "un pedido" else "un cobro"
        val conAdelanto = if (adelanto > Monto.CERO) " con adelanto de ${adelanto.enSoles()}" else ""
        val accion = if (existente == null) "Registraste" else "Editaste"
        actividades.registrar(
            "$accion $queEs a ${contacto.nombre} por ${monto.enSoles()}$conAdelanto (\"$motivoLimpio\").",
            reloj.ahora(),
        )
        return Resultado.Registrado(cobro)
    }
}

/**
 * Regla D1: un fiado o un pedido cuenta como venta **cuando se registra** (se entregó el producto
 * o se tomó el pedido), no cuando se cobra. Si [contarComoVenta] es true, al crear el cobro se
 * registra también la venta del negocio por el total, con la misma fecha y el motivo como
 * descripción. Cobrar o abonar después no vuelve a sumar ventas, así nada se cuenta dos veces.
 */
class RegistrarCobroYVentaUseCase(
    private val registrarCobro: RegistrarCobroUseCase,
    private val guardarIngreso: GuardarIngresoUseCase,
) {
    suspend operator fun invoke(
        contacto: Contacto,
        motivo: String,
        monto: Monto,
        fecha: LocalDate,
        tipo: TipoCobro,
        adelanto: Monto,
        idExistente: String?,
        contarComoVenta: Boolean,
    ): RegistrarCobroUseCase.Resultado {
        val resultado = registrarCobro(contacto, motivo, monto, fecha, tipo, adelanto, idExistente)
        if (resultado is RegistrarCobroUseCase.Resultado.Registrado && idExistente == null && contarComoVenta) {
            guardarIngreso(null, monto, Categoria.NEGOCIO, fecha, resultado.cobro.motivo)
        }
        return resultado
    }
}

/** Pago parcial de un fiado o pedido. Si cubre todo el saldo, el cobro queda cobrado. */
class RegistrarAbonoUseCase(
    private val repositorio: CobroRepository,
    private val actividades: ActividadRepository,
    private val reloj: Reloj,
) {
    sealed interface Resultado {
        data class Abonado(val cobro: Cobro) : Resultado
        data object MontoInvalido : Resultado
        data object MayorQueElSaldo : Resultado
        data object NoEncontrado : Resultado
    }

    suspend operator fun invoke(id: String, monto: Monto): Resultado {
        val cobro = repositorio.obtenerCobro(id)?.takeIf { it.estaPendiente } ?: return Resultado.NoEncontrado
        if (monto <= Monto.CERO) return Resultado.MontoInvalido
        if (monto > cobro.saldo) return Resultado.MayorQueElSaldo

        val conAbono = cobro.copy(abonado = cobro.abonado + monto)
        val actualizado = if (conAbono.saldo == Monto.CERO) {
            conAbono.copy(estado = EstadoCobro.COBRADO, fechaCobrado = reloj.hoy())
        } else {
            conAbono
        }
        repositorio.guardarCobro(actualizado)
        val cierre = if (actualizado.estaPendiente) "Le falta ${actualizado.saldo.enSoles()}." else "Terminó de pagar."
        actividades.registrar(
            "${cobro.contacto.nombre} abonó ${monto.enSoles()} a \"${cobro.motivo}\". $cierre",
            reloj.ahora(),
        )
        return Resultado.Abonado(actualizado)
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
            "${cobro.contacto.nombre} te pagó ${cobro.saldo.enSoles()} (\"${cobro.motivo}\").",
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
