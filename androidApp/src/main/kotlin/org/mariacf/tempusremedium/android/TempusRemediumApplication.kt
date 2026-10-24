package org.mariacf.tempusremedium.android

import android.app.Application
import android.content.Context

class TempusRemediumApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        container.start()
    }
}

val Context.container: AppContainer
    get() = (applicationContext as TempusRemediumApplication).container
