package com.app.centavot.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import kotlinx.coroutines.flow.Flow

/**
 * v7: un registro que cambió en el teléfono y falta subir al backend. Lo anotan disparadores de SQLite
 * (ver [DisparadoresSync]), así ningún DAO tiene que acordarse. "marca" crece con cada cambio: al subir se borran
 * solo los pendientes hasta la marca leída, y lo que cambie mientras tanto se sube la próxima vez.
 */
@Entity(tableName = "sync_pendientes", primaryKeys = ["tabla", "id"])
data class CambioPendienteEntity(
    /** Tabla de Room: ingresos, gastos, contactos, cobros, partidas, montos_presupuesto, metas o abonos_meta. */
    val tabla: String,
    /** ID del registro (en montos: "partidaId|mes"). */
    val id: String,
    val borrado: Boolean,
    val marca: Long,
)

/** v7: marcas de la sincronización (última bajada, consentimiento enviado). */
@Entity(tableName = "sync_ajustes")
data class AjusteSyncEntity(
    @PrimaryKey val clave: String,
    val valor: String,
)

/** Tablas que se sincronizan y cómo se arma el ID de cada fila en SQL (NEW u OLD). */
internal val TABLAS_SYNC: Map<String, (String) -> String> = mapOf(
    "partidas" to { f -> "$f.id" },
    "montos_presupuesto" to { f -> "$f.partidaId || '|' || $f.mes" },
    "contactos" to { f -> "$f.id" },
    "ingresos" to { f -> "$f.id" },
    "gastos" to { f -> "$f.id" },
    "cobros" to { f -> "$f.id" },
    "metas" to { f -> "$f.id" },
    "abonos_meta" to { f -> "$f.id" },
)

/** Crea (si faltan) los disparadores que anotan cada cambio local en sync_pendientes. */
object DisparadoresSync : RoomDatabase.Callback() {
    override fun onOpen(connection: SQLiteConnection) {
        for ((tabla, id) in TABLAS_SYNC) {
            fun anotar(fila: String, borrado: Int) =
                "INSERT OR REPLACE INTO sync_pendientes(tabla, id, borrado, marca) VALUES ('$tabla', ${id(fila)}, $borrado, " +
                    "(SELECT IFNULL(MAX(marca), 0) + 1 FROM sync_pendientes))"
            connection.execSQL("CREATE TRIGGER IF NOT EXISTS sync_${tabla}_ins AFTER INSERT ON $tabla BEGIN ${anotar("NEW", 0)}; END")
            connection.execSQL("CREATE TRIGGER IF NOT EXISTS sync_${tabla}_upd AFTER UPDATE ON $tabla BEGIN ${anotar("NEW", 0)}; END")
            connection.execSQL("CREATE TRIGGER IF NOT EXISTS sync_${tabla}_del AFTER DELETE ON $tabla BEGIN ${anotar("OLD", 1)}; END")
        }
    }
}

/** Lo que llegó del servidor, ya convertido a filas de Room. */
data class CambiosRemotos(
    val partidas: List<PartidaEntity> = emptyList(),
    val montos: List<MontoPresupuestoEntity> = emptyList(),
    val contactos: List<ContactoEntity> = emptyList(),
    val ingresos: List<IngresoEntity> = emptyList(),
    val gastos: List<GastoEntity> = emptyList(),
    val cobros: List<CobroEntity> = emptyList(),
    val metas: List<MetaEntity> = emptyList(),
    val abonos: List<AbonoMetaEntity> = emptyList(),
    /** Borrados: tabla de Room → IDs. */
    val borrados: Map<String, List<String>> = emptyMap(),
) {
    val vacio get() = partidas.isEmpty() && montos.isEmpty() && contactos.isEmpty() && ingresos.isEmpty() &&
        gastos.isEmpty() && cobros.isEmpty() && metas.isEmpty() && abonos.isEmpty() && borrados.values.all { it.isEmpty() }
}

