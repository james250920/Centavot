package com.app.lucka.fakes

import com.app.lucka.core.util.Reloj
import com.app.lucka.domain.model.Actividad
import com.app.lucka.domain.model.Cobro
import com.app.lucka.domain.model.Contacto
import com.app.lucka.domain.model.Gasto
import com.app.lucka.domain.model.Notificacion
import com.app.lucka.domain.model.RegimenTributario
import com.app.lucka.domain.repository.ActividadRepository
import com.app.lucka.domain.repository.CobroRepository
import com.app.lucka.domain.repository.GastoRepository
import com.app.lucka.domain.repository.NotificacionRepository
import com.app.lucka.domain.repository.RegimenRepository
import com.app.lucka.domain.model.EventoUso
import com.app.lucka.domain.model.Ingreso
import com.app.lucka.domain.repository.IngresoRepository
import com.app.lucka.domain.repository.UsoRepository
import com.app.lucka.domain.usecase.ObservarEstadoTopeUseCase
import com.app.lucka.domain.usecase.RevisarAlertaTopeUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

class FakeGastoRepository(iniciales: List<Gasto> = emptyList()) : GastoRepository {
    val gastos = MutableStateFlow(iniciales)

    override fun observarGastos(): Flow<List<Gasto>> = gastos.map { it.sortedByDescending(Gasto::fecha) }

    override fun observarGastosEntre(desde: LocalDate, hasta: LocalDate): Flow<List<Gasto>> =
        observarGastos().map { lista -> lista.filter { it.fecha in desde..hasta } }

    override suspend fun obtener(id: String): Gasto? = gastos.value.firstOrNull { it.id == id }

    override suspend fun guardar(gasto: Gasto) = gastos.update { lista -> lista.filterNot { it.id == gasto.id } + gasto }

    override suspend fun eliminar(id: String) = gastos.update { lista -> lista.filterNot { it.id == id } }
}

class FakeRegimenRepository(inicial: RegimenTributario? = null) : RegimenRepository {
    val regimen = MutableStateFlow(inicial)

    override fun observarRegimen(): Flow<RegimenTributario?> = regimen

    override suspend fun guardar(regimen: RegimenTributario) {
        this.regimen.value = regimen
    }

    override suspend fun opcionesDisponibles(): List<RegimenTributario> = emptyList()
}

class FakeActividadRepository : ActividadRepository {
    val actividades = MutableStateFlow<List<Actividad>>(emptyList())

    override fun observar(limite: Int?): Flow<List<Actividad>> =
        actividades.map { lista -> lista.sortedByDescending { it.id }.let { if (limite == null) it else it.take(limite) } }

    override suspend fun registrar(descripcion: String, fechaHora: LocalDateTime) =
        actividades.update { it + Actividad(it.size + 1L, descripcion, fechaHora) }
}

class FakeNotificacionRepository : NotificacionRepository {
    val notificaciones = MutableStateFlow<List<Notificacion>>(emptyList())

    override fun observarTodas(): Flow<List<Notificacion>> = notificaciones

    override fun observarNoLeidas(): Flow<Int> = notificaciones.map { lista -> lista.count { !it.leida } }

    override suspend fun agregarSiNoExiste(notificacion: Notificacion) =
        notificaciones.update { lista -> if (lista.any { it.id == notificacion.id }) lista else lista + notificacion }

    override suspend fun marcarTodasLeidas() = notificaciones.update { lista -> lista.map { it.copy(leida = true) } }
}

class FakeCobroRepository : CobroRepository {
    val contactos = MutableStateFlow<List<Contacto>>(emptyList())
    val cobros = MutableStateFlow<List<Cobro>>(emptyList())

    override fun observarContactos(): Flow<List<Contacto>> = contactos

    override suspend fun guardarContacto(contacto: Contacto) =
        contactos.update { lista -> lista.filterNot { it.id == contacto.id } + contacto }

    override suspend fun eliminarContacto(id: String) = contactos.update { lista -> lista.filterNot { it.id == id } }

    override fun observarCobros(): Flow<List<Cobro>> = cobros

    override suspend fun obtenerCobro(id: String): Cobro? = cobros.value.firstOrNull { it.id == id }

    override suspend fun guardarCobro(cobro: Cobro) = cobros.update { lista -> lista.filterNot { it.id == cobro.id } + cobro }

    override suspend fun eliminarCobro(id: String) = cobros.update { lista -> lista.filterNot { it.id == id } }
}

class FakeIngresoRepository(iniciales: List<Ingreso> = emptyList()) : IngresoRepository {
    val ingresos = MutableStateFlow(iniciales)

    override fun observarIngresos(): Flow<List<Ingreso>> = ingresos.map { it.sortedByDescending(Ingreso::fecha) }

    override fun observarIngresosEntre(desde: LocalDate, hasta: LocalDate): Flow<List<Ingreso>> =
        observarIngresos().map { lista -> lista.filter { it.fecha in desde..hasta } }

    override suspend fun obtener(id: String): Ingreso? = ingresos.value.firstOrNull { it.id == id }

    override suspend fun guardar(ingreso: Ingreso) = ingresos.update { lista -> lista.filterNot { it.id == ingreso.id } + ingreso }

    override suspend fun eliminar(id: String) = ingresos.update { lista -> lista.filterNot { it.id == id } }
}

class FakeUsoRepository : UsoRepository {
    val eventos = MutableStateFlow<List<EventoUso>>(emptyList())

    override fun observar(): Flow<List<EventoUso>> = eventos

    override suspend fun registrar(evento: EventoUso) = eventos.update { it + evento }
}

/** Alerta de tope armada con fakes, para los casos de uso que la necesitan. */
fun revisarAlertaTope(
    gastos: GastoRepository,
    regimenes: RegimenRepository,
    notificaciones: NotificacionRepository,
    reloj: Reloj,
    ingresos: IngresoRepository = FakeIngresoRepository(),
) = RevisarAlertaTopeUseCase(ObservarEstadoTopeUseCase(ingresos, gastos, regimenes, reloj), notificaciones, reloj)
