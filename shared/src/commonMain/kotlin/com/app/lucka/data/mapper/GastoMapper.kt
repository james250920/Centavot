package com.app.lucka.data.mapper

import com.app.lucka.data.local.GastoEntity
import com.app.lucka.domain.model.Categoria
import com.app.lucka.domain.model.EstadoGasto
import com.app.lucka.domain.model.Gasto
import com.app.lucka.domain.model.Monto
import com.app.lucka.domain.model.OrigenGasto
import com.app.lucka.domain.model.SubcategoriaGasto
import kotlinx.datetime.LocalDate

fun GastoEntity.toDomain() = Gasto(
    id = id,
    monto = Monto(montoCentimos),
    fecha = LocalDate.parse(fecha),
    origen = OrigenGasto.valueOf(origen),
    estado = EstadoGasto.valueOf(estado),
    categoria = categoria?.let(Categoria::valueOf),
    subcategoria = subcategoria?.let(SubcategoriaGasto::valueOf),
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
    subcategoria = subcategoria?.name,
    proveedor = proveedor,
    descripcion = descripcion,
    corregidoManualmente = corregidoManualmente,
)
