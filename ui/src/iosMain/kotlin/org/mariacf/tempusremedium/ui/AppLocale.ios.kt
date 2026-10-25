// Compose resources need a CompositionLocal here to recompose on a language change (Compose Multiplatform docs).
@file:Suppress("ktlint:compose:compositionlocal-allowlist")

package org.mariacf.tempusremedium.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.preferredLanguages

private const val LANGUAGES_KEY = "AppleLanguages"
private val device = NSLocale.preferredLanguages.firstOrNull() as? String ?: "pt"
private val LocalAppLanguage = staticCompositionLocalOf { device }

@Composable
internal actual fun provideAppLocale(tag: String?): ProvidedValue<*> {
    val defaults = NSUserDefaults.standardUserDefaults
    if (tag == null) defaults.removeObjectForKey(LANGUAGES_KEY) else defaults.setObject(listOf(tag), LANGUAGES_KEY)
    return LocalAppLanguage.provides(tag ?: device)
}
