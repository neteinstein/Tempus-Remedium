package org.mariacf.tempusremedium.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.mariacf.tempusremedium.core.dosing.CalculatorMedication
import org.mariacf.tempusremedium.core.dosing.DoseCalculation
import org.mariacf.tempusremedium.core.dosing.DoseCalculator
import org.mariacf.tempusremedium.core.dosing.DoseSuggestion
import org.mariacf.tempusremedium.core.model.Course
import org.mariacf.tempusremedium.core.model.CourseEnd
import org.mariacf.tempusremedium.core.model.DoseUnit
import org.mariacf.tempusremedium.core.model.Medication
import org.mariacf.tempusremedium.core.model.PillColor
import org.mariacf.tempusremedium.core.model.PillShape
import org.mariacf.tempusremedium.core.model.Schedule
import org.mariacf.tempusremedium.core.model.Stock
import org.mariacf.tempusremedium.core.model.newMedicationId
import org.mariacf.tempusremedium.core.schedule.plannedDoseCount
import org.mariacf.tempusremedium.core.schedule.plannedLastDose
import org.mariacf.tempusremedium.ui.PlatformBackHandler
import org.mariacf.tempusremedium.ui.components.BrandButton
import org.mariacf.tempusremedium.ui.components.brandBackground
import org.mariacf.tempusremedium.ui.components.dayLabel
import org.mariacf.tempusremedium.ui.components.decimal
import org.mariacf.tempusremedium.ui.components.doseLabel
import org.mariacf.tempusremedium.ui.components.glass
import org.mariacf.tempusremedium.ui.components.hhmm
import org.mariacf.tempusremedium.ui.components.intervalLabel
import org.mariacf.tempusremedium.ui.components.label
import org.mariacf.tempusremedium.ui.components.longDateLabel
import org.mariacf.tempusremedium.ui.components.substance
import org.mariacf.tempusremedium.ui.resources.Res
import org.mariacf.tempusremedium.ui.resources.action_back
import org.mariacf.tempusremedium.ui.resources.action_cancel
import org.mariacf.tempusremedium.ui.resources.action_delete
import org.mariacf.tempusremedium.ui.resources.action_save
import org.mariacf.tempusremedium.ui.resources.calc_adult
import org.mariacf.tempusremedium.ui.resources.calc_below_min_weight
import org.mariacf.tempusremedium.ui.resources.calc_clav
import org.mariacf.tempusremedium.ui.resources.calc_clav_excessive
import org.mariacf.tempusremedium.ui.resources.calc_complete_course
import org.mariacf.tempusremedium.ui.resources.calc_concentration
import org.mariacf.tempusremedium.ui.resources.calc_daily_dose
import org.mariacf.tempusremedium.ui.resources.calc_disclaimer
import org.mariacf.tempusremedium.ui.resources.calc_doses_per_day
import org.mariacf.tempusremedium.ui.resources.calc_formulation
import org.mariacf.tempusremedium.ui.resources.calc_formulation_clav
import org.mariacf.tempusremedium.ui.resources.calc_infants_4_1
import org.mariacf.tempusremedium.ui.resources.calc_invalid
import org.mariacf.tempusremedium.ui.resources.calc_limited
import org.mariacf.tempusremedium.ui.resources.calc_mg_per_kg_day
import org.mariacf.tempusremedium.ui.resources.calc_need_weight
import org.mariacf.tempusremedium.ui.resources.calc_prefer_14_1
import org.mariacf.tempusremedium.ui.resources.calc_result
import org.mariacf.tempusremedium.ui.resources.calc_result_detail
import org.mariacf.tempusremedium.ui.resources.calc_times_a_day
import org.mariacf.tempusremedium.ui.resources.calc_title
import org.mariacf.tempusremedium.ui.resources.calc_too_small
import org.mariacf.tempusremedium.ui.resources.calc_use
import org.mariacf.tempusremedium.ui.resources.calc_weight
import org.mariacf.tempusremedium.ui.resources.calc_weight_range
import org.mariacf.tempusremedium.ui.resources.course_edit
import org.mariacf.tempusremedium.ui.resources.course_intro
import org.mariacf.tempusremedium.ui.resources.course_new
import org.mariacf.tempusremedium.ui.resources.course_suggestions
import org.mariacf.tempusremedium.ui.resources.course_summary
import org.mariacf.tempusremedium.ui.resources.course_summary_empty
import org.mariacf.tempusremedium.ui.resources.day_today
import org.mariacf.tempusremedium.ui.resources.day_tomorrow
import org.mariacf.tempusremedium.ui.resources.days_count
import org.mariacf.tempusremedium.ui.resources.editor_delete_confirm
import org.mariacf.tempusremedium.ui.resources.end_kind_date
import org.mariacf.tempusremedium.ui.resources.end_kind_days
import org.mariacf.tempusremedium.ui.resources.end_kind_doses
import org.mariacf.tempusremedium.ui.resources.end_kind_pack
import org.mariacf.tempusremedium.ui.resources.end_last_day
import org.mariacf.tempusremedium.ui.resources.field_amount
import org.mariacf.tempusremedium.ui.resources.field_color
import org.mariacf.tempusremedium.ui.resources.field_dose_per_take
import org.mariacf.tempusremedium.ui.resources.field_end
import org.mariacf.tempusremedium.ui.resources.field_first_dose
import org.mariacf.tempusremedium.ui.resources.field_interval
import org.mariacf.tempusremedium.ui.resources.field_name
import org.mariacf.tempusremedium.ui.resources.field_notes
import org.mariacf.tempusremedium.ui.resources.field_pack_amount
import org.mariacf.tempusremedium.ui.resources.field_shape
import org.mariacf.tempusremedium.ui.resources.field_strength
import org.mariacf.tempusremedium.ui.resources.field_time
import org.mariacf.tempusremedium.ui.resources.field_track_stock
import org.mariacf.tempusremedium.ui.resources.field_units_left
import org.mariacf.tempusremedium.ui.resources.takes_count
import org.mariacf.tempusremedium.ui.resources.unit_ml
import org.mariacf.tempusremedium.ui.resources.unit_units
import org.mariacf.tempusremedium.ui.resources.unit_units_short
import org.mariacf.tempusremedium.ui.theme.TempusTheme

