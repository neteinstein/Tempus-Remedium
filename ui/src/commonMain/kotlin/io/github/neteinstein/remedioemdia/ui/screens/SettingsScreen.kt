package io.github.neteinstein.remedioemdia.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.neteinstein.remedioemdia.core.model.Settings
import io.github.neteinstein.remedioemdia.core.model.TimeSlot
import io.github.neteinstein.remedioemdia.ui.components.SectionHeader
import io.github.neteinstein.remedioemdia.ui.components.glass
import io.github.neteinstein.remedioemdia.ui.components.hhmm
import io.github.neteinstein.remedioemdia.ui.components.label
import io.github.neteinstein.remedioemdia.ui.resources.Res
import io.github.neteinstein.remedioemdia.ui.resources.action_cancel
import io.github.neteinstein.remedioemdia.ui.resources.action_save
import io.github.neteinstein.remedioemdia.ui.resources.nav_settings
import io.github.neteinstein.remedioemdia.ui.resources.settings_times
import io.github.neteinstein.remedioemdia.ui.theme.RemedioColors
import io.github.neteinstein.remedioemdia.ui.theme.RemedioType
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.stringResource

/** Settings shared by every platform; [extras] adds the platform's own rows below. */
@Composable
fun SettingsScreen(
    settings: Settings,
    onChange: (Settings) -> Unit,
    modifier: Modifier = Modifier,
    extras: @Composable ColumnScope.() -> Unit = {},
) {
    var editing by remember { mutableStateOf<TimeSlot?>(null) }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionHeader(title = stringResource(Res.string.nav_settings), meta = "")
        Text(
            stringResource(Res.string.settings_times).uppercase(),
            style = RemedioType.SlotLabel,
            color = RemedioColors.TextSecondary,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp),
        )
        TimeSlot.entries.forEach { slot ->
            SettingsRow(
                title = stringResource(slot.label),
                value = settings.timeOf(slot).hhmm(),
                onClick = { editing = slot },
            )
        }
        extras()
    }

    editing?.let { slot ->
        TimeDialog(
            initial = settings.timeOf(slot),
            onDismiss = { editing = null },
            onConfirm = { time ->
                onChange(settings.copy(slotTimes = settings.slotTimes + (slot to time)))
                editing = null
            },
        )
    }
}

@Composable
fun SettingsRow(
    title: String,
    value: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    body: String? = null,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .glass(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = RemedioColors.TextPrimary)
            body?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = RemedioColors.TextSecondary) }
        }
        Text(value, style = MaterialTheme.typography.titleMedium, color = RemedioColors.Amber)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(
    initial: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { TimePicker(state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime(state.hour, state.minute)) }) { Text(stringResource(Res.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    )
}
