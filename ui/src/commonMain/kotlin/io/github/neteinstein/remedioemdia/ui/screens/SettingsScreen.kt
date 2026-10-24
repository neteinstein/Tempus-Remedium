package io.github.neteinstein.remedioemdia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.neteinstein.remedioemdia.core.model.PillColor
import io.github.neteinstein.remedioemdia.core.model.PillShape
import io.github.neteinstein.remedioemdia.core.model.Settings
import io.github.neteinstein.remedioemdia.core.model.StockDisplay
import io.github.neteinstein.remedioemdia.core.model.ThemeMode
import io.github.neteinstein.remedioemdia.core.model.TimeSlot
import io.github.neteinstein.remedioemdia.ui.components.AppMark
import io.github.neteinstein.remedioemdia.ui.components.BrandButton
import io.github.neteinstein.remedioemdia.ui.components.GroupCard
import io.github.neteinstein.remedioemdia.ui.components.GroupDivider
import io.github.neteinstein.remedioemdia.ui.components.ScreenHeader
import io.github.neteinstein.remedioemdia.ui.components.glass
import io.github.neteinstein.remedioemdia.ui.components.hhmm
import io.github.neteinstein.remedioemdia.ui.components.label
import io.github.neteinstein.remedioemdia.ui.pill.StockIcon
import io.github.neteinstein.remedioemdia.ui.resources.Res
import io.github.neteinstein.remedioemdia.ui.resources.about_author
import io.github.neteinstein.remedioemdia.ui.resources.about_author_role
import io.github.neteinstein.remedioemdia.ui.resources.about_disclaimer
import io.github.neteinstein.remedioemdia.ui.resources.about_site
import io.github.neteinstein.remedioemdia.ui.resources.action_cancel
import io.github.neteinstein.remedioemdia.ui.resources.action_save
import io.github.neteinstein.remedioemdia.ui.resources.app_name
import io.github.neteinstein.remedioemdia.ui.resources.nav_settings
import io.github.neteinstein.remedioemdia.ui.resources.settings_about
import io.github.neteinstein.remedioemdia.ui.resources.settings_stock_display
import io.github.neteinstein.remedioemdia.ui.resources.settings_theme
import io.github.neteinstein.remedioemdia.ui.resources.settings_times
import io.github.neteinstein.remedioemdia.ui.resources.stock_display_blisters
import io.github.neteinstein.remedioemdia.ui.resources.stock_display_jars
import io.github.neteinstein.remedioemdia.ui.resources.theme_dark
import io.github.neteinstein.remedioemdia.ui.resources.theme_light
import io.github.neteinstein.remedioemdia.ui.resources.theme_system
import io.github.neteinstein.remedioemdia.ui.theme.RemedioTheme
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.stringResource

/** The author's site, credited in Definições → Sobre. */
const val AUTHOR_SITE_URL = "https://mariacf.pt"

/** Settings shared by every platform; [extras] adds the platform's own sections. */
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
        ScreenHeader(eyebrow = stringResource(Res.string.app_name), title = stringResource(Res.string.nav_settings))

        SettingsSection(stringResource(Res.string.settings_times)) {
            GroupCard {
                TimeSlot.entries.forEachIndexed { index, slot ->
                    if (index > 0) GroupDivider()
                    SettingsRow(title = stringResource(slot.label), value = settings.timeOf(slot).hhmm(), onClick = { editing = slot })
                }
            }
        }

        SettingsSection(stringResource(Res.string.settings_stock_display)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StockDisplay.entries.forEach { option ->
                    StockDisplayTile(
                        display = option,
                        selected = settings.stockDisplay == option,
                        onClick = { onChange(settings.copy(stockDisplay = option)) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        SettingsSection(stringResource(Res.string.settings_theme)) {
            SegmentedChoice(
                options = ThemeMode.entries,
                selected = settings.theme,
                label = {
                    when (it) {
                        ThemeMode.SYSTEM -> stringResource(Res.string.theme_system)
                        ThemeMode.LIGHT -> stringResource(Res.string.theme_light)
                        ThemeMode.DARK -> stringResource(Res.string.theme_dark)
                    }
                },
                onSelect = { onChange(settings.copy(theme = it)) },
            )
        }

        extras()

        SettingsSection(stringResource(Res.string.settings_about)) { AboutCard() }
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

/** A small uppercase title above a group of settings. */
@Composable
fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            title.uppercase(),
            style = RemedioTheme.type.eyebrow,
            color = RemedioTheme.colors.inkMute,
            modifier = Modifier.padding(start = 8.dp),
        )
        content()
    }
}

/** A row of a [GroupCard]: title (and optional body) on the left, value on the right. */
@Composable
fun SettingsRow(
    title: String,
    value: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    body: String? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = RemedioTheme.type.rowTitle, color = RemedioTheme.colors.ink)
            body?.let { Text(it, style = RemedioTheme.type.bodySmall, color = RemedioTheme.colors.inkMute) }
        }
        Text(value, style = RemedioTheme.type.rowTitle, color = RemedioTheme.colors.accent)
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    body: String? = null,
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = RemedioTheme.type.rowTitle, color = RemedioTheme.colors.ink)
            body?.let { Text(it, style = RemedioTheme.type.bodySmall, color = RemedioTheme.colors.inkMute) }
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun StockDisplayTile(
    display: StockDisplay,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = RemedioTheme.colors
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier
            .glass(shape, palette, strong = selected, elevation = if (selected) 8.dp else 0.dp)
            .border(if (selected) 2.dp else 0.dp, if (selected) palette.accent else Color.Transparent, shape)
            .clip(shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StockIcon(PillShape.ROUND_SCORED, PillColor.TEAL, count = 6, height = 84.dp, display = display)
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(if (display == StockDisplay.JARS) Res.string.stock_display_jars else Res.string.stock_display_blisters),
            style = RemedioTheme.type.rowTitle,
            color = if (selected) palette.accent else palette.ink,
        )
    }
}

@Composable
private fun <T> SegmentedChoice(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = RemedioTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .glass(CircleShape, palette)
            .padding(4.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Text(
                label(option),
                style = RemedioTheme.type.button,
                color = if (isSelected) Color.White else palette.inkSoft,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .then(if (isSelected) Modifier.background(palette.brandBrush) else Modifier)
                        .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(option) })
                        .padding(vertical = 10.dp),
            )
        }
    }
}

/** Credits the author, Dra. Maria Cortês Ferreira, and links to her site. */
@Composable
private fun AboutCard() {
    val palette = RemedioTheme.colors
    val uriHandler = LocalUriHandler.current
    GroupCard {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppMark(size = 52.dp)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(stringResource(Res.string.app_name), style = RemedioTheme.type.title, color = palette.ink)
                    Text(stringResource(Res.string.about_author), style = RemedioTheme.type.bodySmall, color = palette.inkSoft)
                    Text(stringResource(Res.string.about_author_role), style = RemedioTheme.type.bodySmall, color = palette.inkMute)
                }
            }
            BrandButton(
                text = stringResource(Res.string.about_site) + "  ↗",
                onClick = { uriHandler.openUri(AUTHOR_SITE_URL) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(Res.string.about_disclaimer), style = RemedioTheme.type.bodySmall, color = palette.inkMute)
        }
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
        containerColor = RemedioTheme.colors.background,
        text = { TimePicker(state) },
        confirmButton = {
            BrandButton(stringResource(Res.string.action_save), onClick = { onConfirm(LocalTime(state.hour, state.minute)) })
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel), style = RemedioTheme.type.button, color = RemedioTheme.colors.inkSoft)
            }
        },
    )
}