private enum class EndKind { DAYS, DOSES, DATE, PACK }

/** Intervals offered as one-tap chips; any other is reached with the stepper. */
private val QuickIntervals = listOf(4, 6, 8, 12, 24)

/**
 * Full-screen form for a course taken every few hours (an antibiotic, say), new when [medication]
 * is null. While the user types the name, the author's dose calculator offers a dose, an interval
 * and a length; nothing is filled in until the user taps "Usar sugestão".
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CourseEditor(
    medication: Medication?,
    now: LocalDateTime,
    onSave: (Medication) -> Unit,
    onDelete: (Medication) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    initialName: String = "",
    initialCalculator: CalculatorMedication? = null,
    initialWeight: String = "",
) {
    PlatformBackHandler(enabled = true, onBack = onClose)
    val existing = medication?.course
    val today = now.date

    var name by remember { mutableStateOf(medication?.name ?: initialName) }
    var strength by remember { mutableStateOf(medication?.strength.orEmpty()) }
    var shape by remember { mutableStateOf(medication?.shape ?: PillShape.CAPSULE) }
    var color by remember { mutableStateOf(medication?.color ?: PillColor.YELLOW) }
    var amount by remember { mutableStateOf(existing?.amount?.let { formatInput(it) }.orEmpty()) }
    var unit by remember { mutableStateOf(existing?.unit ?: DoseUnit.ML) }
    var interval by remember { mutableStateOf(existing?.intervalHours ?: 8) }
    var startOffset by remember {
        mutableStateOf(existing?.let { today.daysUntil(it.firstDose.date) } ?: 0)
    }
    var time by remember { mutableStateOf((existing?.firstDose?.time ?: LocalTime(now.hour, now.minute / 5 * 5)).hhmm()) }
    val end = existing?.end
    var endKind by remember {
        mutableStateOf(
            when (end) {
                is CourseEnd.AfterDoses -> EndKind.DOSES
                is CourseEnd.OnDate -> EndKind.DATE
                is CourseEnd.FinishPack -> EndKind.PACK
                else -> EndKind.DAYS
            },
        )
    }
    var days by remember { mutableStateOf((end as? CourseEnd.AfterDays)?.days ?: 7) }
    var doses by remember { mutableStateOf((end as? CourseEnd.AfterDoses)?.doses ?: 21) }
    var lastDayOffset by remember {
        mutableStateOf((end as? CourseEnd.OnDate)?.let { existing.firstDose.date.daysUntil(it.date) } ?: 6)
    }
    var pack by remember { mutableStateOf((end as? CourseEnd.FinishPack)?.packAmount?.let { formatInput(it) }.orEmpty()) }
    var trackStock by remember { mutableStateOf(medication?.stock != null) }
    var remaining by remember { mutableStateOf(medication?.stock?.remaining?.toString() ?: "20") }
    var notes by remember { mutableStateOf(medication?.notes.orEmpty()) }
    var confirmDelete by remember { mutableStateOf(false) }

    // Dose calculator state: which suggestion the user picked and the inputs it needs.
    var calculator by remember { mutableStateOf(initialCalculator) }
    var weight by remember { mutableStateOf(initialWeight) }
    var formulationIndex by remember { mutableIntStateOf(0) }
    var presetIndex by remember { mutableIntStateOf(0) }
    var calculatorInterval by remember { mutableStateOf(initialCalculator?.defaultInterval(initialCalculator.formulations.first())) }

    fun chooseCalculator(option: CalculatorMedication) {
        calculator = option
        formulationIndex = 0
        presetIndex = 0
        calculatorInterval = option.defaultInterval(option.formulations.first())
    }

    val firstDose = parseTime(time)?.let { LocalDateTime(today.plus(DatePeriod(days = startOffset)), it) }
    val amountValue = parseDecimal(amount)?.takeIf { it > 0 }
    val packValue = parseDecimal(pack)?.takeIf { it > 0 }
    val draft =
        if (firstDose != null && amountValue != null && (endKind != EndKind.PACK || packValue != null)) {
            Course(
                firstDose = firstDose,
                intervalHours = interval,
                amount = amountValue,
                unit = unit,
                end =
                    when (endKind) {
                        EndKind.DAYS -> CourseEnd.AfterDays(days)
                        EndKind.DOSES -> CourseEnd.AfterDoses(doses)
                        EndKind.DATE -> CourseEnd.OnDate(firstDose.date.plus(DatePeriod(days = lastDayOffset)))
                        EndKind.PACK -> CourseEnd.FinishPack(packValue ?: 0.0)
                    },
            )
        } else {
            null
        }
    val canSave =
        name.isNotBlank() &&
            draft != null &&
            draft.plannedDoseCount() > 0 &&
            (!trackStock || unit != DoseUnit.UNITS || remaining.toIntOrNull() != null)

    fun build(course: Course): Medication =
        Medication(
            id = medication?.id ?: newMedicationId(),
            name = name.trim(),
            strength = strength.trim(),
            shape = shape,
            color = color,
            schedule = Schedule(doses = emptyMap(), startDate = course.firstDose.date),
            stock =
                if (trackStock && unit == DoseUnit.UNITS) {
                    Stock(remaining = remaining.toIntOrNull() ?: 0, lowThreshold = medication?.stock?.lowThreshold)
                } else {
                    null
                },
            notes = notes.trim(),
            archived = medication?.archived ?: false,
            course = course,
        )

    Column(modifier.fillMaxSize().brandBackground(TempusTheme.colors)) {
        TopAppBar(
            title = {
                Text(
                    stringResource(if (medication == null) Res.string.course_new else Res.string.course_edit),
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
                    onClick = { draft?.let { onSave(build(it)) } },
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
            if (medication == null) {
                Text(stringResource(Res.string.course_intro), style = TempusTheme.type.bodyText, color = TempusTheme.colors.inkSoft)
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(Res.string.field_name)) },
                singleLine = true,
                shape = FieldShape,
                modifier = Modifier.fillMaxWidth(),
            )
            val matches = DoseCalculator.match(name)
            if (matches.isNotEmpty() && medication == null) {
                FieldTitle(stringResource(Res.string.course_suggestions))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    matches.forEach { option ->
                        val optionName = stringResource(option.label)
                        FrequencyChip(optionName, selected = option == calculator) {
                            chooseCalculator(option)
                            name = optionName
                        }
                    }
                }
            }
            calculator?.let { option ->
                val strengthLabels = option.formulations.map { formulationLabelPlain(it.mgPer5Ml, it.clavPer5Ml) }
                CalculatorCard(
                    medication = option,
                    weight = weight,
                    onWeightChange = { weight = it },
                    formulationIndex = formulationIndex,
                    onFormulationChange = { index ->
                        formulationIndex = index
                        option.formulations[index].intervalHours?.let { calculatorInterval = it }
                    },
                    presetIndex = presetIndex,
                    onPresetChange = { presetIndex = it },
                    interval = calculatorInterval,
                    onIntervalChange = { calculatorInterval = it },
                    onUse = { suggestion ->
                        amount = formatInput(suggestion.ml)
                        unit = DoseUnit.ML
                        interval = suggestion.intervalHours
                        trackStock = false
                        if (strength.isBlank() && suggestion.formulation != null) strength = strengthLabels[formulationIndex]
                        suggestion.suggestedDays?.let {
                            endKind = EndKind.DAYS
                            days = it
                        }
                    },
                )
            }
            OutlinedTextField(
                value = strength,
                onValueChange = { strength = it },
                label = { Text(stringResource(Res.string.field_strength)) },
                singleLine = true,
                shape = FieldShape,
                modifier = Modifier.fillMaxWidth(),
            )

            FieldTitle(stringResource(Res.string.field_dose_per_take))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(6) },
                    label = { Text(stringResource(Res.string.field_amount)) },
                    singleLine = true,
                    shape = FieldShape,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FrequencyChip(stringResource(Res.string.unit_ml), unit == DoseUnit.ML) { unit = DoseUnit.ML }
                FrequencyChip(stringResource(Res.string.unit_units), unit == DoseUnit.UNITS) { unit = DoseUnit.UNITS }
            }

            FieldTitle(stringResource(Res.string.field_interval))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickIntervals.forEach { hours ->
                    FrequencyChip("$hours h", interval == hours) { interval = hours }
                }
            }
            QuantityRow(
                label = intervalLabel(interval),
                quantity = interval,
                onChange = { interval = it },
                range = 1..72,
            )

            FieldTitle(stringResource(Res.string.field_first_dose))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FrequencyChip(stringResource(Res.string.day_today), startOffset == 0) { startOffset = 0 }
                FrequencyChip(stringResource(Res.string.day_tomorrow), startOffset == 1) { startOffset = 1 }
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it.filter { c -> c.isDigit() || c == ':' }.take(5) },
                    label = { Text(stringResource(Res.string.field_time)) },
                    singleLine = true,
                    isError = parseTime(time) == null,
                    shape = FieldShape,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }

            FieldTitle(stringResource(Res.string.field_end))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FrequencyChip(stringResource(Res.string.end_kind_days), endKind == EndKind.DAYS) { endKind = EndKind.DAYS }
                FrequencyChip(stringResource(Res.string.end_kind_doses), endKind == EndKind.DOSES) { endKind = EndKind.DOSES }
                FrequencyChip(stringResource(Res.string.end_kind_date), endKind == EndKind.DATE) { endKind = EndKind.DATE }
                FrequencyChip(stringResource(Res.string.end_kind_pack), endKind == EndKind.PACK) { endKind = EndKind.PACK }
            }
            when (endKind) {
                EndKind.DAYS -> {
                    QuantityRow(pluralStringResource(Res.plurals.days_count, days, days), days, { days = it }, range = 1..90)
                }

                EndKind.DOSES -> {
                    QuantityRow(pluralStringResource(Res.plurals.takes_count, doses, doses), doses, { doses = it }, range = 1..200)
                }

                EndKind.DATE -> {
                    val startDate = today.plus(DatePeriod(days = startOffset))
                    QuantityRow(
                        label =
                            stringResource(Res.string.end_last_day) + " · " +
                                longDateLabel(startDate.plus(DatePeriod(days = lastDayOffset))),
                        quantity = lastDayOffset,
                        onChange = { lastDayOffset = it },
                        range = 0..90,
                        showValue = false,
                    )
                }

                EndKind.PACK -> {
                    OutlinedTextField(
                        value = pack,
                        onValueChange = { pack = it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(6) },
                        label = {
                            Text(
                                stringResource(
                                    Res.string.field_pack_amount,
                                    stringResource(if (unit == DoseUnit.ML) Res.string.unit_ml else Res.string.unit_units_short),
                                ),
                            )
                        },
                        singleLine = true,
                        shape = FieldShape,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            CourseSummary(draft)

            FieldTitle(stringResource(Res.string.field_shape))
            ShapePicker(shape, color, onChange = { shape = it })
            FieldTitle(stringResource(Res.string.field_color))
            ColorPicker(color, onChange = { color = it })

            if (unit == DoseUnit.UNITS) {
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
                }
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

/** "21 tomas · última toma qua., 29 às 00:00". */
@Composable
private fun CourseSummary(course: Course?) {
    val count = course?.plannedDoseCount() ?: 0
    val last = course?.plannedLastDose()
    val text =
        if (count == 0 || last == null) {
            stringResource(Res.string.course_summary_empty)
        } else {
            stringResource(
                Res.string.course_summary,
                pluralStringResource(Res.plurals.takes_count, count, count),
                dayLabel(last.date),
                last.time.hhmm(),
            )
        }
    Text(text, style = TempusTheme.type.rowTitle, color = if (count == 0) TempusTheme.colors.danger else TempusTheme.colors.accent)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalculatorCard(
    medication: CalculatorMedication,
    weight: String,
    onWeightChange: (String) -> Unit,
    formulationIndex: Int,
    onFormulationChange: (Int) -> Unit,
    presetIndex: Int,
    onPresetChange: (Int) -> Unit,
    interval: Int?,
    onIntervalChange: (Int) -> Unit,
    onUse: (DoseSuggestion) -> Unit,
) {
    val palette = TempusTheme.colors
    val shape = RoundedCornerShape(22.dp)
    val formulation = medication.formulations[formulationIndex]
    val preset = medication.presets[presetIndex]
    val result =
        DoseCalculator.calculate(
            weightKg = parseDecimal(weight),
            medication = medication,
            mgPerMl = formulation.mgPerMl,
            mgPerKgDay = preset.mgPerKgDay,
            intervalHours = interval,
        )
    Column(
        Modifier.fillMaxWidth().glass(shape, palette).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(Res.string.calc_title), style = TempusTheme.type.title, color = palette.ink)
        OutlinedTextField(
            value = weight,
            onValueChange = { onWeightChange(it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(5)) },
            label = { Text(stringResource(Res.string.calc_weight)) },
            singleLine = true,
            shape = FieldShape,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        FieldTitle(stringResource(Res.string.calc_concentration))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            medication.formulations.forEachIndexed { index, option ->
                FrequencyChip(formulationLabel(option.mgPer5Ml, option.clavPer5Ml, option.ratio), index == formulationIndex) {
                    onFormulationChange(index)
                }
            }
        }
        FieldTitle(stringResource(Res.string.calc_daily_dose))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            medication.presets.forEachIndexed { index, option ->
                PresetRow(
                    title = stringResource(Res.string.calc_mg_per_kg_day, option.mgPerKgDay.decimal()),
                    detail = stringResource(option.indication.label),
                    selected = index == presetIndex,
                    onClick = { onPresetChange(index) },
                )
            }
        }
        if (medication.intervals.size > 1) {
            FieldTitle(stringResource(Res.string.calc_doses_per_day))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                medication.intervals.forEach { hours ->
                    FrequencyChip(stringResource(Res.string.calc_times_a_day, 24 / hours), hours == interval) { onIntervalChange(hours) }
                }
            }
        }
        when (result) {
            is DoseCalculation.Suggested -> SuggestionResult(result.dose, onUse)
            else -> Text(calculationProblem(result, medication), style = TempusTheme.type.bodyText, color = palette.inkSoft)
        }
        Text(stringResource(Res.string.calc_disclaimer), style = TempusTheme.type.bodySmall, color = palette.inkMute)
    }
}

