package com.app.centavot.domain.usecase

import com.app.centavot.core.util.Reloj
import com.app.centavot.domain.model.Categoria
import com.app.centavot.domain.model.EventoUso
import com.app.centavot.domain.model.Ingreso
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.TipoEntrada
import com.app.centavot.domain.model.TipoEventoUso
import com.app.centavot.domain.model.tipo
import com.app.centavot.domain.model.VentaFrecuente
import com.app.centavot.domain.model.enSoles
import com.app.centavot.domain.model.ventasFrecuentes
import com.app.centavot.domain.repository.ActividadRepository
import com.app.centavot.domain.repository.IngresoRepository
import com.app.centavot.domain.repository.UsoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class ObservarIngresosUseCase(private val repositorio: IngresoRepository) {
    operator fun invoke(): Flow<List<Ingreso>> = repositorio.observarIngresos()
}

class ObtenerIngresoUseCase(private val repositorio: IngresoRepository) {
    suspend operator fun invoke(id: String): Ingreso? = repositorio.obtener(id)
}

/** Las ventas (o ingresos personales) que más se repiten, para registrarlos con un toque. */
class ObservarVentasFrecuentesUseCase(private val repositorio: IngresoRepository) {
    operator fun invoke(categoria: Categoria = Categoria.NEGOCIO): Flow<List<VentaFrecuente>> =
        repositorio.observarIngresos().map { it.ventasFrecuentes(categoria) }
}

/**
 * Registra una venta, un ingreso personal o un retiro para la casa, o edita uno existente.
 * Un retiro siempre es personal: sale de la caja del negocio y entra a la plata de la casa.
 */
class GuardarIngresoUseCase(
    private val repositorio: IngresoRepository,
    private val reloj: Reloj,
    private val generarId: () -> String,
    private val actividades: ActividadRepository,
    private val uso: UsoRepository,
    private val revisarAlertaTope: RevisarAlertaTopeUseCase,
) {
    sealed interface Resultado {
        data class Guardado(val ingreso: Ingreso) : Resultado
        data object MontoInvalido : Resultado
        data object FechaFutura : Resultado
        data object NoEncontrado : Resultado
    }

    suspend operator fun invoke(
        idExistente: String?,
        monto: Monto,
        categoria: Categoria,
        fecha: LocalDate,
        descripcion: String?,
        retiroDelNegocio: Boolean = false,
    ): Resultado {
        if (monto <= Monto.CERO) return Resultado.MontoInvalido
        if (fecha > reloj.hoy()) return Resultado.FechaFutura
        val descripcionLimpia = descripcion?.trim()?.takeIf { it.isNotEmpty() }

        val id = if (idExistente == null) {
            generarId()
        } else {
            repositorio.obtener(idExistente)?.id ?: return Resultado.NoEncontrado
        }
        val ingreso = Ingreso(
            id = id,
            monto = monto,
            fecha = fecha,
            categoria = if (retiroDelNegocio) Categoria.PERSONAL else categoria,
            descripcion = descripcionLimpia,
            retiroDelNegocio = retiroDelNegocio,
        )
        repositorio.guardar(ingreso)

        val accion = if (idExistente == null) "Registraste" else "Editaste"
        val que = when (ingreso.tipo) {
            TipoEntrada.VENTA -> "una venta"
            TipoEntrada.INGRESO -> "un ingreso personal"
            TipoEntrada.RETIRO -> "un retiro para la casa"
        }
        val nombre = descripcionLimpia?.let { " \"$it\"" }.orEmpty()
        actividades.registrar("$accion $que$nombre de ${monto.enSoles()}.", reloj.ahora())
        if (idExistente == null) uso.registrar(EventoUso(TipoEventoUso.REGISTRO, reloj.ahora()))
        revisarAlertaTope()
        return Resultado.Guardado(ingreso)
    }
}

/** Solo para corregir una venta registrada por error; queda anotado en Actividad. */
class EliminarIngresoUseCase(
    private val repositorio: IngresoRepository,
    private val actividades: ActividadRepository,
    private val reloj: Reloj,
) {
    suspend operator fun invoke(id: String) {
        val ingreso = repositorio.obtener(id) ?: return
        repositorio.eliminar(id)
        val que = when (ingreso.tipo) {
            TipoEntrada.VENTA -> "la venta"
            TipoEntrada.INGRESO -> "el ingreso"
            TipoEntrada.RETIRO -> "el retiro para la casa"
        }
        val nombre = ingreso.descripcion?.let { " \"$it\"" }.orEmpty()
        actividades.registrar("Eliminaste $que$nombre de ${ingreso.monto.enSoles()}.", reloj.ahora())
    }
}
