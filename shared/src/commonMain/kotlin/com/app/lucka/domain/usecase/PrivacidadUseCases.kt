package com.app.lucka.domain.usecase

import com.app.lucka.core.util.CompartidorArchivos
import com.app.lucka.core.util.Reloj
import com.app.lucka.domain.model.EventoUso
import com.app.lucka.domain.model.TipoEventoUso
import com.app.lucka.domain.model.VERSION_AVISO_PRIVACIDAD
import com.app.lucka.domain.model.exportarTodoCsv
import com.app.lucka.domain.repository.ActividadRepository
import com.app.lucka.domain.repository.CobroRepository
import com.app.lucka.domain.repository.DatosRepository
import com.app.lucka.domain.repository.GastoRepository
import com.app.lucka.domain.repository.IngresoRepository
import com.app.lucka.domain.repository.PerfilRepository
import com.app.lucka.domain.repository.RegimenRepository
import com.app.lucka.domain.repository.UsoRepository
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
        nombre = "lucka-mis-datos-${reloj.hoy()}.csv",
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
