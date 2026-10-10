package com.app.lucka.domain.usecase

import com.app.lucka.core.util.Reloj
import com.app.lucka.domain.model.Categoria
import com.app.lucka.domain.model.EstadoGasto
import com.app.lucka.domain.model.EventoUso
import com.app.lucka.domain.model.Gasto
import com.app.lucka.domain.model.Monto
import com.app.lucka.domain.model.OrigenGasto
import com.app.lucka.domain.model.SubcategoriaGasto
import com.app.lucka.domain.model.TipoEventoUso
import com.app.lucka.domain.model.aplicaA
import com.app.lucka.domain.model.enSoles
import com.app.lucka.domain.repository.ActividadRepository
import com.app.lucka.domain.repository.GastoRepository
import com.app.lucka.domain.repository.UsoRepository
import kotlinx.datetime.LocalDate

/** Crea un gasto nuevo o actualiza uno existente, validando los datos. */
class GuardarGastoUseCase(
    private val repositorio: GastoRepository,
    private val reloj: Reloj,
    private val generarId: () -> String,
    private val actividades: ActividadRepository,
    private val uso: UsoRepository,
    private val revisarAlertaTope: RevisarAlertaTopeUseCase,
) {
    sealed interface Resultado {
        data class Guardado(val gasto: Gasto) : Resultado
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
        subcategoria: SubcategoriaGasto? = null,
    ): Resultado {
        if (monto <= Monto.CERO) return Resultado.MontoInvalido
        if (fecha > reloj.hoy()) return Resultado.FechaFutura
        val descripcionLimpia = descripcion?.trim()?.takeIf { it.isNotEmpty() }

        val base = if (idExistente == null) {
            Gasto(
                id = generarId(),
                monto = monto,
                fecha = fecha,
                origen = OrigenGasto.TEXTO,
                estado = EstadoGasto.CONFIRMADO,
            )
        } else {
            repositorio.obtener(idExistente) ?: return Resultado.NoEncontrado
        }

        val gasto = base.copy(
            monto = monto,
            fecha = fecha,
            categoria = categoria,
            // Una subcategoría de casa no va en un gasto de negocio (ni al revés).
            subcategoria = subcategoria?.takeIf { it.aplicaA(categoria) },
            descripcion = descripcionLimpia,
            estado = EstadoGasto.CONFIRMADO,
            // La categoría la eligió el usuario: el backend no debe cambiarla.
            corregidoManualmente = true,
        )
        repositorio.guardar(gasto)

        val accion = if (idExistente == null) "Registraste" else "Editaste"
        val tipo = if (categoria == Categoria.NEGOCIO) "de negocio" else "personal"
        val nombre = descripcionLimpia?.let { " \"$it\"" }.orEmpty()
        actividades.registrar("$accion un gasto $tipo$nombre de ${monto.enSoles()}.", reloj.ahora())
        if (idExistente == null) uso.registrar(EventoUso(TipoEventoUso.REGISTRO, reloj.ahora()))
        revisarAlertaTope()
        return Resultado.Guardado(gasto)
    }
}
