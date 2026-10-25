package org.mariacf.tempusremedium.ui.screens

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
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.mariacf.tempusremedium.core.model.Frequency
import org.mariacf.tempusremedium.core.model.Medication
import org.mariacf.tempusremedium.core.model.PillColor
import org.mariacf.tempusremedium.core.model.PillPortion
import org.mariacf.tempusremedium.core.model.PillShape
import org.mariacf.tempusremedium.core.model.Schedule
import org.mariacf.tempusremedium.core.model.Stock
import org.mariacf.tempusremedium.core.model.TimeSlot
import org.mariacf.tempusremedium.core.model.newMedicationId
import org.mariacf.tempusremedium.core.schedule.lastDueDate
import org.mariacf.tempusremedium.ui.PlatformBackHandler
import org.mariacf.tempusremedium.ui.components.BrandButton
import org.mariacf.tempusremedium.ui.components.brandBackground
import org.mariacf.tempusremedium.ui.components.doseLabel
import org.mariacf.tempusremedium.ui.components.frequencyLabel
import org.mariacf.tempusremedium.ui.components.glass
import org.mariacf.tempusremedium.ui.components.label
import org.mariacf.tempusremedium.ui.components.longDateLabel
import org.mariacf.tempusremedium.ui.components.shortLabel
import org.mariacf.tempusremedium.ui.pill.PillIcon
import org.mariacf.tempusremedium.ui.pill.PillInk
import org.mariacf.tempusremedium.ui.pill.edge
import org.mariacf.tempusremedium.ui.pill.fill
import org.mariacf.tempusremedium.ui.resources.Res
import org.mariacf.tempusremedium.ui.resources.action_back
import org.mariacf.tempusremedium.ui.resources.action_cancel
import org.mariacf.tempusremedium.ui.resources.action_delete
import org.mariacf.tempusremedium.ui.resources.action_save
import org.mariacf.tempusremedium.ui.resources.contraceptive_hint
import org.mariacf.tempusremedium.ui.resources.cycle_days_off
import org.mariacf.tempusremedium.ui.resources.cycle_days_on
import org.mariacf.tempusremedium.ui.resources.day_today
import org.mariacf.tempusremedium.ui.resources.day_tomorrow
import org.mariacf.tempusremedium.ui.resources.editor_delete_confirm
import org.mariacf.tempusremedium.ui.resources.editor_edit
import org.mariacf.tempusremedium.ui.resources.editor_new
import org.mariacf.tempusremedium.ui.resources.end_kind_date
import org.mariacf.tempusremedium.ui.resources.end_kind_occurrences
import org.mariacf.tempusremedium.ui.resources.end_last_day
import org.mariacf.tempusremedium.ui.resources.end_last_time
import org.mariacf.tempusremedium.ui.resources.end_never
import org.mariacf.tempusremedium.ui.resources.every_weeks
import org.mariacf.tempusremedium.ui.resources.field_color
import org.mariacf.tempusremedium.ui.resources.field_end
import org.mariacf.tempusremedium.ui.resources.field_frequency
import org.mariacf.tempusremedium.ui.resources.field_low_threshold
import org.mariacf.tempusremedium.ui.resources.field_name
import org.mariacf.tempusremedium.ui.resources.field_notes
import org.mariacf.tempusremedium.ui.resources.field_portion
import org.mariacf.tempusremedium.ui.resources.field_shape
import org.mariacf.tempusremedium.ui.resources.field_start
import org.mariacf.tempusremedium.ui.resources.field_strength
import org.mariacf.tempusremedium.ui.resources.field_track_stock
import org.mariacf.tempusremedium.ui.resources.field_units_left
import org.mariacf.tempusremedium.ui.resources.field_when
import org.mariacf.tempusremedium.ui.resources.frequency_contraceptive
import org.mariacf.tempusremedium.ui.resources.frequency_cycle
import org.mariacf.tempusremedium.ui.resources.frequency_daily
import org.mariacf.tempusremedium.ui.resources.frequency_every_n_days
import org.mariacf.tempusremedium.ui.resources.frequency_monthly
import org.mariacf.tempusremedium.ui.resources.frequency_weekdays
import org.mariacf.tempusremedium.ui.resources.portion_half
import org.mariacf.tempusremedium.ui.resources.portion_quarter
import org.mariacf.tempusremedium.ui.resources.portion_whole
import org.mariacf.tempusremedium.ui.resources.times_count
import org.mariacf.tempusremedium.ui.theme.TempusTheme

