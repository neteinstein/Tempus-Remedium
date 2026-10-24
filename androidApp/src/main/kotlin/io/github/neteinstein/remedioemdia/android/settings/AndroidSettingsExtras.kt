package io.github.neteinstein.remedioemdia.android.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.neteinstein.remedioemdia.android.container
import io.github.neteinstein.remedioemdia.core.model.Settings
import io.github.neteinstein.remedioemdia.ui.components.GroupCard
import io.github.neteinstein.remedioemdia.ui.components.GroupDivider
import io.github.neteinstein.remedioemdia.ui.resources.Res
import io.github.neteinstein.remedioemdia.ui.resources.settings_exact_alarms
import io.github.neteinstein.remedioemdia.ui.resources.settings_exact_alarms_body
import io.github.neteinstein.remedioemdia.ui.resources.settings_hide_names
import io.github.neteinstein.remedioemdia.ui.resources.settings_notifications_permission
import io.github.neteinstein.remedioemdia.ui.resources.settings_reminders
import io.github.neteinstein.remedioemdia.ui.resources.settings_reminders_body
import io.github.neteinstein.remedioemdia.ui.resources.settings_widgets_hint
import io.github.neteinstein.remedioemdia.ui.screens.SettingsRow
import io.github.neteinstein.remedioemdia.ui.screens.SettingsSection
import io.github.neteinstein.remedioemdia.ui.screens.SettingsSwitchRow
import io.github.neteinstein.remedioemdia.ui.theme.RemedioTheme
import org.jetbrains.compose.resources.stringResource

/** Reminder settings and the permissions they depend on (Android only). */
@Composable
fun AndroidSettingsExtras(
    settings: Settings,
    onChange: (Settings) -> Unit,
    onRequestNotifications: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // Permissions can change in system settings; re-read them whenever we come back.
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        resumes++
        onPauseOrDispose { }
    }
    val canNotify = remember(resumes) { context.container.notifications.canPost() }
    val canExact = remember(resumes) { context.container.reminders.canScheduleExact() }

    SettingsSection(stringResource(Res.string.settings_reminders), modifier) {
        GroupCard {
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_reminders),
                body = stringResource(Res.string.settings_reminders_body),
                checked = settings.remindersEnabled,
                onCheckedChange = { onChange(settings.copy(remindersEnabled = it)) },
            )
            GroupDivider()
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_hide_names),
                checked = settings.hideNamesInReminders,
                onCheckedChange = { onChange(settings.copy(hideNamesInReminders = it)) },
            )
            if (!canNotify) {
                GroupDivider()
                SettingsRow(
                    title = stringResource(Res.string.settings_notifications_permission),
                    value = "›",
                    onClick = onRequestNotifications,
                )
            }
            if (!canExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                GroupDivider()
                SettingsRow(
                    title = stringResource(Res.string.settings_exact_alarms),
                    body = stringResource(Res.string.settings_exact_alarms_body),
                    value = "›",
                    onClick = {
                        context.startActivity(
                            Intent(ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri())
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    },
                )
            }
        }
        Text(
            stringResource(Res.string.settings_widgets_hint),
            style = RemedioTheme.type.bodySmall,
            color = RemedioTheme.colors.inkMute,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}
