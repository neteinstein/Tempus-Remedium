package io.github.neteinstein.remedioemdia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.neteinstein.remedioemdia.core.model.Frequency
import io.github.neteinstein.remedioemdia.core.model.Medication
import io.github.neteinstein.remedioemdia.core.model.PillColor
import io.github.neteinstein.remedioemdia.core.model.PillShape
import io.github.neteinstein.remedioemdia.core.model.Schedule
import io.github.neteinstein.remedioemdia.core.model.Stock
import io.github.neteinstein.remedioemdia.core.model.TimeSlot
import io.github.neteinstein.remedioemdia.core.model.newMedicationId
import io.github.neteinstein.remedioemdia.ui.PlatformBackHandler
import io.github.neteinstein.remedioemdia.ui.components.BrandButton
import io.github.neteinstein.remedioemdia.ui.components.brandBackground
import io.github.neteinstein.remedioemdia.ui.components.glass
import io.github.neteinstein.remedioemdia.ui.components.label
import io.github.neteinstein.remedioemdia.ui.components.shortLabel
import io.github.neteinstein.remedioemdia.ui.pill.PillIcon
import io.github.neteinstein.remedioemdia.ui.pill.PillInk
import io.github.neteinstein.remedioemdia.ui.pill.edge
import io.github.neteinstein.remedioemdia.ui.pill.fill
import io.github.neteinstein.remedioemdia.ui.resources.Res
import io.github.neteinstein.remedioemdia.ui.resources.action_back
import io.github.neteinstein.remedioemdia.ui.resources.action_cancel
import io.github.neteinstein.remedioemdia.ui.resources.action_delete
import io.github.neteinstein.remedioemdia.ui.resources.action_save
import io.github.neteinstein.remedioemdia.ui.resources.editor_delete_confirm
import io.github.neteinstein.remedioemdia.ui.resources.editor_edit
import io.github.neteinstein.remedioemdia.ui.resources.editor_new
import io.github.neteinstein.remedioemdia.ui.resources.field_color
import io.github.neteinstein.remedioemdia.ui.resources.field_frequency
import io.github.neteinstein.remedioemdia.ui.resources.field_low_threshold
import io.github.neteinstein.remedioemdia.ui.resources.field_name
import io.github.neteinstein.remedioemdia.ui.resources.field_notes
import io.github.neteinstein.remedioemdia.ui.resources.field_shape
import io.github.neteinstein.remedioemdia.ui.resources.field_strength
import io.github.neteinstein.remedioemdia.ui.resources.field_track_stock
import io.github.neteinstein.remedioemdia.ui.resources.field_units_left
import io.github.neteinstein.remedioemdia.ui.resources.field_when
import io.github.neteinstein.remedioemdia.ui.resources.frequency_daily
import io.github.neteinstein.remedioemdia.ui.resources.frequency_every_n_days
import io.github.neteinstein.remedioemdia.ui.resources.frequency_weekdays
import io.github.neteinstein.remedioemdia.ui.theme.RemedioTheme
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

private enum class FrequencyKind { DAILY, WEEKDAYS, EVERY_N_DAYS }

private val FieldShape = RoundedCornerShape(14.dp)

