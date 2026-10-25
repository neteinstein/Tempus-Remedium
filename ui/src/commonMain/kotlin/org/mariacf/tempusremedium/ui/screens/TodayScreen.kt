package org.mariacf.tempusremedium.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.mariacf.tempusremedium.core.model.DoseStatus
import org.mariacf.tempusremedium.core.model.Medication
import org.mariacf.tempusremedium.core.model.PillColor
import org.mariacf.tempusremedium.core.model.PillShape
import org.mariacf.tempusremedium.core.model.StockDisplay
import org.mariacf.tempusremedium.core.model.TimeSlot
import org.mariacf.tempusremedium.core.schedule.CourseProgress
import org.mariacf.tempusremedium.core.schedule.DayPlan
import org.mariacf.tempusremedium.core.schedule.LowStockItem
import org.mariacf.tempusremedium.core.schedule.PlannedDose
import org.mariacf.tempusremedium.ui.components.BrandButton
import org.mariacf.tempusremedium.ui.components.Eyebrow
import org.mariacf.tempusremedium.ui.components.GroupCard
import org.mariacf.tempusremedium.ui.components.GroupDivider
import org.mariacf.tempusremedium.ui.components.HojePanel
import org.mariacf.tempusremedium.ui.components.ReporEmBrevePanel
import org.mariacf.tempusremedium.ui.components.SoftButton
import org.mariacf.tempusremedium.ui.components.doseLabel
import org.mariacf.tempusremedium.ui.components.hhmm
import org.mariacf.tempusremedium.ui.components.label
import org.mariacf.tempusremedium.ui.components.longDateLabel
import org.mariacf.tempusremedium.ui.components.relativeDay
import org.mariacf.tempusremedium.ui.pill.PillIcon
import org.mariacf.tempusremedium.ui.pill.fill
import org.mariacf.tempusremedium.ui.resources.Res
import org.mariacf.tempusremedium.ui.resources.action_add_course
import org.mariacf.tempusremedium.ui.resources.action_add_medication
import org.mariacf.tempusremedium.ui.resources.action_cancel
import org.mariacf.tempusremedium.ui.resources.action_refill
import org.mariacf.tempusremedium.ui.resources.action_skip
import org.mariacf.tempusremedium.ui.resources.action_take_all
import org.mariacf.tempusremedium.ui.resources.action_taken
import org.mariacf.tempusremedium.ui.resources.action_undo
import org.mariacf.tempusremedium.ui.resources.course_next
import org.mariacf.tempusremedium.ui.resources.course_remaining
import org.mariacf.tempusremedium.ui.resources.courses_title
import org.mariacf.tempusremedium.ui.resources.days_count
import org.mariacf.tempusremedium.ui.resources.empty_body
import org.mariacf.tempusremedium.ui.resources.empty_title
import org.mariacf.tempusremedium.ui.resources.greeting
import org.mariacf.tempusremedium.ui.resources.greeting_done
import org.mariacf.tempusremedium.ui.resources.greeting_nothing
import org.mariacf.tempusremedium.ui.resources.greeting_pending
import org.mariacf.tempusremedium.ui.resources.refill_dialog_title
import org.mariacf.tempusremedium.ui.resources.refill_dialog_units
import org.mariacf.tempusremedium.ui.resources.slot_empty
import org.mariacf.tempusremedium.ui.resources.slot_sheet_title
import org.mariacf.tempusremedium.ui.resources.status_skipped
import org.mariacf.tempusremedium.ui.resources.status_taken_at
import org.mariacf.tempusremedium.ui.resources.takes_count
import org.mariacf.tempusremedium.ui.resources.units
import org.mariacf.tempusremedium.ui.theme.TempusTheme
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** Opens part of the Today screen on launch, e.g. from a widget or a reminder. */
sealed interface LaunchRequest {
    data class OpenSlot(
        val slot: TimeSlot,
    ) : LaunchRequest

    data class OpenRefill(
        val medicationId: String,
    ) : LaunchRequest
}

