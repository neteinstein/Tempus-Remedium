package org.mariacf.tempusremedium.android.settings

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import org.jetbrains.compose.resources.stringResource
import org.mariacf.tempusremedium.android.container
import org.mariacf.tempusremedium.core.model.Settings
import org.mariacf.tempusremedium.ui.components.GroupCard
import org.mariacf.tempusremedium.ui.components.GroupDivider
import org.mariacf.tempusremedium.ui.resources.Res
import org.mariacf.tempusremedium.ui.resources.alarm_sound
import org.mariacf.tempusremedium.ui.resources.alarm_sound_default
import org.mariacf.tempusremedium.ui.resources.permission_notifications
import org.mariacf.tempusremedium.ui.resources.settings_alarm_reminders
import org.mariacf.tempusremedium.ui.resources.settings_alarm_reminders_body
import org.mariacf.tempusremedium.ui.resources.settings_exact_alarms
import org.mariacf.tempusremedium.ui.resources.settings_exact_alarms_body
import org.mariacf.tempusremedium.ui.resources.settings_hide_names
import org.mariacf.tempusremedium.ui.resources.settings_permissions
import org.mariacf.tempusremedium.ui.resources.settings_reminders
import org.mariacf.tempusremedium.ui.resources.settings_reminders_body
import org.mariacf.tempusremedium.ui.resources.settings_widgets_hint
import org.mariacf.tempusremedium.ui.screens.PermissionRow
import org.mariacf.tempusremedium.ui.screens.SettingsRow
import org.mariacf.tempusremedium.ui.screens.SettingsSection
import org.mariacf.tempusremedium.ui.screens.SettingsSwitchRow
import org.mariacf.tempusremedium.ui.theme.TempusTheme

/**
 * The permissions reminders depend on, then the reminder settings (Android only). A missing
 * permission is shown in red with the action that grants it; the red bar at the top opens here.
 */
@Composable
fun AndroidSettingsExtras(
    settings: Settings,
    onChange: (Settings) -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestExactAlarms: () -> Unit,
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
    val pickSound =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val picked =
                result.data?.let {
                    IntentCompat.getParcelableExtra(
                        it,
                        RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                        Uri::class.java,
                    )
                }
            if (result.resultCode == Activity.RESULT_OK && picked != null) {
                onChange(settings.copy(alarmSound = picked.takeIf { it != DEFAULT_ALARM_ALERT_URI }?.toString()))
            }
        }
    val pickerTitle = stringResource(Res.string.alarm_sound)
    val defaultSound = stringResource(Res.string.alarm_sound_default)
    val soundName = remember(settings.alarmSound) { settings.alarmSound?.let { soundTitle(context, it) } ?: defaultSound }

    SettingsSection(stringResource(Res.string.settings_permissions), modifier) {
        GroupCard {
            PermissionRow(
                title = stringResource(Res.string.permission_notifications),
                granted = canNotify,
                onGrant = onRequestNotifications,
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                GroupDivider()
                PermissionRow(
                    title = stringResource(Res.string.settings_exact_alarms),
                    granted = canExact,
                    onGrant = onRequestExactAlarms,
                    explanation = stringResource(Res.string.settings_exact_alarms_body),
                )
            }
        }
    }

    SettingsSection(stringResource(Res.string.settings_reminders)) {
        GroupCard {
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_reminders),
                body = stringResource(Res.string.settings_reminders_body),
                checked = settings.remindersEnabled,
                onCheckedChange = { onChange(settings.copy(remindersEnabled = it)) },
            )
            GroupDivider()
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_alarm_reminders),
                body = stringResource(Res.string.settings_alarm_reminders_body),
                checked = settings.alarmReminders,
                onCheckedChange = { onChange(settings.copy(alarmReminders = it)) },
            )
            if (settings.alarmReminders) {
                GroupDivider()
                SettingsRow(
                    title = pickerTitle,
                    value = "›",
                    body = soundName,
                    onClick = {
                        try {
                            pickSound.launch(soundPicker(pickerTitle, settings.alarmSound))
                        } catch (_: ActivityNotFoundException) {
                            // No tone picker on this device: the alarm keeps the default sound.
                        }
                    },
                )
            }
            GroupDivider()
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_hide_names),
                checked = settings.hideNamesInReminders,
                onCheckedChange = { onChange(settings.copy(hideNamesInReminders = it)) },
            )
        }
        Text(
            stringResource(Res.string.settings_widgets_hint),
            style = TempusTheme.type.bodySmall,
            color = TempusTheme.colors.inkMute,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

/** The system tone picker, on alarm tones, with the current choice selected. */
private fun soundPicker(
    title: String,
    current: String?,
): Intent =
    Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
        .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
        .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, title)
        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        .putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, DEFAULT_ALARM_ALERT_URI)
        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
        .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, current?.toUri() ?: DEFAULT_ALARM_ALERT_URI)

private fun soundTitle(
    context: Context,
    uri: String,
): String? = runCatching { RingtoneManager.getRingtone(context, uri.toUri())?.getTitle(context) }.getOrNull()
