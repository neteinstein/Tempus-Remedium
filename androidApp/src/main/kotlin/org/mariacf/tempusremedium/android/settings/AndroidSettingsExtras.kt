package org.mariacf.tempusremedium.android.settings

import android.os.Build
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import org.jetbrains.compose.resources.stringResource
import org.mariacf.tempusremedium.android.container
import org.mariacf.tempusremedium.core.model.Settings
import org.mariacf.tempusremedium.ui.components.GroupCard
import org.mariacf.tempusremedium.ui.components.GroupDivider
import org.mariacf.tempusremedium.ui.resources.Res
import org.mariacf.tempusremedium.ui.resources.permission_granted
import org.mariacf.tempusremedium.ui.resources.permission_missing
import org.mariacf.tempusremedium.ui.resources.permission_notifications
import org.mariacf.tempusremedium.ui.resources.settings_exact_alarms
import org.mariacf.tempusremedium.ui.resources.settings_exact_alarms_body
import org.mariacf.tempusremedium.ui.resources.settings_hide_names
import org.mariacf.tempusremedium.ui.resources.settings_permissions
import org.mariacf.tempusremedium.ui.resources.settings_reminders
import org.mariacf.tempusremedium.ui.resources.settings_reminders_body
import org.mariacf.tempusremedium.ui.resources.settings_widgets_hint
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

/** "Notificações · Dada" or, in red, "Notificações · Em falta · toque para dar ›". */
@Composable
fun PermissionRow(
    title: String,
    granted: Boolean,
    onGrant: () -> Unit,
    modifier: Modifier = Modifier,
    explanation: String? = null,
) {
    val status = stringResource(if (granted) Res.string.permission_granted else Res.string.permission_missing)
    SettingsRow(
        title = title,
        body = if (granted || explanation == null) status else "$status\n$explanation",
        value = if (granted) "✓" else "›",
        onClick = if (granted) null else onGrant,
        danger = !granted,
        modifier = modifier,
    )
}