@Composable
fun TodayScreen(
    now: LocalDateTime,
    plan: DayPlan,
    lowStock: List<LowStockItem>,
    medications: List<Medication>,
    onLogDose: (Medication, TimeSlot, DoseStatus) -> Unit,
    onUndoDose: (Medication, TimeSlot) -> Unit,
    onLogSlot: (TimeSlot) -> Unit,
    onRefill: (Medication, Int) -> Unit,
    onAddMedication: () -> Unit,
    modifier: Modifier = Modifier,
    stockDisplay: StockDisplay = StockDisplay.JARS,
    stockAlerts: Set<String> = emptySet(),
    onAcknowledgeStockAlert: (Medication) -> Unit = {},
    launchRequest: LaunchRequest? = null,
    onConsumeLaunchRequest: () -> Unit = {},
    courses: List<CourseProgress> = emptyList(),
    onLogCourseDose: (Medication, Int, DoseStatus) -> Unit = { _, _, _ -> },
    onUndoCourseDose: (Medication, Int) -> Unit = { _, _ -> },
    onAddCourse: () -> Unit = {},
) {
    var openSlot by rememberSaveable { mutableStateOf<TimeSlot?>(null) }
    var refillId by rememberSaveable { mutableStateOf<String?>(null) }

    val consumeLaunchRequest by rememberUpdatedState(onConsumeLaunchRequest)
    LaunchedEffect(launchRequest) {
        when (launchRequest) {
            is LaunchRequest.OpenSlot -> openSlot = launchRequest.slot
            is LaunchRequest.OpenRefill -> refillId = launchRequest.medicationId
            null -> return@LaunchedEffect
        }
        consumeLaunchRequest()
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
    ) {
        Greeting(now, plan)
        Spacer(Modifier.height(28.dp))
        if (medications.isEmpty()) {
            EmptyState(onAddMedication, onAddCourse)
            return@Column
        }
        HojePanel(
            plan,
            highlight = plan.slots.firstOrNull { it.hasPending }?.slot,
            overdue = plan.overdueSlots(now),
            onSlotClick = { openSlot = it },
        )
        if (courses.isNotEmpty()) {
            Spacer(Modifier.height(36.dp))
            CoursesCard(courses, now, onLogCourseDose, onUndoCourseDose)
        }
        if (lowStock.isNotEmpty()) {
            Spacer(Modifier.height(36.dp))
            ReporEmBrevePanel(
                lowStock,
                display = stockDisplay,
                alerts = stockAlerts,
                onItemClick = { item ->
                    if (item.medication.id in stockAlerts) onAcknowledgeStockAlert(item.medication)
                    refillId = item.medication.id
                },
            )
        }
    }

    openSlot?.let { slot ->
        SlotSheet(
            plan = plan,
            slot = slot,
            onDismiss = { openSlot = null },
            onLogDose = onLogDose,
            onUndoDose = onUndoDose,
            onLogSlot = onLogSlot,
        )
    }

    refillId
        ?.let { id -> medications.firstOrNull { it.id == id && it.stock != null } }
        ?.let { medication ->
            RefillDialog(
                medication = medication,
                onDismiss = { refillId = null },
                onConfirm = { units ->
                    onRefill(medication, units)
                    refillId = null
                },
            )
        }
}

/** "● QUINTA-FEIRA, 22 DE OUTUBRO / Olá, *faltam 3 doses.*" */
@Composable
private fun Greeting(
    now: LocalDateTime,
    plan: DayPlan,
) {
    val palette = TempusTheme.colors
    val type = TempusTheme.type
    val hello = stringResource(Res.string.greeting)
    val pending = plan.slots.sumOf { slot -> slot.doses.count { it.status == null } } + plan.courseDoses.count { it.status == null }
    val status =
        when {
            plan.doseCount == 0 && plan.courseDoses.isEmpty() -> stringResource(Res.string.greeting_nothing)
            pending == 0 -> stringResource(Res.string.greeting_done)
            else -> pluralStringResource(Res.plurals.greeting_pending, pending, pending)
        }
    Column(Modifier.padding(horizontal = 8.dp)) {
        Eyebrow(longDateLabel(now.date))
        Spacer(Modifier.height(12.dp))
        Text(
            buildAnnotatedString {
                append(hello)
                append('\n')
                withStyle(
                    SpanStyle(
                        fontStyle = type.headlineAccent.fontStyle,
                        fontWeight = type.headlineAccent.fontWeight,
                        color = palette.accent,
                    ),
                ) {
                    append(status)
                }
            },
            style = type.headline,
            color = palette.ink,
            modifier = Modifier.semantics { heading() },
        )
    }
}

