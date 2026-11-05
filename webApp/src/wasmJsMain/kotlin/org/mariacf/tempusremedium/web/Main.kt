package org.mariacf.tempusremedium.web

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.preloadFont
import org.jetbrains.compose.resources.stringResource
import org.mariacf.tempusremedium.core.data.LocalStorageDocumentStorage
import org.mariacf.tempusremedium.core.data.MedicationRepository
import org.mariacf.tempusremedium.core.model.TimeSlot
import org.mariacf.tempusremedium.core.schedule.CALENDAR_EXPORT_DAYS
import org.mariacf.tempusremedium.core.schedule.calendarExportStale
import org.mariacf.tempusremedium.ui.App
import org.mariacf.tempusremedium.ui.InstallPrompt
import org.mariacf.tempusremedium.ui.PlatformUi
import org.mariacf.tempusremedium.ui.components.GroupCard
import org.mariacf.tempusremedium.ui.resources.Res
import org.mariacf.tempusremedium.ui.resources.app_name
import org.mariacf.tempusremedium.ui.resources.fraunces_medium
import org.mariacf.tempusremedium.ui.resources.fraunces_medium_italic
import org.mariacf.tempusremedium.ui.resources.fraunces_semibold
import org.mariacf.tempusremedium.ui.resources.fraunces_semibold_italic
import org.mariacf.tempusremedium.ui.resources.jakarta_bold
import org.mariacf.tempusremedium.ui.resources.jakarta_extrabold
import org.mariacf.tempusremedium.ui.resources.jakarta_medium
import org.mariacf.tempusremedium.ui.resources.jakarta_regular
import org.mariacf.tempusremedium.ui.resources.jakarta_semibold
import org.mariacf.tempusremedium.ui.resources.permission_notifications
import org.mariacf.tempusremedium.ui.resources.settings_browser_storage
import org.mariacf.tempusremedium.ui.resources.settings_calendar
import org.mariacf.tempusremedium.ui.resources.settings_calendar_export
import org.mariacf.tempusremedium.ui.resources.settings_calendar_export_body
import org.mariacf.tempusremedium.ui.resources.settings_calendar_stale
import org.mariacf.tempusremedium.ui.resources.settings_reminders
import org.mariacf.tempusremedium.ui.resources.settings_reminders_body
import org.mariacf.tempusremedium.ui.resources.settings_web_alerts
import org.mariacf.tempusremedium.ui.resources.settings_web_alerts_body
import org.mariacf.tempusremedium.ui.resources.settings_web_install_first
import org.mariacf.tempusremedium.ui.resources.settings_web_reduced
import org.mariacf.tempusremedium.ui.screens.LaunchRequest
import org.mariacf.tempusremedium.ui.screens.PermissionRow
import org.mariacf.tempusremedium.ui.screens.SettingsRow
import org.mariacf.tempusremedium.ui.screens.SettingsSection
import org.mariacf.tempusremedium.ui.screens.SettingsSwitchRow
import org.mariacf.tempusremedium.ui.theme.TempusTheme

/**
 * The reduced web version: the shared screens with data kept in localStorage, installable as a
 * web app (manifest and service worker in index.html). It only alerts while open: there are no
 * reminders with the page closed, widgets or lock screen on the web. Instead, it can add the
 * coming reminders to the phone's calendar (see WebCalendarSupport), offers that on iPhone and
 * iPad, and tells when that copy is out of date. A tapped notification opens its slot through the address (launchRequestFromAddress).
 */
private const val SPLASH_SETTLE_MS = 250L
private const val SPLASH_FADE_MS = 320L

/** The native app, which has the reminders and widgets the web lacks. */
private const val STORE_URL = "https://play.google.com/store/apps/details?id=org.mariacf.tempusremedium"

/** In a phone's browser, what to install instead: the home screen web app (iOS) or the native app (Android). */
private fun installPrompt(): InstallPrompt? =
    when {
        isStandalone() -> null
        isIos() -> InstallPrompt.HomeScreen
        isAndroid() -> InstallPrompt.Store { window.open(STORE_URL, "_blank") }
        else -> null
    }

/** The page's theme-color for each theme: the ground of TempusPalette, as in index.html. */
private const val THEME_COLOR_LIGHT = "#f9fafc"
private const val THEME_COLOR_DARK = "#0b1015"

/** What a tapped notification asks to open: ./?slot=MORNING or ./?refill=<id> (see WebReminders). */
private fun launchRequestFromAddress(): LaunchRequest? {
    queryParameter("slot")?.let { name -> TimeSlot.entries.firstOrNull { it.name == name } }?.let { return LaunchRequest.OpenSlot(it) }
    return queryParameter("refill")?.let { LaunchRequest.OpenRefill(it) }
}

@Composable
private fun Note(text: String) {
    Text(
        text,
        style = TempusTheme.type.bodySmall,
        color = TempusTheme.colors.inkMute,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** Every font the theme uses (see TempusTheme). */
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
    // iOS has no way to remind with the app closed but the calendar, so the app offers it.
    val calendar = WebCalendarSupport(repository, offers = isIos())
    val platform =
        PlatformUi(
            settingsExtras = { settings, onChange ->
                SettingsSection(stringResource(Res.string.settings_reminders)) {
                    GroupCard {
                        SettingsSwitchRow(
                            title = stringResource(Res.string.settings_reminders),
                            body = stringResource(Res.string.settings_reminders_body),
                            checked = settings.remindersEnabled,
                            onCheckedChange = { onChange(settings.copy(remindersEnabled = it)) },
                        )
                    }
                }
                SettingsSection(stringResource(Res.string.settings_web_alerts)) {
                    if (WebNotifications.supported) {
                        GroupCard {
                            PermissionRow(
                                title = stringResource(Res.string.permission_notifications),
                                granted = WebNotifications.granted,
                                onGrant = WebNotifications::request,
                            )
                        }
                    }
                    Note(
                        stringResource(
                            if (WebNotifications.supported) Res.string.settings_web_alerts_body else Res.string.settings_web_install_first,
                        ),
                    )
                }
                val data by repository.data.collectAsState()
                SettingsSection(stringResource(Res.string.settings_calendar)) {
                    GroupCard {
                        SettingsRow(
                            title = stringResource(Res.string.settings_calendar_export),
                            body = stringResource(Res.string.settings_calendar_export_body, CALENDAR_EXPORT_DAYS),
                            value = "›",
                            onClick = calendar::add,
                        )
                    }
                    if (data.calendarExportStale(currentTime())) {
                        Text(
                            stringResource(Res.string.settings_calendar_stale),
                            style = TempusTheme.type.bodySmall,
                            color = TempusTheme.colors.danger,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }
                }
                listOf(Res.string.settings_browser_storage, Res.string.settings_web_reduced).forEach { Note(stringResource(it)) }
            },
            onDarkThemeChange = { dark -> setThemeColor(if (dark) THEME_COLOR_DARK else THEME_COLOR_LIGHT) },
            installPrompt = installPrompt(),
            calendar = calendar,
            files = WebFileTransfer,
        )
    // A container of its own: the viewport takes over its element, and the splash must stay visible.
    persistStorage()
    var launchRequest by mutableStateOf(launchRequestFromAddress())
    clearQuery()
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
            LaunchedEffect(Unit) { remindWhileOpen(repository) }
            App(
                repository,
                platform = platform,
                launchRequest = launchRequest,
                onConsumeLaunchRequest = { launchRequest = null },
            )
        }
    }
}
