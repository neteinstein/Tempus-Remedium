package org.mariacf.tempusremedium.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.key

/**
 * Overrides the locale Compose resources use, following the pattern in the Compose
 * Multiplatform localization docs: provides [tag] ("fr", "de"...) as the locale, or the
 * device's own when null. Each platform makes it take effect its own way.
 */
@Composable
internal expect fun provideAppLocale(tag: String?): ProvidedValue<*>

/** Runs [content] in the language [tag], or the device's when null; changing it redraws everything. */
@Composable
fun AppLocale(
    tag: String?,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(provideAppLocale(tag)) {
        key(tag) { content() }
    }
}
