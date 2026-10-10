package com.app.lucka.data.local

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
        IngresoEntity::class,
        EventoUsoEntity::class,
    ],
    version = 3,
    // v2: subcategoría del gasto, perfil, contactos, cobros, actividad y notificaciones.
    // v3: ingresos (ventas), eventos de uso, y tipo, adelanto y abonos de los cobros.
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
@ConstructedBy(LuckaDatabaseConstructor::class)
abstract class LuckaDatabase : RoomDatabase() {
    abstract fun gastoDao(): GastoDao
    abstract fun regimenDao(): RegimenDao
    abstract fun perfilDao(): PerfilDao
    abstract fun cobroDao(): CobroDao
    abstract fun actividadDao(): ActividadDao
    abstract fun notificacionDao(): NotificacionDao
    abstract fun ingresoDao(): IngresoDao
    abstract fun eventoUsoDao(): EventoUsoDao
    abstract fun datosDao(): DatosDao
}

// Room genera la implementación de cada plataforma.
@Suppress("KotlinNoActualForExpect")
expect object LuckaDatabaseConstructor : RoomDatabaseConstructor<LuckaDatabase> {
    override fun initialize(): LuckaDatabase
}

fun crearDatabase(builder: RoomDatabase.Builder<LuckaDatabase>): LuckaDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
