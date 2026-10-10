package com.app.centavot.data.mapper

import com.app.centavot.data.local.ActividadEntity
import com.app.centavot.data.local.CobroConContacto
import com.app.centavot.data.local.CobroEntity
import com.app.centavot.data.local.ContactoEntity
import com.app.centavot.data.local.NotificacionEntity
import com.app.centavot.data.local.PerfilEntity
import com.app.centavot.domain.model.Actividad
import com.app.centavot.domain.model.Cobro
import com.app.centavot.domain.model.Contacto
import com.app.centavot.domain.model.EstadoCobro
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.Notificacion
import com.app.centavot.domain.model.Perfil
import com.app.centavot.domain.model.Modo
import com.app.centavot.domain.model.Categoria
import com.app.centavot.domain.model.Rubro
import com.app.centavot.domain.model.TasaAhorro
import com.app.centavot.domain.model.TipoCobro
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

fun PerfilEntity.toDomain() = Perfil(
    nombre = nombre,
    rubro = rubro?.let(Rubro::valueOf),
    ingresoMensual = Monto(ingresoMensualCentimos),
    tasaAhorro = TasaAhorro(tasaAhorroDecimas),
    modo = Modo.valueOf(modo),
)

fun Perfil.toEntity() = PerfilEntity(
    nombre = nombre,
    rubro = rubro?.name,
    ingresoMensualCentimos = ingresoMensual.centimos,
    tasaAhorroDecimas = tasaAhorro.decimas,
    modo = modo.name,
)

fun ContactoEntity.toDomain() = Contacto(id = id, nombre = nombre, telefono = telefono)

fun Contacto.toEntity() = ContactoEntity(id = id, nombre = nombre, telefono = telefono)

fun CobroConContacto.toDomain() = Cobro(
    id = cobro.id,
    contacto = contacto.toDomain(),
    motivo = cobro.motivo,
    monto = Monto(cobro.montoCentimos),
    fecha = LocalDate.parse(cobro.fecha),
    estado = EstadoCobro.valueOf(cobro.estado),
    fechaCobrado = cobro.fechaCobrado?.let(LocalDate::parse),
    tipo = TipoCobro.valueOf(cobro.tipo),
    adelanto = Monto(cobro.adelantoCentimos),
    abonado = Monto(cobro.abonadoCentimos),
    categoria = Categoria.valueOf(cobro.categoria),
)

fun Cobro.toEntity() = CobroEntity(
    id = id,
    contactoId = contacto.id,
    motivo = motivo,
    montoCentimos = monto.centimos,
    fecha = fecha.toString(),
    estado = estado.name,
    fechaCobrado = fechaCobrado?.toString(),
    tipo = tipo.name,
    adelantoCentimos = adelanto.centimos,
    abonadoCentimos = abonado.centimos,
    categoria = categoria.name,
)

fun ActividadEntity.toDomain() = Actividad(id = id, descripcion = descripcion, fechaHora = LocalDateTime.parse(fechaHora))

fun NotificacionEntity.toDomain() = Notificacion(
    id = id,
    asunto = asunto,
    mensaje = mensaje,
    fechaHora = LocalDateTime.parse(fechaHora),
    leida = leida,
)

fun Notificacion.toEntity() = NotificacionEntity(
    id = id,
    asunto = asunto,
    mensaje = mensaje,
    fechaHora = fechaHora.toString(),
    leida = leida,
)
