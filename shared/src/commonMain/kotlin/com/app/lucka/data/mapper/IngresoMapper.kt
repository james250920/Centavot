package com.app.lucka.data.mapper

import com.app.lucka.data.local.EventoUsoEntity
import com.app.lucka.data.local.IngresoEntity
import com.app.lucka.domain.model.Categoria
import com.app.lucka.domain.model.EventoUso
import com.app.lucka.domain.model.Ingreso
import com.app.lucka.domain.model.Monto
import com.app.lucka.domain.model.TipoEventoUso
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

fun IngresoEntity.toDomain() = Ingreso(
    id = id,
    monto = Monto(montoCentimos),
    fecha = LocalDate.parse(fecha),
    categoria = Categoria.valueOf(categoria),
    descripcion = descripcion,
)

fun Ingreso.toEntity() = IngresoEntity(
    id = id,
    montoCentimos = monto.centimos,
    fecha = fecha.toString(),
    categoria = categoria.name,
    descripcion = descripcion,
)

fun EventoUsoEntity.toDomain() = EventoUso(
    tipo = TipoEventoUso.valueOf(tipo),
    fechaHora = LocalDateTime.parse(fechaHora),
    valor = valor,
)

fun EventoUso.toEntity() = EventoUsoEntity(tipo = tipo.name, fechaHora = fechaHora.toString(), valor = valor)
