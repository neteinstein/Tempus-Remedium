package io.github.neteinstein.remedioemdia.web

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeViewport
import io.github.neteinstein.remedioemdia.core.data.LocalStorageDocumentStorage
import io.github.neteinstein.remedioemdia.core.data.MedicationRepository
import io.github.neteinstein.remedioemdia.ui.App
import io.github.neteinstein.remedioemdia.ui.PlatformUi
import io.github.neteinstein.remedioemdia.ui.resources.Res
import io.github.neteinstein.remedioemdia.ui.resources.settings_browser_storage
import io.github.neteinstein.remedioemdia.ui.resources.settings_web_reduced
import io.github.neteinstein.remedioemdia.ui.theme.RemedioColors
import org.jetbrains.compose.resources.stringResource

/**
 * The reduced web version: the shared screens with data kept in localStorage.
 * There are no reminders, widgets or lock screen on the web.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val repository = MedicationRepository(LocalStorageDocumentStorage())
    val platform =
        PlatformUi(
            settingsExtras = { _, _ ->
                listOf(Res.string.settings_browser_storage, Res.string.settings_web_reduced).forEach {
                    Text(
                        stringResource(it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = RemedioColors.TextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            },
        )
    ComposeViewport { App(repository, platform = platform) }
}
