package io.github.neteinstein.remedioemdia.web

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeViewport
import io.github.neteinstein.remedioemdia.core.data.LocalStorageDocumentStorage
import io.github.neteinstein.remedioemdia.core.data.MedicationRepository
import io.github.neteinstein.remedioemdia.ui.App
import io.github.neteinstein.remedioemdia.ui.PlatformUi
import io.github.neteinstein.remedioemdia.ui.resources.Res
import io.github.neteinstein.remedioemdia.ui.resources.app_name
import io.github.neteinstein.remedioemdia.ui.resources.fraunces_medium
import io.github.neteinstein.remedioemdia.ui.resources.fraunces_medium_italic
import io.github.neteinstein.remedioemdia.ui.resources.fraunces_semibold
import io.github.neteinstein.remedioemdia.ui.resources.fraunces_semibold_italic
import io.github.neteinstein.remedioemdia.ui.resources.jakarta_bold
import io.github.neteinstein.remedioemdia.ui.resources.jakarta_extrabold
import io.github.neteinstein.remedioemdia.ui.resources.jakarta_medium
import io.github.neteinstein.remedioemdia.ui.resources.jakarta_regular
import io.github.neteinstein.remedioemdia.ui.resources.jakarta_semibold
import io.github.neteinstein.remedioemdia.ui.resources.settings_browser_storage
import io.github.neteinstein.remedioemdia.ui.resources.settings_web_reduced
import io.github.neteinstein.remedioemdia.ui.theme.RemedioTheme
import kotlinx.browser.document
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.preloadFont
import org.jetbrains.compose.resources.stringResource

/**
 * The reduced web version: the shared screens with data kept in localStorage.
 * There are no reminders, widgets or lock screen on the web.
 */
private const val SPLASH_SETTLE_MS = 250L
private const val SPLASH_FADE_MS = 320L

/** Every font the theme uses (see RemedioTheme). */
private val webFonts =
    listOf(
        Res.font.jakarta_regular,
        Res.font.jakarta_medium,
        Res.font.jakarta_semibold,
        Res.font.jakarta_bold,
        Res.font.jakarta_extrabold,
        Res.font.fraunces_medium,
        Res.font.fraunces_semibold,
        Res.font.fraunces_medium_italic,
        Res.font.fraunces_semibold_italic,
    )

@OptIn(ExperimentalComposeUiApi::class, ExperimentalResourceApi::class)
fun main() {
    val repository = MedicationRepository(LocalStorageDocumentStorage())
    val platform =
        PlatformUi(
            settingsExtras = { _, _ ->
                listOf(Res.string.settings_browser_storage, Res.string.settings_web_reduced).forEach {
                    Text(
                        stringResource(it),
                        style = RemedioTheme.type.bodySmall,
                        color = RemedioTheme.colors.inkMute,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            },
        )
    // A container of its own: the viewport takes over its element, and the splash must stay visible.
    ComposeViewport(document.getElementById("app")!!) {
        // Fonts load over the network on the web: keep the HTML splash until they are ready,
        // so the first frame already has the right typography.
        val fonts = webFonts.map { preloadFont(it) }
        // Loading one string fetches (and caches) the whole strings file.
        val stringsReady by produceState(false) {
            getString(Res.string.app_name)
            value = true
        }
        if (stringsReady && fonts.all { it.value != null }) {
            LaunchedEffect(Unit) {
                // Let the app draw its first frames before fading the splash out.
                repeat(2) { withFrameNanos { } }
                delay(SPLASH_SETTLE_MS)
                val splash = document.getElementById("splash") ?: return@LaunchedEffect
                splash.classList.add("hide")
                delay(SPLASH_FADE_MS)
                splash.remove()
            }
            App(repository, platform = platform)
        }
    }
}
