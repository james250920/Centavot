package com.app.centavot.domain.usecase

import com.app.centavot.core.util.Reloj
import com.app.centavot.domain.model.enSoles
import com.app.centavot.domain.repository.ActividadRepository
import com.app.centavot.domain.repository.GastoRepository

/** Solo para corregir un gasto registrado por error; la eliminación queda en Actividad. */
class EliminarGastoUseCase(
    private val repositorio: GastoRepository,
    private val actividades: ActividadRepository,
    private val reloj: Reloj,
) {
    suspend operator fun invoke(id: String) {
        val gasto = repositorio.obtener(id) ?: return
        repositorio.eliminar(id)
        val nombre = gasto.descripcion?.let { " \"$it\"" }.orEmpty()
        actividades.registrar("Eliminaste el gasto$nombre de ${gasto.monto.enSoles()}.", reloj.ahora())
    }
}