@Dao
abstract class SyncDao {
    @Query("SELECT * FROM sync_pendientes ORDER BY marca")
    abstract suspend fun pendientes(): List<CambioPendienteEntity>

    @Query("SELECT COUNT(*) FROM sync_pendientes")
    abstract fun observarPendientes(): Flow<Int>

    @Query("DELETE FROM sync_pendientes WHERE marca <= :hasta")
    abstract suspend fun quitarHasta(hasta: Long)

    @Query("DELETE FROM sync_pendientes WHERE tabla = :tabla AND id = :id")
    abstract suspend fun quitar(tabla: String, id: String)

    @Query("DELETE FROM sync_pendientes")
    abstract suspend fun vaciarPendientes()

    @Query("SELECT valor FROM sync_ajustes WHERE clave = :clave")
    abstract suspend fun ajuste(clave: String): String?

    @Upsert
    abstract suspend fun guardarAjuste(ajuste: AjusteSyncEntity)

    @Query("DELETE FROM sync_ajustes")
    abstract suspend fun borrarAjustes()

    // ---------- Lecturas para armar el lote ----------
    @Query("SELECT * FROM partidas WHERE id IN (:ids)") abstract suspend fun partidas(ids: List<String>): List<PartidaEntity>
    @Query("SELECT * FROM montos_presupuesto WHERE partidaId || '|' || mes IN (:ids)") abstract suspend fun montos(ids: List<String>): List<MontoPresupuestoEntity>
    @Query("SELECT * FROM contactos WHERE id IN (:ids)") abstract suspend fun contactos(ids: List<String>): List<ContactoEntity>
    @Query("SELECT * FROM ingresos WHERE id IN (:ids)") abstract suspend fun ingresos(ids: List<String>): List<IngresoEntity>
    @Query("SELECT * FROM gastos WHERE id IN (:ids)") abstract suspend fun gastos(ids: List<String>): List<GastoEntity>
    @Query("SELECT * FROM cobros WHERE id IN (:ids)") abstract suspend fun cobros(ids: List<String>): List<CobroEntity>
    @Query("SELECT * FROM metas WHERE id IN (:ids)") abstract suspend fun metas(ids: List<String>): List<MetaEntity>
    @Query("SELECT * FROM abonos_meta WHERE id IN (:ids)") abstract suspend fun abonos(ids: List<String>): List<AbonoMetaEntity>

    // ---------- La primera vez se sube todo lo que ya había ----------
    @Query("INSERT OR IGNORE INTO sync_pendientes(tabla, id, borrado, marca) SELECT 'partidas', id, 0, 0 FROM partidas") protected abstract suspend fun encolarPartidas()
    @Query("INSERT OR IGNORE INTO sync_pendientes(tabla, id, borrado, marca) SELECT 'montos_presupuesto', partidaId || '|' || mes, 0, 0 FROM montos_presupuesto") protected abstract suspend fun encolarMontos()
    @Query("INSERT OR IGNORE INTO sync_pendientes(tabla, id, borrado, marca) SELECT 'contactos', id, 0, 0 FROM contactos") protected abstract suspend fun encolarContactos()
    @Query("INSERT OR IGNORE INTO sync_pendientes(tabla, id, borrado, marca) SELECT 'ingresos', id, 0, 0 FROM ingresos") protected abstract suspend fun encolarIngresos()
    @Query("INSERT OR IGNORE INTO sync_pendientes(tabla, id, borrado, marca) SELECT 'gastos', id, 0, 0 FROM gastos") protected abstract suspend fun encolarGastos()
    @Query("INSERT OR IGNORE INTO sync_pendientes(tabla, id, borrado, marca) SELECT 'cobros', id, 0, 0 FROM cobros") protected abstract suspend fun encolarCobros()
    @Query("INSERT OR IGNORE INTO sync_pendientes(tabla, id, borrado, marca) SELECT 'metas', id, 0, 0 FROM metas") protected abstract suspend fun encolarMetas()
    @Query("INSERT OR IGNORE INTO sync_pendientes(tabla, id, borrado, marca) SELECT 'abonos_meta', id, 0, 0 FROM abonos_meta") protected abstract suspend fun encolarAbonos()

