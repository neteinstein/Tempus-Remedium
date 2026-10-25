package org.mariacf.tempusremedium.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

private var deviceLocale: Locale? = null

@Composable
internal actual fun provideAppLocale(tag: String?): ProvidedValue<*> {
    val configuration = LocalConfiguration.current
    val device = deviceLocale ?: Locale.getDefault().also { deviceLocale = it }
    val locale = tag?.let(Locale::forLanguageTag) ?: device
    // Resources read outside composition (notifications, widgets) use the default locale.
    Locale.setDefault(locale)
    return LocalConfiguration.provides(Configuration(configuration).apply { setLocale(locale) })
}
