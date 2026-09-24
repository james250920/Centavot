package com.app.centavot.data.mapper

import com.app.centavot.data.local.GastoEntity
import com.app.centavot.domain.model.Categoria
import com.app.centavot.domain.model.EstadoGasto
import com.app.centavot.domain.model.Gasto
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.OrigenGasto
import kotlinx.datetime.LocalDate

fun GastoEntity.toDomain() = Gasto(
    id = id,
    monto = Monto(montoCentimos),
    fecha = LocalDate.parse(fecha),
    origen = OrigenGasto.valueOf(origen),
    estado = EstadoGasto.valueOf(estado),
    categoria = categoria?.let(Categoria::valueOf),
    proveedor = proveedor,
    descripcion = descripcion,
    corregidoManualmente = corregidoManualmente,
)

fun Gasto.toEntity() = GastoEntity(
    id = id,
    montoCentimos = monto.centimos,
    fecha = fecha.toString(),
    origen = origen.name,
    estado = estado.name,
    categoria = categoria?.name,
    proveedor = proveedor,
    descripcion = descripcion,
    corregidoManualmente = corregidoManualmente,
)
