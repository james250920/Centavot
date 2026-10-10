package com.app.centavot.data.mapper

import com.app.centavot.data.local.EventoUsoEntity
import com.app.centavot.data.local.IngresoEntity
import com.app.centavot.domain.model.Categoria
import com.app.centavot.domain.model.EventoUso
import com.app.centavot.domain.model.Ingreso
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.TipoEventoUso
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

fun IngresoEntity.toDomain() = Ingreso(
    id = id,
    monto = Monto(montoCentimos),
    fecha = LocalDate.parse(fecha),
    categoria = Categoria.valueOf(categoria),
    descripcion = descripcion,
    retiroDelNegocio = retiroDelNegocio,
)

fun Ingreso.toEntity() = IngresoEntity(
    id = id,
    montoCentimos = monto.centimos,
    fecha = fecha.toString(),
    categoria = categoria.name,
    descripcion = descripcion,
    retiroDelNegocio = retiroDelNegocio,
)

fun EventoUsoEntity.toDomain() = EventoUso(
    tipo = TipoEventoUso.valueOf(tipo),
    fechaHora = LocalDateTime.parse(fechaHora),
    valor = valor,
)

fun EventoUso.toEntity() = EventoUsoEntity(tipo = tipo.name, fechaHora = fechaHora.toString(), valor = valor)
