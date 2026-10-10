package com.app.lucka.data.repository

import com.app.lucka.data.local.ActividadDao
import com.app.lucka.data.local.ActividadEntity
import com.app.lucka.data.local.CobroDao
import com.app.lucka.data.local.NotificacionDao
import com.app.lucka.data.local.PerfilDao
import com.app.lucka.data.mapper.toDomain
import com.app.lucka.data.mapper.toEntity
import com.app.lucka.domain.model.Actividad
import com.app.lucka.domain.model.Cobro
import com.app.lucka.domain.model.Contacto
import com.app.lucka.domain.model.Notificacion
import com.app.lucka.domain.model.Perfil
import com.app.lucka.domain.repository.ActividadRepository
import com.app.lucka.domain.repository.CobroRepository
import com.app.lucka.domain.repository.NotificacionRepository
import com.app.lucka.domain.repository.PerfilRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime

class PerfilRepositoryImpl(private val dao: PerfilDao) : PerfilRepository {
    override fun observarPerfil(): Flow<Perfil?> = dao.observar().map { it?.toDomain() }

    override suspend fun guardar(perfil: Perfil) = dao.guardar(perfil.toEntity())
}

class CobroRepositoryImpl(private val dao: CobroDao) : CobroRepository {
    override fun observarContactos(): Flow<List<Contacto>> =
        dao.observarContactos().map { lista -> lista.map { it.toDomain() } }

    override suspend fun guardarContacto(contacto: Contacto) = dao.guardarContacto(contacto.toEntity())

    override suspend fun eliminarContacto(id: String) = dao.eliminarContacto(id)

    override fun observarCobros(): Flow<List<Cobro>> = dao.observarCobros().map { lista -> lista.map { it.toDomain() } }

    override suspend fun obtenerCobro(id: String): Cobro? = dao.obtenerCobro(id)?.toDomain()

    override suspend fun guardarCobro(cobro: Cobro) = dao.guardarCobro(cobro.toEntity())

    override suspend fun eliminarCobro(id: String) = dao.eliminarCobro(id)
}

class ActividadRepositoryImpl(private val dao: ActividadDao) : ActividadRepository {
    override fun observar(limite: Int?): Flow<List<Actividad>> =
        (if (limite == null) dao.observarTodas() else dao.observarRecientes(limite))
            .map { lista -> lista.map { it.toDomain() } }

    override suspend fun registrar(descripcion: String, fechaHora: LocalDateTime) =
        dao.insertar(ActividadEntity(descripcion = descripcion, fechaHora = fechaHora.toString()))
}

class NotificacionRepositoryImpl(private val dao: NotificacionDao) : NotificacionRepository {
    override fun observarTodas(): Flow<List<Notificacion>> =
        dao.observarTodas().map { lista -> lista.map { it.toDomain() } }

    override fun observarNoLeidas(): Flow<Int> = dao.observarNoLeidas()

    override suspend fun agregarSiNoExiste(notificacion: Notificacion) =
        dao.insertarSiNoExiste(notificacion.toEntity())

    override suspend fun marcarTodasLeidas() = dao.marcarTodasLeidas()
}
