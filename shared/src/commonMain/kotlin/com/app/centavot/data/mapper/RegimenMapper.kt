package com.app.centavot.data.mapper

import com.app.centavot.data.local.RegimenEntity
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.PeriodoTope
import com.app.centavot.domain.model.RegimenTributario
import com.app.centavot.domain.model.TipoRegimen

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
