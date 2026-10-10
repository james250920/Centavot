package com.app.lucka.domain.usecase

import com.app.lucka.core.util.Reloj
import com.app.lucka.domain.model.RegimenTributario
import com.app.lucka.domain.repository.ActividadRepository
import com.app.lucka.domain.repository.RegimenRepository

class GuardarRegimenUseCase(
    private val repositorio: RegimenRepository,
    private val actividades: ActividadRepository,
    private val revisarAlertaTope: RevisarAlertaTopeUseCase,
    private val reloj: Reloj,
) {
    suspend operator fun invoke(regimen: RegimenTributario) {
        repositorio.guardar(regimen)
        actividades.registrar("Elegiste el régimen ${regimen.nombre}.", reloj.ahora())
        // Con otro tope, los gastos que ya tiene pueden cruzar un umbral.
        revisarAlertaTope()
    }
}