@Composable
private fun SuggestionResult(
    dose: DoseSuggestion,
    onUse: (DoseSuggestion) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val palette = TempusTheme.colors
        val substance = stringResource(dose.medication.substance)
        Text(
            stringResource(Res.string.calc_result, doseLabel(dose.ml, DoseUnit.ML), intervalLabel(dose.intervalHours).lowercaseFirst()),
            style = TempusTheme.type.title,
            color = palette.accent,
        )
        Text(
            stringResource(Res.string.calc_result_detail, dose.mgPerDose.decimal(), substance, dose.dosesPerDay.toString()) +
                (dose.suggestedDays?.let { " · " + pluralStringResource(Res.plurals.days_count, it, it) } ?: ""),
            style = TempusTheme.type.bodyText,
            color = palette.ink,
        )
        val notices =
            buildList {
                if (dose.limited) add(stringResource(Res.string.calc_limited, dose.maxMgPerDay.decimal(), substance))
                val clav = dose.clavMgPerDose
                val clavPerKg = dose.clavMgPerKgDay
                if (clav != null && clavPerKg != null) {
                    add(
                        if (dose.clavExcessive) {
                            stringResource(Res.string.calc_clav_excessive, clavPerKg.decimal(), dose.maxClavMgPerKgDay.decimal())
                        } else {
                            stringResource(Res.string.calc_clav, dose.mgPerDose.decimal(), clav.decimal(), clavPerKg.decimal())
                        },
                    )
                }
                if (dose.prefer14To1) add(stringResource(Res.string.calc_prefer_14_1))
                if (dose.ratioNotForInfants) add(stringResource(Res.string.calc_infants_4_1))
                if (dose.adultWeight) add(stringResource(Res.string.calc_adult))
                add(stringResource(Res.string.calc_complete_course))
            }
        notices.forEach { Text("· $it", style = TempusTheme.type.bodySmall, color = palette.inkSoft) }
        BrandButton(stringResource(Res.string.calc_use), onClick = { onUse(dose) })
    }
}

