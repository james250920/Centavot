package com.app.centavot

import android.app.Application
import com.app.centavot.di.moduloAndroid
import com.app.centavot.di.modulosComunes
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module
import com.app.centavot.core.util.Sesion

class CentavotApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@CentavotApplication)
            // Va al final: reemplaza la SesionLocal de los módulos comunes por la de Firebase.
            modules(modulosComunes + moduloAndroid + module { single<Sesion> { SesionFirebase(this@CentavotApplication) } })
        }
    }
}
