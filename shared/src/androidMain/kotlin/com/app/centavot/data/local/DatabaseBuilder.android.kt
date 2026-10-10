package com.app.centavot.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

fun crearDatabaseBuilder(context: Context): RoomDatabase.Builder<CentavotDatabase> {
    val appContext = context.applicationContext
    return Room.databaseBuilder<CentavotDatabase>(
        context = appContext,
        name = appContext.getDatabasePath("centavot.db").absolutePath,
    )
}
