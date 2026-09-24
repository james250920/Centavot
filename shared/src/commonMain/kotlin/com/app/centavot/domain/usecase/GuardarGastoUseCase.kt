package com.app.centavot.domain.usecase

import com.app.centavot.core.util.Reloj
import com.app.centavot.domain.model.Categoria
import com.app.centavot.domain.model.EstadoGasto
import com.app.centavot.domain.model.Gasto
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.OrigenGasto
import com.app.centavot.domain.repository.GastoRepository
import kotlinx.datetime.LocalDate

/** Crea un gasto nuevo o actualiza uno existente, validando los datos. */
class GuardarGastoUseCase(
    private val repositorio: GastoRepository,
    private val reloj: Reloj,
    private val generarId: () -> String,
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
            descripcion = descripcionLimpia,
            estado = EstadoGasto.CONFIRMADO,
            // La categoría la eligió el usuario: el backend no debe cambiarla.
            corregidoManualmente = true,
        )
        repositorio.guardar(gasto)
        return Resultado.Guardado(gasto)
    }
}
