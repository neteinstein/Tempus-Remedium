package io.github.neteinstein.remedioemdia.android

import android.app.Application
import android.content.Context

class RemedioApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        container.start()
    }
}

val Context.container: AppContainer
    get() = (applicationContext as RemedioApplication).container
