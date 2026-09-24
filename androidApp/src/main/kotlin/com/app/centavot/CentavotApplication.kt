package com.app.centavot

import android.app.Application
import com.app.centavot.di.moduloAndroid
import com.app.centavot.di.modulosComunes
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class CentavotApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@CentavotApplication)
            modules(modulosComunes + moduloAndroid)
        }
    }
}
