package com.app.lucka.domain.usecase

import com.app.lucka.core.util.Reloj
import com.app.lucka.domain.model.Categoria
import com.app.lucka.domain.model.EventoUso
import com.app.lucka.domain.model.Ingreso
import com.app.lucka.domain.model.Monto
import com.app.lucka.domain.model.TipoEventoUso
import com.app.lucka.domain.model.VentaFrecuente
import com.app.lucka.domain.model.enSoles
import com.app.lucka.domain.model.ventasFrecuentes
import com.app.lucka.domain.repository.ActividadRepository
import com.app.lucka.domain.repository.IngresoRepository
import com.app.lucka.domain.repository.UsoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class ObservarIngresosUseCase(private val repositorio: IngresoRepository) {
    operator fun invoke(): Flow<List<Ingreso>> = repositorio.observarIngresos()
}

class ObtenerIngresoUseCase(private val repositorio: IngresoRepository) {
    suspend operator fun invoke(id: String): Ingreso? = repositorio.obtener(id)
}

/** Las ventas que más se repiten, para registrarlas con un toque. */
class ObservarVentasFrecuentesUseCase(private val repositorio: IngresoRepository) {
    operator fun invoke(): Flow<List<VentaFrecuente>> = repositorio.observarIngresos().map { it.ventasFrecuentes() }
}

/** Registra una venta (o un ingreso personal) nueva, o edita una existente. */
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
    ): Resultado {
        if (monto <= Monto.CERO) return Resultado.MontoInvalido
        if (fecha > reloj.hoy()) return Resultado.FechaFutura
        val descripcionLimpia = descripcion?.trim()?.takeIf { it.isNotEmpty() }

        val id = if (idExistente == null) {
            generarId()
        } else {
            repositorio.obtener(idExistente)?.id ?: return Resultado.NoEncontrado
        }
        val ingreso = Ingreso(id, monto, fecha, categoria, descripcionLimpia)
        repositorio.guardar(ingreso)

        val accion = if (idExistente == null) "Registraste" else "Editaste"
        val que = if (categoria == Categoria.NEGOCIO) "una venta" else "un ingreso personal"
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
        val que = if (ingreso.esDeNegocio) "la venta" else "el ingreso"
        val nombre = ingreso.descripcion?.let { " \"$it\"" }.orEmpty()
        actividades.registrar("Eliminaste $que$nombre de ${ingreso.monto.enSoles()}.", reloj.ahora())
    }
}