/** Full-screen form for a new ([medication] == null) or existing medication. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MedicationEditor(
    medication: Medication?,
    today: LocalDate,
    onSave: (Medication) -> Unit,
    onDelete: (Medication) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlatformBackHandler(enabled = true, onBack = onClose)

    var name by remember { mutableStateOf(medication?.name.orEmpty()) }
    var strength by remember { mutableStateOf(medication?.strength.orEmpty()) }
    var shape by remember { mutableStateOf(medication?.shape ?: PillShape.ROUND_SCORED) }
    var color by remember { mutableStateOf(medication?.color ?: PillColor.TEAL) }
    var doses by remember { mutableStateOf(medication?.schedule?.doses ?: mapOf(TimeSlot.MORNING to 1)) }
    val initialFrequency = medication?.schedule?.frequency ?: Frequency.Daily
    var frequencyKind by remember {
        mutableStateOf(
            when (initialFrequency) {
                Frequency.Daily -> FrequencyKind.DAILY
                is Frequency.Weekdays -> FrequencyKind.WEEKDAYS
                is Frequency.EveryNDays -> FrequencyKind.EVERY_N_DAYS
            },
        )
    }
    var weekdays by remember { mutableStateOf((initialFrequency as? Frequency.Weekdays)?.days ?: DayOfWeek.entries.toSet()) }
    var interval by remember { mutableStateOf((initialFrequency as? Frequency.EveryNDays)?.interval ?: 2) }
    var trackStock by remember { mutableStateOf(medication?.stock != null) }
    var remaining by remember { mutableStateOf(medication?.stock?.remaining?.toString() ?: "30") }
    var threshold by remember {
        mutableStateOf(
            medication
                ?.stock
                ?.lowThreshold
                ?.toString()
                .orEmpty(),
        )
    }
    var notes by remember { mutableStateOf(medication?.notes.orEmpty()) }
    var confirmDelete by remember { mutableStateOf(false) }

    val canSave =
        name.isNotBlank() &&
            doses.values.any { it > 0 } &&
            (frequencyKind != FrequencyKind.WEEKDAYS || weekdays.isNotEmpty()) &&
            (!trackStock || remaining.toIntOrNull() != null)

    fun build(): Medication =
        Medication(
            id = medication?.id ?: newMedicationId(),
            name = name.trim(),
            strength = strength.trim(),
            shape = shape,
            color = color,
            schedule =
                Schedule(
                    doses = doses.filterValues { it > 0 },
                    frequency =
                        when (frequencyKind) {
                            FrequencyKind.DAILY -> Frequency.Daily
                            FrequencyKind.WEEKDAYS -> Frequency.Weekdays(weekdays)
                            FrequencyKind.EVERY_N_DAYS -> Frequency.EveryNDays(interval)
                        },
                    startDate = medication?.schedule?.startDate ?: today,
                    endDate = medication?.schedule?.endDate,
                ),
            stock =
                if (trackStock) {
                    Stock(remaining = remaining.toIntOrNull() ?: 0, lowThreshold = threshold.toIntOrNull())
                } else {
                    null
                },
            notes = notes.trim(),
            archived = medication?.archived ?: false,
        )

    Column(modifier.fillMaxSize().brandBackground(RemedioTheme.colors)) {
        TopAppBar(
            title = {
                Text(
                    stringResource(if (medication == null) Res.string.editor_new else Res.string.editor_edit),
                    style = RemedioTheme.type.title.copy(fontSize = 21.sp),
                    color = RemedioTheme.colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.action_back))
                }
            },
            actions = {
                BrandButton(
                    stringResource(Res.string.action_save),
                    onClick = { onSave(build()) },
                    enabled = canSave,
                    modifier = Modifier.padding(end = 12.dp),
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(Res.string.field_name)) },
                singleLine = true,
                shape = FieldShape,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = strength,
                onValueChange = { strength = it },
                label = { Text(stringResource(Res.string.field_strength)) },
                singleLine = true,
                shape = FieldShape,
                modifier = Modifier.fillMaxWidth(),
            )

            FieldTitle(stringResource(Res.string.field_shape))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillShape.entries.forEach { option ->
                    val description = stringResource(option.label)
                    SelectableTile(
                        selected = option == shape,
                        onClick = { shape = option },
                        description = description,
                        modifier = Modifier.weight(1f),
                    ) {
                        PillIcon(option, color, height = 28.dp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            description,
                            style = RemedioTheme.type.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 14.sp),
                            color = RemedioTheme.colors.inkSoft,
                            minLines = 2,
                            maxLines = 2,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            FieldTitle(stringResource(Res.string.field_color))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PillColor.entries.forEach { option ->
                    val selected = option == color
                    val description = stringResource(option.label)
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(option.fill)
                            .border(
                                if (selected) 3.dp else 1.dp,
                                if (selected) RemedioTheme.colors.accent else option.edge.copy(alpha = 0.5f),
                                CircleShape,
                            ).clickable { color = option }
                            .semantics {
                                contentDescription = description
                                this.selected = selected
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = PillInk)
                    }
                }
            }

            FieldTitle(stringResource(Res.string.field_when))
            TimeSlot.entries.forEach { slot ->
                QuantityRow(
                    label = stringResource(slot.label),
                    quantity = doses[slot] ?: 0,
                    onChange = { doses = doses + (slot to it) },
                )
            }

            FieldTitle(stringResource(Res.string.field_frequency))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FrequencyChip(stringResource(Res.string.frequency_daily), frequencyKind == FrequencyKind.DAILY) {
                    frequencyKind = FrequencyKind.DAILY
                }
                FrequencyChip(stringResource(Res.string.frequency_weekdays), frequencyKind == FrequencyKind.WEEKDAYS) {
                    frequencyKind = FrequencyKind.WEEKDAYS
                }
                FrequencyChip(
                    stringResource(Res.string.frequency_every_n_days, interval),
                    frequencyKind == FrequencyKind.EVERY_N_DAYS,
                ) { frequencyKind = FrequencyKind.EVERY_N_DAYS }
            }
            when (frequencyKind) {
                FrequencyKind.DAILY -> {}

                FrequencyKind.WEEKDAYS -> {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DayOfWeek.entries.forEach { day ->
                            FrequencyChip(stringResource(day.shortLabel), day in weekdays) {
                                weekdays = if (day in weekdays) weekdays - day else weekdays + day
                            }
                        }
                    }
                }

                FrequencyKind.EVERY_N_DAYS -> {
                    QuantityRow(
                        label = stringResource(Res.string.frequency_every_n_days, interval),
                        quantity = interval,
                        onChange = { interval = it.coerceIn(2, 30) },
                        range = 2..30,
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.field_track_stock),
                    style = RemedioTheme.type.rowTitle,
                    color = RemedioTheme.colors.ink,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = trackStock, onCheckedChange = { trackStock = it })
            }
            if (trackStock) {
                OutlinedTextField(
                    value = remaining,
                    onValueChange = { remaining = it.filter(Char::isDigit).take(4) },
                    label = { Text(stringResource(Res.string.field_units_left)) },
                    singleLine = true,
                    shape = FieldShape,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = threshold,
                    onValueChange = { threshold = it.filter(Char::isDigit).take(4) },
                    label = { Text(stringResource(Res.string.field_low_threshold)) },
                    singleLine = true,
                    shape = FieldShape,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(Res.string.field_notes)) },
                minLines = 2,
                shape = FieldShape,
                modifier = Modifier.fillMaxWidth(),
            )

            if (medication != null) {
                TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.action_delete), style = RemedioTheme.type.button, color = RemedioTheme.colors.danger)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmDelete && medication != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            text = { Text(stringResource(Res.string.editor_delete_confirm, medication.displayName)) },
            confirmButton = {
                Button(onClick = { onDelete(medication) }) { Text(stringResource(Res.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(Res.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun FieldTitle(text: String) {
    Text(text.uppercase(), style = RemedioTheme.type.eyebrow, color = RemedioTheme.colors.inkMute, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun SelectableTile(
    selected: Boolean,
    onClick: () -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .clip(shape)
            .glass(shape, RemedioTheme.colors, strong = selected)
            .border(if (selected) 2.dp else 0.dp, if (selected) RemedioTheme.colors.accent else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = description
                this.selected = selected
            }.padding(vertical = 12.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}

@Composable
private fun QuantityRow(
    label: String,
    quantity: Int,
    onChange: (Int) -> Unit,
    range: IntRange = 0..9,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = RemedioTheme.type.rowTitle, color = RemedioTheme.colors.ink, modifier = Modifier.weight(1f))
        StepButton("−", enabled = quantity > range.first) { onChange((quantity - 1).coerceIn(range)) }
        Text(
            quantity.toString(),
            style = RemedioTheme.type.title,
            color = RemedioTheme.colors.ink,
            modifier = Modifier.width(44.dp).padding(horizontal = 8.dp),
            textAlign = TextAlign.Center,
        )
        StepButton("+", enabled = quantity < range.last) { onChange((quantity + 1).coerceIn(range)) }
    }
}

@Composable
private fun StepButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val palette = RemedioTheme.colors
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (enabled) palette.accentSoft else palette.line)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = RemedioTheme.type.title, color = if (enabled) palette.accent else palette.inkMute)
    }
}

@Composable
private fun FrequencyChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = RemedioTheme.colors
    Text(
        label,
        style = RemedioTheme.type.button,
        color = if (selected) Color.White else palette.inkSoft,
        modifier =
            Modifier
                .clip(CircleShape)
                .then(if (selected) Modifier.background(palette.brandBrush) else Modifier.glass(CircleShape, palette))
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 9.dp),
    )
}