private enum class FrequencyKind { DAILY, WEEKDAYS, EVERY_N_DAYS, MONTHLY, CYCLE, CONTRACEPTIVE }

private enum class ScheduleEnd { NEVER, DATE, OCCURRENCES }

internal val FieldShape = RoundedCornerShape(14.dp)

/** How far the start and end dates can be moved, in days. */
private const val MAX_DATE_OFFSET = 730

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
    var portion by remember { mutableStateOf(medication?.portion ?: PillPortion.WHOLE) }
    val effectivePortion = if (PillPortion.canSplit(shape)) portion else PillPortion.WHOLE
    val initialParts = medication?.portion?.parts ?: 1
    var doses by remember { mutableStateOf(medication?.schedule?.doses ?: mapOf(TimeSlot.MORNING to 1)) }
    val initialFrequency = medication?.schedule?.frequency ?: Frequency.Daily
    var frequencyKind by remember {
        mutableStateOf(
            when (initialFrequency) {
                Frequency.Daily -> FrequencyKind.DAILY
                is Frequency.Weekdays -> FrequencyKind.WEEKDAYS
                is Frequency.EveryNDays -> FrequencyKind.EVERY_N_DAYS
                is Frequency.Monthly -> FrequencyKind.MONTHLY
                is Frequency.Cycle -> FrequencyKind.CYCLE
                is Frequency.Contraceptive -> FrequencyKind.CONTRACEPTIVE
            },
        )
    }
    var weekdays by remember { mutableStateOf((initialFrequency as? Frequency.Weekdays)?.days ?: DayOfWeek.entries.toSet()) }
    var everyWeeks by remember { mutableStateOf((initialFrequency as? Frequency.Weekdays)?.everyWeeks ?: 1) }
    var interval by remember { mutableStateOf((initialFrequency as? Frequency.EveryNDays)?.interval ?: 3) }
    var everyMonths by remember { mutableStateOf((initialFrequency as? Frequency.Monthly)?.everyMonths ?: 1) }
    var daysOn by remember { mutableStateOf((initialFrequency as? Frequency.Cycle)?.daysOn ?: 21) }
    var daysOff by remember { mutableStateOf((initialFrequency as? Frequency.Cycle)?.daysOff ?: 7) }
    var pillDaysOn by remember { mutableStateOf((initialFrequency as? Frequency.Contraceptive)?.daysOn ?: 21) }
    var pillDaysOff by remember { mutableStateOf((initialFrequency as? Frequency.Contraceptive)?.daysOff ?: 7) }
    // Dates are edited as day offsets: the start from today, the end from the start.
    val initialStart = medication?.schedule?.startDate ?: today
    var startOffset by remember { mutableStateOf(today.daysUntil(initialStart)) }
    val startDate = today.plus(DatePeriod(days = startOffset))
    var endKind by remember {
        mutableStateOf(
            when {
                medication?.schedule?.occurrences != null -> ScheduleEnd.OCCURRENCES
                medication?.schedule?.endDate != null -> ScheduleEnd.DATE
                else -> ScheduleEnd.NEVER
            },
        )
    }
    var lastDayOffset by remember {
        mutableStateOf(medication?.schedule?.endDate?.let { initialStart.daysUntil(it).coerceAtLeast(0) } ?: 29)
    }
    var occurrences by remember { mutableStateOf(medication?.schedule?.occurrences ?: 10) }
    var trackStock by remember { mutableStateOf(medication?.stock != null) }
    var remaining by remember { mutableStateOf(medication?.stockTablets?.toString() ?: "30") }
    val initialTablets = remaining
    var threshold by remember {
        mutableStateOf(
            medication
                ?.stock
                ?.lowThreshold
                ?.let { it / initialParts }
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

    val schedule =
        Schedule(
            doses = doses.filterValues { it > 0 },
            frequency =
                when (frequencyKind) {
                    FrequencyKind.DAILY -> Frequency.Daily
                    FrequencyKind.WEEKDAYS -> Frequency.Weekdays(weekdays, everyWeeks)
                    FrequencyKind.EVERY_N_DAYS -> Frequency.EveryNDays(interval)
                    FrequencyKind.MONTHLY -> Frequency.Monthly(everyMonths)
                    FrequencyKind.CYCLE -> Frequency.Cycle(daysOn, daysOff)
                    FrequencyKind.CONTRACEPTIVE -> Frequency.Contraceptive(pillDaysOn, pillDaysOff)
                },
            startDate = startDate,
            endDate = if (endKind == ScheduleEnd.DATE) startDate.plus(DatePeriod(days = lastDayOffset)) else null,
            occurrences = if (endKind == ScheduleEnd.OCCURRENCES) occurrences else null,
        )

    fun build(): Medication =
        Medication(
            id = medication?.id ?: newMedicationId(),
            name = name.trim(),
            strength = strength.trim(),
            shape = shape,
            color = color,
            schedule = schedule,
            stock =
                if (trackStock) {
                    val parts = effectivePortion.parts
                    val storedRemaining = medication?.stock?.remaining
                    val unchanged = storedRemaining != null && remaining == initialTablets && parts == initialParts
                    Stock(
                        remaining = if (unchanged && storedRemaining != null) storedRemaining else (remaining.toIntOrNull() ?: 0) * parts,
                        lowThreshold = threshold.toIntOrNull()?.times(parts),
                    )
                } else {
                    null
                },
            portion = effectivePortion,
            notes = notes.trim(),
            archived = medication?.archived ?: false,
        )

    Column(modifier.fillMaxSize().brandBackground(TempusTheme.colors)) {
        TopAppBar(
            title = {
                Text(
                    stringResource(if (medication == null) Res.string.editor_new else Res.string.editor_edit),
                    style = TempusTheme.type.title.copy(fontSize = 21.sp),
                    color = TempusTheme.colors.ink,
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
            ShapePicker(shape, color, onChange = { shape = it })

            if (PillPortion.canSplit(shape)) {
                FieldTitle(stringResource(Res.string.field_portion))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FrequencyChip(stringResource(Res.string.portion_whole), portion == PillPortion.WHOLE) { portion = PillPortion.WHOLE }
                    FrequencyChip(stringResource(Res.string.portion_half), portion == PillPortion.HALF) { portion = PillPortion.HALF }
                    FrequencyChip(stringResource(Res.string.portion_quarter), portion == PillPortion.QUARTER) {
                        portion = PillPortion.QUARTER
                    }
                }
            }

            FieldTitle(stringResource(Res.string.field_color))
            ColorPicker(color, onChange = { color = it })

            FieldTitle(stringResource(Res.string.field_when))
            TimeSlot.entries.forEach { slot ->
                QuantityRow(
                    label =
                        stringResource(slot.label) +
                            if (effectivePortion != PillPortion.WHOLE && (doses[slot] ?: 0) > 0) {
                                " · " +
                                    doseLabel(
                                        Medication("", "", shape = shape, color = color, schedule = schedule, portion = effectivePortion),
                                        doses[slot] ?: 0,
                                    )
                            } else {
                                ""
                            },
                    quantity = doses[slot] ?: 0,
                    onChange = { doses = doses + (slot to it) },
                )
            }

            FieldTitle(stringResource(Res.string.field_frequency))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                FrequencyChip(stringResource(Res.string.frequency_monthly), frequencyKind == FrequencyKind.MONTHLY) {
                    frequencyKind = FrequencyKind.MONTHLY
                }
                FrequencyChip(stringResource(Res.string.frequency_cycle), frequencyKind == FrequencyKind.CYCLE) {
                    frequencyKind = FrequencyKind.CYCLE
                }
                FrequencyChip(stringResource(Res.string.frequency_contraceptive), frequencyKind == FrequencyKind.CONTRACEPTIVE) {
                    frequencyKind = FrequencyKind.CONTRACEPTIVE
                }
            }
            when (frequencyKind) {
                FrequencyKind.DAILY -> {}

                FrequencyKind.WEEKDAYS -> {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        DayOfWeek.entries.forEach { day ->
                            FrequencyChip(stringResource(day.shortLabel), day in weekdays) {
                                weekdays = if (day in weekdays) weekdays - day else weekdays + day
                            }
                        }
                    }
                    QuantityRow(
                        label = pluralStringResource(Res.plurals.every_weeks, everyWeeks, everyWeeks),
                        quantity = everyWeeks,
                        onChange = { everyWeeks = it },
                        range = 1..12,
                    )
                }

                FrequencyKind.EVERY_N_DAYS -> {
                    QuantityRow(
                        label = stringResource(Res.string.frequency_every_n_days, interval),
                        quantity = interval,
                        onChange = { interval = it },
                        range = 2..90,
                    )
                }

                FrequencyKind.MONTHLY -> {
                    QuantityRow(
                        label = frequencyLabel(Frequency.Monthly(everyMonths), startDate),
                        quantity = everyMonths,
                        onChange = { everyMonths = it },
                        range = 1..12,
                    )
                }

                FrequencyKind.CYCLE -> {
                    QuantityRow(stringResource(Res.string.cycle_days_on), daysOn, { daysOn = it }, range = 1..90)
                    QuantityRow(stringResource(Res.string.cycle_days_off), daysOff, { daysOff = it }, range = 1..90)
                }

                FrequencyKind.CONTRACEPTIVE -> {
                    QuantityRow(stringResource(Res.string.cycle_days_on), pillDaysOn, { pillDaysOn = it }, range = 1..90)
                    QuantityRow(stringResource(Res.string.cycle_days_off), pillDaysOff, { pillDaysOff = it }, range = 0..90)
                    Text(
                        stringResource(Res.string.contraceptive_hint),
                        style = TempusTheme.type.bodySmall,
                        color = TempusTheme.colors.inkSoft,
                    )
                }
            }

            FieldTitle(stringResource(Res.string.field_start))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FrequencyChip(stringResource(Res.string.day_today), startOffset == 0) { startOffset = 0 }
                FrequencyChip(stringResource(Res.string.day_tomorrow), startOffset == 1) { startOffset = 1 }
            }
            QuantityRow(
                label = longDateLabel(startDate),
                quantity = startOffset,
                onChange = { startOffset = it },
                range = minOf(-MAX_DATE_OFFSET, startOffset)..MAX_DATE_OFFSET,
                showValue = false,
            )

            FieldTitle(stringResource(Res.string.field_end))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FrequencyChip(stringResource(Res.string.end_never), endKind == ScheduleEnd.NEVER) { endKind = ScheduleEnd.NEVER }
                FrequencyChip(stringResource(Res.string.end_kind_date), endKind == ScheduleEnd.DATE) { endKind = ScheduleEnd.DATE }
                FrequencyChip(stringResource(Res.string.end_kind_occurrences), endKind == ScheduleEnd.OCCURRENCES) {
                    endKind = ScheduleEnd.OCCURRENCES
                }
            }
            when (endKind) {
                ScheduleEnd.NEVER -> {}

                ScheduleEnd.DATE -> {
                    QuantityRow(
                        label =
                            stringResource(Res.string.end_last_day) + " · " +
                                longDateLabel(startDate.plus(DatePeriod(days = lastDayOffset))),
                        quantity = lastDayOffset,
                        onChange = { lastDayOffset = it },
                        range = 0..MAX_DATE_OFFSET,
                        showValue = false,
                    )
                }

                ScheduleEnd.OCCURRENCES -> {
                    QuantityRow(
                        label = pluralStringResource(Res.plurals.times_count, occurrences, occurrences),
                        quantity = occurrences,
                        onChange = { occurrences = it },
                        range = 1..365,
                    )
                    schedule.lastDueDate()?.let { last ->
                        Text(
                            stringResource(Res.string.end_last_time) + " · " + longDateLabel(last),
                            style = TempusTheme.type.bodySmall,
                            color = TempusTheme.colors.inkSoft,
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.field_track_stock),
                    style = TempusTheme.type.rowTitle,
                    color = TempusTheme.colors.ink,
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
                    Text(stringResource(Res.string.action_delete), style = TempusTheme.type.button, color = TempusTheme.colors.danger)
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
internal fun ShapePicker(
    shape: PillShape,
    color: PillColor,
    onChange: (PillShape) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Three per row: the device names ("Penso transdérmico") need the width.
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PillShape.entries.chunked(SHAPES_PER_ROW).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { option ->
                    val description = stringResource(option.label)
                    SelectableTile(
                        selected = option == shape,
                        onClick = { onChange(option) },
                        description = description,
                        modifier = Modifier.weight(1f),
                    ) {
                        PillIcon(option, color, height = 28.dp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            description,
                            style = TempusTheme.type.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 14.sp),
                            color = TempusTheme.colors.inkSoft,
                            minLines = 2,
                            maxLines = 2,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                repeat(SHAPES_PER_ROW - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private const val SHAPES_PER_ROW = 3

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ColorPicker(
    color: PillColor,
    onChange: (PillColor) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        if (selected) TempusTheme.colors.accent else option.edge.copy(alpha = 0.5f),
                        CircleShape,
                    ).clickable { onChange(option) }
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
}

@Composable
internal fun FieldTitle(text: String) {
    Text(text.uppercase(), style = TempusTheme.type.eyebrow, color = TempusTheme.colors.inkMute, modifier = Modifier.padding(top = 4.dp))
}

@Composable
internal fun SelectableTile(
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
            .glass(shape, TempusTheme.colors, strong = selected)
            .border(if (selected) 2.dp else 0.dp, if (selected) TempusTheme.colors.accent else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = description
                this.selected = selected
            }.padding(vertical = 12.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}

@Composable
internal fun QuantityRow(
    label: String,
    quantity: Int,
    onChange: (Int) -> Unit,
    range: IntRange = 0..9,
    showValue: Boolean = true,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = TempusTheme.type.rowTitle, color = TempusTheme.colors.ink, modifier = Modifier.weight(1f))
        StepButton("−", enabled = quantity > range.first) { onChange((quantity - 1).coerceIn(range)) }
        Text(
            if (showValue) quantity.toString() else "",
            style = TempusTheme.type.title,
            color = TempusTheme.colors.ink,
            modifier = Modifier.width(if (showValue) 44.dp else 12.dp).padding(horizontal = 8.dp),
            textAlign = TextAlign.Center,
        )
        StepButton("+", enabled = quantity < range.last) { onChange((quantity + 1).coerceIn(range)) }
    }
}

@Composable
internal fun StepButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val palette = TempusTheme.colors
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (enabled) palette.accentSoft else palette.line)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = TempusTheme.type.title, color = if (enabled) palette.accent else palette.inkMute)
    }
}

@Composable
internal fun FrequencyChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = TempusTheme.colors
    Text(
        label,
        style = TempusTheme.type.button,
        color = if (selected) Color.White else palette.inkSoft,
        modifier =
            Modifier
                .clip(CircleShape)
                .then(if (selected) Modifier.background(palette.brandBrush) else Modifier.glass(CircleShape, palette))
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 9.dp),
    )
}
