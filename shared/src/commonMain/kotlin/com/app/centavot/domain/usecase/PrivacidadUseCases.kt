package com.app.centavot.domain.usecase

import com.app.centavot.core.util.CompartidorArchivos
import com.app.centavot.core.util.Reloj
import com.app.centavot.domain.model.EventoUso
import com.app.centavot.domain.model.TipoEventoUso
import com.app.centavot.domain.model.VERSION_AVISO_PRIVACIDAD
import com.app.centavot.domain.model.exportarTodoCsv
import com.app.centavot.domain.repository.ActividadRepository
import com.app.centavot.domain.repository.CobroRepository
import com.app.centavot.domain.repository.DatosRepository
import com.app.centavot.domain.repository.GastoRepository
import com.app.centavot.domain.repository.IngresoRepository
import com.app.centavot.domain.repository.PerfilRepository
import com.app.centavot.domain.repository.RegimenRepository
import com.app.centavot.domain.repository.UsoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** true si el usuario aceptó la versión vigente del aviso de privacidad. */
class ObservarConsentimientoUseCase(private val uso: UsoRepository) {
    operator fun invoke(): Flow<Boolean> = uso.observar().map { eventos ->
        eventos.any { it.tipo == TipoEventoUso.CONSENTIMIENTO_PRIVACIDAD && it.valor == VERSION_AVISO_PRIVACIDAD }
    }
}

/** Guarda que el usuario aceptó el aviso de privacidad vigente, con la fecha y la versión. */
class AceptarAvisoPrivacidadUseCase(private val uso: UsoRepository, private val reloj: Reloj) {
    suspend operator fun invoke() =
        uso.registrar(EventoUso(TipoEventoUso.CONSENTIMIENTO_PRIVACIDAD, reloj.ahora(), VERSION_AVISO_PRIVACIDAD))
}

/**
 * Derecho de acceso (Ley 29733): arma un solo archivo, que se abre en Excel, con todo lo que la
 * app guarda del usuario. Sirve también como copia de respaldo hecha por él mismo.
 */
class ExportarTodosLosDatosUseCase(
    private val perfiles: PerfilRepository,
    private val regimenes: RegimenRepository,
    private val ingresos: IngresoRepository,
    private val gastos: GastoRepository,
    private val cobros: CobroRepository,
    private val actividades: ActividadRepository,
    private val reloj: Reloj,
) {
    data class Archivo(val nombre: String, val contenido: String)

    suspend operator fun invoke(): Archivo = Archivo(
        nombre = "centavot-mis-datos-${reloj.hoy()}.csv",
        contenido = exportarTodoCsv(
            perfil = perfiles.observarPerfil().first(),
            regimen = regimenes.observarRegimen().first(),
            ingresos = ingresos.observarIngresos().first(),
            gastos = gastos.observarGastos().first(),
            contactos = cobros.observarContactos().first(),
            cobros = cobros.observarCobros().first(),
            actividades = actividades.observar(limite = null).first(),
            generado = reloj.ahora(),
        ),
    )
}

/**
 * Derecho de cancelación (Ley 29733): borra todo lo que la app guarda en el teléfono, incluidos
 * los archivos exportados que quedaron en la memoria temporal. Después la app vuelve a empezar.
 */
class BorrarTodosLosDatosUseCase(
    private val datos: DatosRepository,
    private val compartidor: CompartidorArchivos,
) {
    suspend operator fun invoke() {
        datos.borrarTodo()
        compartidor.borrarArchivos()
    }
}
