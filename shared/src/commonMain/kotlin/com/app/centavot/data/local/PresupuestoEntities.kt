package com.app.centavot.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** v5: partidas del presupuesto (lo que el usuario marcó en la plantilla o agregó). */
@Entity(tableName = "partidas")
data class PartidaEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    /** GASTO o INGRESO. */
    val tipo: String,
    /** NEGOCIO o PERSONAL: cada modo tiene su presupuesto. */
    val categoria: String,
    val subcategoria: String?,
    val orden: Int,
    /** v6: FIJO, VARIABLE, PRESCINDIBLE o AHORRO; null = según la subcategoría. */
    val tipoGasto: String? = null,
    /** v6: monto por semana. */
    @ColumnInfo(defaultValue = "0") val semanal: Boolean = false,
    /** v6: pago de una deuda. */
    @ColumnInfo(defaultValue = "0") val deuda: Boolean = false,
)

/** v5: monto de una partida desde un mes ("AAAA-MM"); vale para los siguientes hasta que se cambie. */
@Entity(
    tableName = "montos_presupuesto",
    primaryKeys = ["partidaId", "mes"],
    foreignKeys = [ForeignKey(PartidaEntity::class, ["id"], ["partidaId"], onDelete = ForeignKey.CASCADE)],
)
data class MontoPresupuestoEntity(
    val partidaId: String,
    val mes: String,
    val montoCentimos: Long,
)

/** v5: metas de ahorro. */
@Entity(tableName = "metas")
data class MetaEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val objetivoCentimos: Long,
    val categoria: String,
    val creada: String,
    val fechaLimite: String?,
)

/** v5: plata separada para una meta. */
@Entity(
    tableName = "abonos_meta",
    indices = [Index("metaId")],
    foreignKeys = [ForeignKey(MetaEntity::class, ["id"], ["metaId"], onDelete = ForeignKey.CASCADE)],
)
data class AbonoMetaEntity(
    @PrimaryKey val id: String,
    val metaId: String,
    val montoCentimos: Long,
    val fecha: String,
)

@Dao
interface PresupuestoDao {
    @Query("SELECT * FROM partidas ORDER BY orden, rowid")
    fun observarPartidas(): Flow<List<PartidaEntity>>

    @Query("SELECT * FROM montos_presupuesto")
    fun observarMontos(): Flow<List<MontoPresupuestoEntity>>

    @Upsert
    suspend fun guardarPartida(partida: PartidaEntity)

    @Upsert
    suspend fun guardarMonto(monto: MontoPresupuestoEntity)

    @Query("DELETE FROM montos_presupuesto WHERE partidaId = :id")
    suspend fun eliminarMontos(id: String)

    /** Los movimientos de la partida se quedan, pero sin partida. */
    @Query("DELETE FROM partidas WHERE id = :id")
    suspend fun eliminarPartida(id: String)

    @Query("UPDATE gastos SET partidaId = NULL WHERE partidaId = :id")
    suspend fun soltarGastos(id: String)

    @Query("UPDATE ingresos SET partidaId = NULL WHERE partidaId = :id")
    suspend fun soltarIngresos(id: String)
}

@Dao
interface MetaDao {
    @Query("SELECT * FROM metas ORDER BY creada, rowid")
    fun observarMetas(): Flow<List<MetaEntity>>

    @Query("SELECT * FROM abonos_meta ORDER BY fecha, rowid")
    fun observarAbonos(): Flow<List<AbonoMetaEntity>>

    @Upsert
    suspend fun guardarMeta(meta: MetaEntity)

    @Upsert
    suspend fun guardarAbono(abono: AbonoMetaEntity)

    @Query("DELETE FROM abonos_meta WHERE metaId = :id")
    suspend fun eliminarAbonosDe(id: String)

    @Query("DELETE FROM metas WHERE id = :id")
    suspend fun eliminarMeta(id: String)

    @Query("DELETE FROM abonos_meta WHERE id = :id")
    suspend fun eliminarAbono(id: String)
}
