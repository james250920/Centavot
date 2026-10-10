package com.app.lucka.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

fun crearDatabaseBuilder(context: Context): RoomDatabase.Builder<LuckaDatabase> {
    val appContext = context.applicationContext
    return Room.databaseBuilder<LuckaDatabase>(
        context = appContext,
        name = appContext.getDatabasePath("lucka.db").absolutePath,
    )
}