@Composable
private fun EmptyState(
    onAddMedication: () -> Unit,
    onAddCourse: () -> Unit,
) {
    GroupCard {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                PillIcon(PillShape.ROUND_SCORED, PillColor.TEAL, 28.dp)
                PillIcon(PillShape.CAPSULE, PillColor.PEACH, 28.dp)
                PillIcon(PillShape.SOFTGEL, PillColor.BLUE, 28.dp)
            }
            Text(stringResource(Res.string.empty_title), style = TempusTheme.type.title, color = TempusTheme.colors.ink)
            Text(stringResource(Res.string.empty_body), style = TempusTheme.type.bodyText, color = TempusTheme.colors.inkSoft)
            Spacer(Modifier.height(4.dp))
            BrandButton(stringResource(Res.string.action_add_medication), onClick = onAddMedication)
            SoftButton(stringResource(Res.string.action_add_course), onClick = onAddCourse)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlotSheet(
    plan: DayPlan,
    slot: TimeSlot,
    onDismiss: () -> Unit,
    onLogDose: (Medication, TimeSlot, DoseStatus) -> Unit,
    onUndoDose: (Medication, TimeSlot) -> Unit,
    onLogSlot: (TimeSlot) -> Unit,
) {
    val palette = TempusTheme.colors
    val slotPlan = plan.slot(slot)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (palette.isDark) palette.background2 else palette.background,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.slot_sheet_title, stringResource(slot.label), slotPlan.time.hhmm()),
                    style = TempusTheme.type.title,
                    color = palette.ink,
                    modifier = Modifier.weight(1f),
                )
                if (slotPlan.doses.count { it.status == null } > 1) {
                    BrandButton(stringResource(Res.string.action_take_all), onClick = { onLogSlot(slot) })
                }
            }
            if (slotPlan.doses.isEmpty()) {
                Text(stringResource(Res.string.slot_empty), style = TempusTheme.type.bodyText, color = palette.inkSoft)
            } else {
                GroupCard {
                    slotPlan.doses.forEachIndexed { index, dose ->
                        if (index > 0) GroupDivider()
                        DoseRow(
                            dose = dose,
                            onTake = { onLogDose(dose.medication, slot, DoseStatus.TAKEN) },
                            onSkip = { onLogDose(dose.medication, slot, DoseStatus.SKIPPED) },
                            onUndo = { onUndoDose(dose.medication, slot) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DoseRow(
    dose: PlannedDose,
    onTake: () -> Unit,
    onSkip: () -> Unit,
    onUndo: () -> Unit,
) {
    val palette = TempusTheme.colors
    Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        PillChip(dose.medication, alpha = if (dose.status == null) 1f else 0.4f)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(dose.medication.displayName, style = TempusTheme.type.rowTitle, color = palette.ink)
            val detail =
                when (dose.status) {
                    null -> pluralStringResource(Res.plurals.units, dose.quantity, dose.quantity)
                    DoseStatus.TAKEN -> stringResource(Res.string.status_taken_at, dose.loggedAtEpochMillis.localTime())
                    DoseStatus.SKIPPED -> stringResource(Res.string.status_skipped)
                }
            Text(detail, style = TempusTheme.type.bodySmall, color = palette.inkMute)
        }
        if (dose.status == null) {
            TextButton(
                onClick = onSkip,
            ) { Text(stringResource(Res.string.action_skip), style = TempusTheme.type.button, color = palette.inkSoft) }
            BrandButton(stringResource(Res.string.action_taken), onClick = onTake)
        } else {
            SoftButton(stringResource(Res.string.action_undo), onClick = onUndo)
        }
    }
}

/** "TRATAMENTOS": courses taken every few hours, outside the HOJE panel. */
@Composable
private fun CoursesCard(
    courses: List<CourseProgress>,
    now: LocalDateTime,
    onLogCourseDose: (Medication, Int, DoseStatus) -> Unit,
    onUndoCourseDose: (Medication, Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Eyebrow(stringResource(Res.string.courses_title), modifier = Modifier.padding(horizontal = 8.dp))
        GroupCard {
            courses.forEachIndexed { index, progress ->
                if (index > 0) GroupDivider()
                CourseRow(progress, now, onLogCourseDose, onUndoCourseDose)
            }
        }
    }
}

@Composable
private fun CourseRow(
    progress: CourseProgress,
    now: LocalDateTime,
    onLogCourseDose: (Medication, Int, DoseStatus) -> Unit,
    onUndoCourseDose: (Medication, Int) -> Unit,
) {
    val palette = TempusTheme.colors
    val medication = progress.medication
    val course = medication.course ?: return
    val next = progress.next ?: return
    // A dose can be logged from an hour before it is due.
    val due = next.at <= now.plusMinutes(TAKE_EARLY_MINUTES)
    val lastLogged = progress.lastLogged
    Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PillChip(medication)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(medication.displayName, style = TempusTheme.type.rowTitle, color = palette.ink)
                Text(
                    stringResource(Res.string.course_next, relativeDay(next.at.date, now.date), next.at.time.hhmm(), course.doseLabel()),
                    style = TempusTheme.type.bodySmall,
                    color = if (due) palette.accent else palette.inkMute,
                )
                Text(
                    stringResource(
                        Res.string.course_remaining,
                        pluralStringResource(Res.plurals.takes_count, progress.remaining, progress.remaining),
                        pluralStringResource(Res.plurals.days_count, progress.remainingDays(now.date), progress.remainingDays(now.date)),
                    ),
                    style = TempusTheme.type.bodySmall,
                    color = palette.inkMute,
                )
            }
        }
        if (due || lastLogged != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                if (due) {
                    TextButton(onClick = { onLogCourseDose(medication, next.index, DoseStatus.SKIPPED) }) {
                        Text(stringResource(Res.string.action_skip), style = TempusTheme.type.button, color = palette.inkSoft)
                    }
                    BrandButton(
                        stringResource(Res.string.action_taken),
                        onClick = { onLogCourseDose(medication, next.index, DoseStatus.TAKEN) },
                    )
                } else if (lastLogged != null) {
                    SoftButton(stringResource(Res.string.action_undo), onClick = { onUndoCourseDose(medication, lastLogged.index) })
                }
            }
        }
    }
}

