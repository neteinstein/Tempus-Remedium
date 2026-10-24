package io.github.neteinstein.remedioemdia.ui.screens

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
import io.github.neteinstein.remedioemdia.core.model.DoseStatus
import io.github.neteinstein.remedioemdia.core.model.Medication
import io.github.neteinstein.remedioemdia.core.model.PillColor
import io.github.neteinstein.remedioemdia.core.model.PillShape
import io.github.neteinstein.remedioemdia.core.model.StockDisplay
import io.github.neteinstein.remedioemdia.core.model.TimeSlot
import io.github.neteinstein.remedioemdia.core.schedule.DayPlan
import io.github.neteinstein.remedioemdia.core.schedule.LowStockItem
import io.github.neteinstein.remedioemdia.core.schedule.PlannedDose
import io.github.neteinstein.remedioemdia.ui.components.BrandButton
import io.github.neteinstein.remedioemdia.ui.components.Eyebrow
import io.github.neteinstein.remedioemdia.ui.components.GroupCard
import io.github.neteinstein.remedioemdia.ui.components.GroupDivider
import io.github.neteinstein.remedioemdia.ui.components.HojePanel
import io.github.neteinstein.remedioemdia.ui.components.ReporEmBrevePanel
import io.github.neteinstein.remedioemdia.ui.components.SoftButton
import io.github.neteinstein.remedioemdia.ui.components.hhmm
import io.github.neteinstein.remedioemdia.ui.components.label
import io.github.neteinstein.remedioemdia.ui.components.longDateLabel
import io.github.neteinstein.remedioemdia.ui.pill.PillIcon
import io.github.neteinstein.remedioemdia.ui.pill.fill
import io.github.neteinstein.remedioemdia.ui.resources.Res
import io.github.neteinstein.remedioemdia.ui.resources.action_add_medication
import io.github.neteinstein.remedioemdia.ui.resources.action_cancel
import io.github.neteinstein.remedioemdia.ui.resources.action_refill
import io.github.neteinstein.remedioemdia.ui.resources.action_skip
import io.github.neteinstein.remedioemdia.ui.resources.action_take_all
import io.github.neteinstein.remedioemdia.ui.resources.action_taken
import io.github.neteinstein.remedioemdia.ui.resources.action_undo
import io.github.neteinstein.remedioemdia.ui.resources.empty_body
import io.github.neteinstein.remedioemdia.ui.resources.empty_title
import io.github.neteinstein.remedioemdia.ui.resources.greeting_afternoon
import io.github.neteinstein.remedioemdia.ui.resources.greeting_done
import io.github.neteinstein.remedioemdia.ui.resources.greeting_evening
import io.github.neteinstein.remedioemdia.ui.resources.greeting_morning
import io.github.neteinstein.remedioemdia.ui.resources.greeting_nothing
import io.github.neteinstein.remedioemdia.ui.resources.greeting_pending
import io.github.neteinstein.remedioemdia.ui.resources.refill_dialog_title
import io.github.neteinstein.remedioemdia.ui.resources.refill_dialog_units
import io.github.neteinstein.remedioemdia.ui.resources.slot_empty
import io.github.neteinstein.remedioemdia.ui.resources.slot_sheet_title
import io.github.neteinstein.remedioemdia.ui.resources.status_skipped
import io.github.neteinstein.remedioemdia.ui.resources.status_taken_at
import io.github.neteinstein.remedioemdia.ui.resources.units
import io.github.neteinstein.remedioemdia.ui.theme.RemedioTheme
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
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
    launchRequest: LaunchRequest? = null,
    onConsumeLaunchRequest: () -> Unit = {},
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
            EmptyState(onAddMedication)
            return@Column
        }
        HojePanel(
            plan,
            highlight = plan.slots.firstOrNull { slot -> slot.doses.any { it.status == null } }?.slot,
            onSlotClick = { openSlot = it },
        )
        if (lowStock.isNotEmpty()) {
            Spacer(Modifier.height(36.dp))
            ReporEmBrevePanel(lowStock, display = stockDisplay, onItemClick = { refillId = it.medication.id })
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

/** "● QUINTA-FEIRA, 22 DE OUTUBRO / Boa tarde, *faltam 3 doses.*" */
@Composable
private fun Greeting(
    now: LocalDateTime,
    plan: DayPlan,
) {
    val palette = RemedioTheme.colors
    val type = RemedioTheme.type
    val hello =
        stringResource(
            when (now.hour) {
                in 5..11 -> Res.string.greeting_morning
                in 12..19 -> Res.string.greeting_afternoon
                else -> Res.string.greeting_evening
            },
        )
    val pending = plan.slots.sumOf { slot -> slot.doses.count { it.status == null } }
    val status =
        when {
            plan.doseCount == 0 -> stringResource(Res.string.greeting_nothing)
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
private fun EmptyState(onAddMedication: () -> Unit) {
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
            Text(stringResource(Res.string.empty_title), style = RemedioTheme.type.title, color = RemedioTheme.colors.ink)
            Text(stringResource(Res.string.empty_body), style = RemedioTheme.type.bodyText, color = RemedioTheme.colors.inkSoft)
            Spacer(Modifier.height(4.dp))
            BrandButton(stringResource(Res.string.action_add_medication), onClick = onAddMedication)
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
    val palette = RemedioTheme.colors
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
                    style = RemedioTheme.type.title,
                    color = palette.ink,
                    modifier = Modifier.weight(1f),
                )
                if (slotPlan.doses.count { it.status == null } > 1) {
                    BrandButton(stringResource(Res.string.action_take_all), onClick = { onLogSlot(slot) })
                }
            }
            if (slotPlan.doses.isEmpty()) {
                Text(stringResource(Res.string.slot_empty), style = RemedioTheme.type.bodyText, color = palette.inkSoft)
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
    val palette = RemedioTheme.colors
    Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        PillChip(dose.medication, alpha = if (dose.status == null) 1f else 0.4f)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(dose.medication.displayName, style = RemedioTheme.type.rowTitle, color = palette.ink)
            val detail =
                when (dose.status) {
                    null -> pluralStringResource(Res.plurals.units, dose.quantity, dose.quantity)
                    DoseStatus.TAKEN -> stringResource(Res.string.status_taken_at, dose.loggedAtEpochMillis.localTime())
                    DoseStatus.SKIPPED -> stringResource(Res.string.status_skipped)
                }
            Text(detail, style = RemedioTheme.type.bodySmall, color = palette.inkMute)
        }
        if (dose.status == null) {
            TextButton(
                onClick = onSkip,
            ) { Text(stringResource(Res.string.action_skip), style = RemedioTheme.type.button, color = palette.inkSoft) }
            BrandButton(stringResource(Res.string.action_taken), onClick = onTake)
        } else {
            SoftButton(stringResource(Res.string.action_undo), onClick = onUndo)
        }
    }
}

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
        containerColor = RemedioTheme.colors.background,
        title = { Text(stringResource(Res.string.refill_dialog_title, medication.name), style = RemedioTheme.type.title) },
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
                Text(stringResource(Res.string.action_cancel), style = RemedioTheme.type.button, color = RemedioTheme.colors.inkSoft)
            }
        },
    )
}