    @Transaction
    open suspend fun encolarTodo() {
        encolarPartidas(); encolarMontos(); encolarContactos(); encolarIngresos()
        encolarGastos(); encolarCobros(); encolarMetas(); encolarAbonos()
    }

    // ---------- Aplicar lo que llegó del servidor ----------
    @Upsert protected abstract suspend fun upsertPartidas(l: List<PartidaEntity>)
    @Upsert protected abstract suspend fun upsertMontos(l: List<MontoPresupuestoEntity>)
    @Upsert protected abstract suspend fun upsertContactos(l: List<ContactoEntity>)
    @Upsert protected abstract suspend fun upsertIngresos(l: List<IngresoEntity>)
    @Upsert protected abstract suspend fun upsertGastos(l: List<GastoEntity>)
    @Upsert protected abstract suspend fun upsertCobros(l: List<CobroEntity>)
    @Upsert protected abstract suspend fun upsertMetas(l: List<MetaEntity>)
    @Upsert protected abstract suspend fun upsertAbonos(l: List<AbonoMetaEntity>)

    @Query("DELETE FROM abonos_meta WHERE id = :id") protected abstract suspend fun borrarAbono(id: String)
    @Query("DELETE FROM metas WHERE id = :id") protected abstract suspend fun borrarMeta(id: String)
    @Query("DELETE FROM cobros WHERE id = :id") protected abstract suspend fun borrarCobro(id: String)
    @Query("DELETE FROM gastos WHERE id = :id") protected abstract suspend fun borrarGasto(id: String)
    @Query("DELETE FROM ingresos WHERE id = :id") protected abstract suspend fun borrarIngreso(id: String)
    /** Un contacto con cobros no se borra (la llave foránea es RESTRICT): se queda hasta que se borren sus cobros. */
    @Query("DELETE FROM contactos WHERE id = :id AND NOT EXISTS (SELECT 1 FROM cobros WHERE contactoId = :id)") protected abstract suspend fun borrarContacto(id: String)
    @Query("DELETE FROM partidas WHERE id = :id") protected abstract suspend fun borrarPartida(id: String)

    /**
     * Aplica los cambios del servidor y quita sus pendientes en la misma transacción: los disparadores los anotan,
     * pero no hay que devolverlos al servidor.
     */
    @Transaction
    open suspend fun aplicarRemoto(c: CambiosRemotos) {
        upsertPartidas(c.partidas); upsertMontos(c.montos); upsertContactos(c.contactos); upsertIngresos(c.ingresos)
        upsertGastos(c.gastos); upsertCobros(c.cobros); upsertMetas(c.metas); upsertAbonos(c.abonos)
        val borrar = mapOf<String, suspend (String) -> Unit>(
            "abonos_meta" to ::borrarAbono, "metas" to ::borrarMeta, "cobros" to ::borrarCobro, "gastos" to ::borrarGasto,
            "ingresos" to ::borrarIngreso, "contactos" to ::borrarContacto, "partidas" to ::borrarPartida,
        )
        for ((tabla, fn) in borrar) c.borrados[tabla].orEmpty().forEach { fn(it) }
        c.partidas.forEach { quitar("partidas", it.id) }
        c.montos.forEach { quitar("montos_presupuesto", "${it.partidaId}|${it.mes}") }
        c.contactos.forEach { quitar("contactos", it.id) }
        c.ingresos.forEach { quitar("ingresos", it.id) }
        c.gastos.forEach { quitar("gastos", it.id) }
        c.cobros.forEach { quitar("cobros", it.id) }
        c.metas.forEach { quitar("metas", it.id) }
        c.abonos.forEach { quitar("abonos_meta", it.id) }
        for ((tabla, ids) in c.borrados) ids.forEach { quitar(tabla, it) }
    }
}
