package io.github.neteinstein.remedioemdia.android.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.neteinstein.remedioemdia.android.container
import io.github.neteinstein.remedioemdia.core.model.Settings
import io.github.neteinstein.remedioemdia.ui.resources.Res
import io.github.neteinstein.remedioemdia.ui.resources.settings_exact_alarms
import io.github.neteinstein.remedioemdia.ui.resources.settings_exact_alarms_body
import io.github.neteinstein.remedioemdia.ui.resources.settings_hide_names
import io.github.neteinstein.remedioemdia.ui.resources.settings_notifications_permission
import io.github.neteinstein.remedioemdia.ui.resources.settings_reminders
import io.github.neteinstein.remedioemdia.ui.resources.settings_reminders_body
import io.github.neteinstein.remedioemdia.ui.resources.settings_widgets_hint
import io.github.neteinstein.remedioemdia.ui.screens.SettingsRow
import io.github.neteinstein.remedioemdia.ui.theme.RemedioColors
import io.github.neteinstein.remedioemdia.ui.theme.RemedioType
import org.jetbrains.compose.resources.stringResource

/** Reminder settings and the permissions they depend on (Android only). */
@Composable
fun AndroidSettingsExtras(
    settings: Settings,
    onChange: (Settings) -> Unit,
    onRequestNotifications: () -> Unit,
    modifier: Modifier = Modifier,
) = Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
    val context = LocalContext.current
    // Permissions can change in system settings; re-read them whenever we come back.
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        resumes++
        onPauseOrDispose { }
    }
    val canNotify = remember(resumes) { context.container.notifications.canPost() }
    val canExact = remember(resumes) { context.container.reminders.canScheduleExact() }

    Text(
        stringResource(Res.string.settings_reminders).uppercase(),
        style = RemedioType.SlotLabel,
        color = RemedioColors.TextSecondary,
        modifier = Modifier.padding(start = 8.dp, top = 16.dp),
    )
    SwitchRow(
        title = stringResource(Res.string.settings_reminders),
        body = stringResource(Res.string.settings_reminders_body),
        checked = settings.remindersEnabled,
        onCheckedChange = { onChange(settings.copy(remindersEnabled = it)) },
    )
    SwitchRow(
        title = stringResource(Res.string.settings_hide_names),
        body = null,
        checked = settings.hideNamesInReminders,
        onCheckedChange = { onChange(settings.copy(hideNamesInReminders = it)) },
    )
    if (!canNotify) {
        SettingsRow(
            title = stringResource(Res.string.settings_notifications_permission),
            value = "›",
            onClick = onRequestNotifications,
        )
    }
    if (!canExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
    Text(
        stringResource(Res.string.settings_widgets_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = RemedioColors.TextSecondary,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
    )
}

@Composable
private fun SwitchRow(
    title: String,
    body: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = RemedioColors.TextPrimary)
            body?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = RemedioColors.TextSecondary) }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