private const val TAKE_EARLY_MINUTES = 60

private fun LocalDateTime.plusMinutes(minutes: Int): LocalDateTime =
    toInstant(TimeZone.UTC).plus(minutes.minutes).toLocalDateTime(TimeZone.UTC)

/** A pill on a soft disc of its own colour, like the icon chips on mariacf.pt. */
@Composable
fun PillChip(
    medication: Medication,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
) {
    Box(
        modifier
            .size(44.dp)
            .background(medication.color.fill.copy(alpha = 0.28f * alpha), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        PillIcon(medication.shape, medication.color, height = 24.dp, alpha = alpha)
    }
}

private fun Long?.localTime(): String =
    this
        ?.let {
            Instant
                .fromEpochMilliseconds(it)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .time
                .hhmm()
        }.orEmpty()

@Composable
private fun RefillDialog(
    medication: Medication,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var text by remember { mutableStateOf("30") }
    val units = text.toIntOrNull()?.takeIf { it > 0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TempusTheme.colors.background,
        title = { Text(stringResource(Res.string.refill_dialog_title, medication.name), style = TempusTheme.type.title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { value -> text = value.filter(Char::isDigit).take(4) },
                label = { Text(stringResource(Res.string.refill_dialog_units)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = {
            BrandButton(stringResource(Res.string.action_refill), onClick = { units?.let(onConfirm) }, enabled = units != null)
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel), style = TempusTheme.type.button, color = TempusTheme.colors.inkSoft)
            }
        },
    )
}
