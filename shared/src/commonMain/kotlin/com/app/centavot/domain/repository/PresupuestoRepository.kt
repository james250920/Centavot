package com.app.centavot.domain.repository

import com.app.centavot.domain.model.AbonoMeta
import com.app.centavot.domain.model.Meta
import com.app.centavot.domain.model.MontoPresupuesto
import com.app.centavot.domain.model.Partida
import kotlinx.coroutines.flow.Flow

interface PresupuestoRepository {
    /** De los dos modos, en orden. */
    fun observarPartidas(): Flow<List<Partida>>

    fun observarMontos(): Flow<List<MontoPresupuesto>>

    suspend fun guardarPartida(partida: Partida)

    suspend fun fijarMonto(monto: MontoPresupuesto)

    /** Borra la partida y sus montos; sus gastos e ingresos se quedan, sin partida. */
    suspend fun eliminarPartida(id: String)
}

interface MetaRepository {
    fun observarMetas(): Flow<List<Meta>>

    fun observarAbonos(): Flow<List<AbonoMeta>>

    suspend fun guardarMeta(meta: Meta)

    suspend fun agregarAbono(abono: AbonoMeta)

    suspend fun eliminarAbono(id: String)

    /** Borra la meta y sus abonos. */
    suspend fun eliminarMeta(id: String)
}
