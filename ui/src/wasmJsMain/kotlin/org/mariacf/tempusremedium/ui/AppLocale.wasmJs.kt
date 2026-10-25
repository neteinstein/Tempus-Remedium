// Compose resources need a CompositionLocal here to recompose on a language change (Compose Multiplatform docs).
@file:Suppress("ktlint:compose:compositionlocal-allowlist")

package org.mariacf.tempusremedium.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf

/** Read by the navigator.languages override in webApp's index.html. */
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(tag) => { window.__customLocale = tag; }")
private external fun setCustomLocale(tag: String?)

private val LocalAppLanguage = staticCompositionLocalOf<String?> { null }

@Composable
internal actual fun provideAppLocale(tag: String?): ProvidedValue<*> {
    setCustomLocale(tag)
    return LocalAppLanguage.provides(tag)
}