@Composable
private fun PresetRow(
    title: String,
    detail: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = TempusTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .glass(shape, palette, strong = selected)
            .then(if (selected) Modifier.background(palette.accentSoft) else Modifier)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(title, style = TempusTheme.type.rowTitle, color = if (selected) palette.accent else palette.ink)
        Text(detail, style = TempusTheme.type.bodySmall, color = palette.inkMute)
    }
}

@Composable
private fun calculationProblem(
    result: DoseCalculation,
    medication: CalculatorMedication,
): String =
    when (result) {
        DoseCalculation.WeightMissing -> {
            stringResource(Res.string.calc_need_weight)
        }

        is DoseCalculation.WeightOutOfRange -> {
            stringResource(Res.string.calc_weight_range, result.minKg.decimal(), result.maxKg.decimal())
        }

        is DoseCalculation.BelowMinimumWeight -> {
            stringResource(Res.string.calc_below_min_weight, stringResource(medication.label), result.minKg.decimal())
        }

        DoseCalculation.VolumeTooSmall -> {
            stringResource(Res.string.calc_too_small)
        }

        else -> {
            stringResource(Res.string.calc_invalid)
        }
    }

/** "250 mg/5 mL" or "400 + 57 mg/5 mL · 7:1". */
@Composable
private fun formulationLabel(
    mgPer5Ml: Double,
    clavPer5Ml: Double?,
    ratio: String?,
): String =
    if (clavPer5Ml == null || ratio == null) {
        stringResource(Res.string.calc_formulation, mgPer5Ml.decimal())
    } else {
        stringResource(Res.string.calc_formulation_clav, mgPer5Ml.decimal(), clavPer5Ml.decimal(), ratio)
    }

/** The strength field is free text, so it takes the label without the ratio. */
@Composable
private fun formulationLabelPlain(
    mgPer5Ml: Double,
    clavPer5Ml: Double?,
): String =
    if (clavPer5Ml == null) {
        stringResource(Res.string.calc_formulation, mgPer5Ml.decimal())
    } else {
        "${mgPer5Ml.decimal()} + ${stringResource(Res.string.calc_formulation, clavPer5Ml.decimal())}"
    }

private fun String.lowercaseFirst(): String = replaceFirstChar { it.lowercase() }

/** "6.5" or "6,5" -> 6.5. */
private fun parseDecimal(text: String): Double? = text.replace(',', '.').toDoubleOrNull()

/** Shows a stored amount in an input field without trailing zeros. */
private fun formatInput(value: Double): String = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

/** "8:05" or "08:05" -> 08:05; null when it is not a valid time. */
private fun parseTime(text: String): LocalTime? {
    val parts = text.split(':')
    if (parts.size != 2 || parts[1].length != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    return if (hour in 0..23 && minute in 0..59) LocalTime(hour, minute) else null
}
