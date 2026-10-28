package org.mariacf.tempusremedium.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.mariacf.tempusremedium.core.model.AppLanguage
import org.mariacf.tempusremedium.core.model.PillColor
import org.mariacf.tempusremedium.core.model.PillShape
import org.mariacf.tempusremedium.core.model.Settings
import org.mariacf.tempusremedium.core.model.StockDisplay
import org.mariacf.tempusremedium.core.model.ThemeMode
import org.mariacf.tempusremedium.core.model.TimeSlot
import org.mariacf.tempusremedium.ui.components.AppMark
import org.mariacf.tempusremedium.ui.components.BrandButton
import org.mariacf.tempusremedium.ui.components.GroupCard
import org.mariacf.tempusremedium.ui.components.GroupDivider
import org.mariacf.tempusremedium.ui.components.ScreenHeader
import org.mariacf.tempusremedium.ui.components.glass
import org.mariacf.tempusremedium.ui.components.hhmm
import org.mariacf.tempusremedium.ui.components.label
import org.mariacf.tempusremedium.ui.pill.StockIcon
import org.mariacf.tempusremedium.ui.resources.Res
import org.mariacf.tempusremedium.ui.resources.about_author
import org.mariacf.tempusremedium.ui.resources.about_author_role
import org.mariacf.tempusremedium.ui.resources.about_disclaimer
import org.mariacf.tempusremedium.ui.resources.about_site
import org.mariacf.tempusremedium.ui.resources.action_cancel
import org.mariacf.tempusremedium.ui.resources.action_save
import org.mariacf.tempusremedium.ui.resources.app_name
import org.mariacf.tempusremedium.ui.resources.language_de
import org.mariacf.tempusremedium.ui.resources.language_en
import org.mariacf.tempusremedium.ui.resources.language_es
import org.mariacf.tempusremedium.ui.resources.language_fr
import org.mariacf.tempusremedium.ui.resources.language_pt
import org.mariacf.tempusremedium.ui.resources.language_system
import org.mariacf.tempusremedium.ui.resources.nav_settings
import org.mariacf.tempusremedium.ui.resources.permission_granted
import org.mariacf.tempusremedium.ui.resources.permission_missing
import org.mariacf.tempusremedium.ui.resources.settings_about
import org.mariacf.tempusremedium.ui.resources.settings_language
import org.mariacf.tempusremedium.ui.resources.settings_stock_display
import org.mariacf.tempusremedium.ui.resources.settings_theme
import org.mariacf.tempusremedium.ui.resources.settings_times
import org.mariacf.tempusremedium.ui.resources.stock_display_blisters
import org.mariacf.tempusremedium.ui.resources.stock_display_jars
import org.mariacf.tempusremedium.ui.resources.theme_dark
import org.mariacf.tempusremedium.ui.resources.theme_light
import org.mariacf.tempusremedium.ui.resources.theme_system
import org.mariacf.tempusremedium.ui.theme.TempusTheme

/** The author's site, credited in Definições → Sobre. */
const val AUTHOR_SITE_URL = "https://mariacf.pt"

/** Settings shared by every platform; [extras] adds the platform's own sections. */
@Composable
fun SettingsScreen(
    settings: Settings,
    onChange: (Settings) -> Unit,
    modifier: Modifier = Modifier,
    extras: @Composable ColumnScope.() -> Unit = {},
    /** Scroll to the platform extras (reminders and permissions), e.g. from the red bar. */
    focusExtras: Boolean = false,
    onExtrasFocus: () -> Unit = {},
    /** Exportar e importar (see [MedicationTransferSection]), just before Sobre. */
    transfer: @Composable () -> Unit = {},
) {
    var editing by remember { mutableStateOf<TimeSlot?>(null) }
    val extrasRequester = remember { BringIntoViewRequester() }
    val extrasFocus by rememberUpdatedState(onExtrasFocus)
    LaunchedEffect(focusExtras) {
        if (focusExtras) {
            extrasRequester.bringIntoView()
            extrasFocus()
        }
    }
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

        SettingsSection(stringResource(Res.string.settings_language)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppLanguage.entries.forEach { language ->
                    FrequencyChip(stringResource(language.label), selected = settings.language == language) {
                        onChange(settings.copy(language = language))
                    }
                }
            }
        }

        Column(Modifier.bringIntoViewRequester(extrasRequester), verticalArrangement = Arrangement.spacedBy(12.dp)) { extras() }

        transfer()

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
            style = TempusTheme.type.eyebrow,
            color = TempusTheme.colors.inkMute,
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
    /** Something is wrong here (a missing permission): title, body and value in red. */
    danger: Boolean = false,
) {
    val palette = TempusTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = TempusTheme.type.rowTitle, color = if (danger) palette.danger else palette.ink)
            body?.let { Text(it, style = TempusTheme.type.bodySmall, color = if (danger) palette.danger else palette.inkMute) }
        }
        Text(value, style = TempusTheme.type.rowTitle, color = if (danger) palette.danger else palette.accent)
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
            Text(title, style = TempusTheme.type.rowTitle, color = TempusTheme.colors.ink)
            body?.let { Text(it, style = TempusTheme.type.bodySmall, color = TempusTheme.colors.inkMute) }
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
    val palette = TempusTheme.colors
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
            style = TempusTheme.type.rowTitle,
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
    val palette = TempusTheme.colors
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
                style = TempusTheme.type.button,
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
    val palette = TempusTheme.colors
    val uriHandler = LocalUriHandler.current
    GroupCard {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppMark(size = 52.dp)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(stringResource(Res.string.app_name), style = TempusTheme.type.title, color = palette.ink)
                    Text(stringResource(Res.string.about_author), style = TempusTheme.type.bodySmall, color = palette.inkSoft)
                    Text(stringResource(Res.string.about_author_role), style = TempusTheme.type.bodySmall, color = palette.inkMute)
                }
            }
            BrandButton(
                text = stringResource(Res.string.about_site) + "  ↗",
                onClick = { uriHandler.openUri(AUTHOR_SITE_URL) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(Res.string.about_disclaimer), style = TempusTheme.type.bodySmall, color = palette.inkMute)
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
        containerColor = TempusTheme.colors.background,
        text = { TimePicker(state) },
        confirmButton = {
            BrandButton(stringResource(Res.string.action_save), onClick = { onConfirm(LocalTime(state.hour, state.minute)) })
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel), style = TempusTheme.type.button, color = TempusTheme.colors.inkSoft)
            }
        },
    )
}

/** Language names in their own language; [AppLanguage.SYSTEM] is translated. */
private val AppLanguage.label: StringResource
    get() =
        when (this) {
            AppLanguage.SYSTEM -> Res.string.language_system
            AppLanguage.PT -> Res.string.language_pt
            AppLanguage.EN -> Res.string.language_en
            AppLanguage.FR -> Res.string.language_fr
            AppLanguage.DE -> Res.string.language_de
            AppLanguage.ES -> Res.string.language_es
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
