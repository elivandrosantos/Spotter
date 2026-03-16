package br.ordnavile.spotter

import android.app.Application
import br.ordnavile.spotter.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class SpotterApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidLogger()
            androidContext(this@SpotterApplication)
            modules(appModule)
        }
    }
}
