package org.mariacf.tempusremedium.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

@Composable
internal actual fun provideAppLocale(tag: String?): ProvidedValue<*> {
    val configuration = LocalConfiguration.current
    // The activity's configuration follows the phone's language, even when it changes while the app runs.
    val device = configuration.locales[0]
    val locale = tag?.let(Locale::forLanguageTag) ?: device
    // Resources read outside composition (notifications, widgets) use the default locale.
    Locale.setDefault(locale)
    return LocalConfiguration.provides(Configuration(configuration).apply { setLocale(locale) })
}
