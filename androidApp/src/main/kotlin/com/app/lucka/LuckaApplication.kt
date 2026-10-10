package com.app.lucka

import android.app.Application
import com.app.lucka.di.moduloAndroid
import com.app.lucka.di.modulosComunes
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class LuckaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@LuckaApplication)
            modules(modulosComunes + moduloAndroid)
        }
    }
}
