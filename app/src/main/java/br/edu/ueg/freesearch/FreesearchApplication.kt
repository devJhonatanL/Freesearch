package br.edu.ueg.freesearch

import android.app.Application
import br.edu.ueg.freesearch.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class FreesearchApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin { androidContext(this@FreesearchApplication); modules(appModule) }
    }
}
