package com.app.centavot.data.repository

import com.app.centavot.data.local.AbonoMetaEntity
import com.app.centavot.data.local.MetaDao
import com.app.centavot.data.local.MetaEntity
import com.app.centavot.data.local.MontoPresupuestoEntity
import com.app.centavot.data.local.PartidaEntity
import com.app.centavot.data.local.PresupuestoDao
import com.app.centavot.domain.model.AbonoMeta
import com.app.centavot.domain.model.Categoria
import com.app.centavot.domain.model.Meta
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.MontoPresupuesto
import com.app.centavot.domain.model.Partida
import com.app.centavot.domain.model.SubcategoriaGasto
import com.app.centavot.domain.model.TipoPartida
import com.app.centavot.domain.repository.MetaRepository
import com.app.centavot.domain.repository.PresupuestoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

class PresupuestoRepositoryImpl(private val dao: PresupuestoDao) : PresupuestoRepository {
    override fun observarPartidas(): Flow<List<Partida>> = dao.observarPartidas().map { lista ->
        lista.map {
            Partida(
                id = it.id,
                nombre = it.nombre,
                tipo = TipoPartida.valueOf(it.tipo),
                categoria = Categoria.valueOf(it.categoria),
                subcategoria = it.subcategoria?.let(SubcategoriaGasto::valueOf),
                orden = it.orden,
            )
        }
    }

    override fun observarMontos(): Flow<List<MontoPresupuesto>> = dao.observarMontos().map { lista ->
        lista.map { MontoPresupuesto(it.partidaId, YearMonth.parse(it.mes), Monto(it.montoCentimos)) }
    }

    override suspend fun guardarPartida(partida: Partida) = dao.guardarPartida(
        PartidaEntity(partida.id, partida.nombre, partida.tipo.name, partida.categoria.name, partida.subcategoria?.name, partida.orden),
    )

    override suspend fun fijarMonto(monto: MontoPresupuesto) =
        dao.guardarMonto(MontoPresupuestoEntity(monto.partidaId, monto.mes.toString(), monto.monto.centimos))

    override suspend fun eliminarPartida(id: String) {
        dao.soltarGastos(id)
        dao.soltarIngresos(id)
        dao.eliminarMontos(id)
        dao.eliminarPartida(id)
    }
}

class MetaRepositoryImpl(private val dao: MetaDao) : MetaRepository {
    override fun observarMetas(): Flow<List<Meta>> = dao.observarMetas().map { lista ->
        lista.map {
            Meta(
                id = it.id,
                nombre = it.nombre,
                objetivo = Monto(it.objetivoCentimos),
                categoria = Categoria.valueOf(it.categoria),
                creada = LocalDate.parse(it.creada),
                fechaLimite = it.fechaLimite?.let(LocalDate::parse),
            )
        }
    }

    override fun observarAbonos(): Flow<List<AbonoMeta>> = dao.observarAbonos().map { lista ->
        lista.map { AbonoMeta(it.id, it.metaId, Monto(it.montoCentimos), LocalDate.parse(it.fecha)) }
    }

    override suspend fun guardarMeta(meta: Meta) = dao.guardarMeta(
        MetaEntity(meta.id, meta.nombre, meta.objetivo.centimos, meta.categoria.name, meta.creada.toString(), meta.fechaLimite?.toString()),
    )

    override suspend fun agregarAbono(abono: AbonoMeta) =
        dao.guardarAbono(AbonoMetaEntity(abono.id, abono.metaId, abono.monto.centimos, abono.fecha.toString()))

    override suspend fun eliminarAbono(id: String) = dao.eliminarAbono(id)

    override suspend fun eliminarMeta(id: String) {
        dao.eliminarAbonosDe(id)
        dao.eliminarMeta(id)
    }
}
