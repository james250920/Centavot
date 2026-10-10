package com.app.lucka.data.mapper

import com.app.lucka.data.local.RegimenEntity
import com.app.lucka.domain.model.Monto
import com.app.lucka.domain.model.PeriodoTope
import com.app.lucka.domain.model.RegimenTributario
import com.app.lucka.domain.model.TipoRegimen

fun RegimenEntity.toDomain() = RegimenTributario(
    tipo = TipoRegimen.valueOf(tipo),
    tope = Monto(topeCentimos),
    periodo = PeriodoTope.valueOf(periodo),
    nombre = nombre,
)

fun RegimenTributario.toEntity() = RegimenEntity(
    tipo = tipo.name,
    nombre = nombre,
    topeCentimos = tope.centimos,
    periodo = periodo.name,
)
