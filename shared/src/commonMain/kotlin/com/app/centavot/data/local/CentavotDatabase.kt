package com.app.centavot.data.local

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    entities = [
        GastoEntity::class,
        RegimenEntity::class,
        PerfilEntity::class,
        ContactoEntity::class,
        CobroEntity::class,
        ActividadEntity::class,
        NotificacionEntity::class,
    ],
    version = 2,
    // v2: subcategoría del gasto, perfil, contactos, cobros, actividad y notificaciones.
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
@ConstructedBy(CentavotDatabaseConstructor::class)
abstract class CentavotDatabase : RoomDatabase() {
    abstract fun gastoDao(): GastoDao
    abstract fun regimenDao(): RegimenDao
    abstract fun perfilDao(): PerfilDao
    abstract fun cobroDao(): CobroDao
    abstract fun actividadDao(): ActividadDao
    abstract fun notificacionDao(): NotificacionDao
}

// Room genera la implementación de cada plataforma.
@Suppress("KotlinNoActualForExpect")
expect object CentavotDatabaseConstructor : RoomDatabaseConstructor<CentavotDatabase> {
    override fun initialize(): CentavotDatabase
}

fun crearDatabase(builder: RoomDatabase.Builder<CentavotDatabase>): CentavotDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
