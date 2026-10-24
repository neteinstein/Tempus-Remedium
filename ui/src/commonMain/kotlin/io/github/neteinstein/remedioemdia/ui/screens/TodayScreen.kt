package io.github.neteinstein.remedioemdia.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.neteinstein.remedioemdia.core.model.DoseStatus
import io.github.neteinstein.remedioemdia.core.model.Medication
import io.github.neteinstein.remedioemdia.core.model.TimeSlot
import io.github.neteinstein.remedioemdia.core.schedule.DayPlan
import io.github.neteinstein.remedioemdia.core.schedule.LowStockItem
import io.github.neteinstein.remedioemdia.core.schedule.PlannedDose
import io.github.neteinstein.remedioemdia.ui.components.HojePanel
import io.github.neteinstein.remedioemdia.ui.components.ReporEmBrevePanel
import io.github.neteinstein.remedioemdia.ui.components.hhmm
import io.github.neteinstein.remedioemdia.ui.components.label
import io.github.neteinstein.remedioemdia.ui.pill.PillIcon
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
import io.github.neteinstein.remedioemdia.ui.resources.refill_dialog_title
import io.github.neteinstein.remedioemdia.ui.resources.refill_dialog_units
import io.github.neteinstein.remedioemdia.ui.resources.slot_empty
import io.github.neteinstein.remedioemdia.ui.resources.slot_sheet_title
import io.github.neteinstein.remedioemdia.ui.resources.status_skipped
import io.github.neteinstein.remedioemdia.ui.resources.status_taken_at
import io.github.neteinstein.remedioemdia.ui.resources.units
import io.github.neteinstein.remedioemdia.ui.theme.RemedioColors
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
    plan: DayPlan,
    lowStock: List<LowStockItem>,
    medications: List<Medication>,
    onLogDose: (Medication, TimeSlot, DoseStatus) -> Unit,
    onUndoDose: (Medication, TimeSlot) -> Unit,
    onLogSlot: (TimeSlot) -> Unit,
    onRefill: (Medication, Int) -> Unit,
    onAddMedication: () -> Unit,
    modifier: Modifier = Modifier,
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
        if (medications.isEmpty()) {
            EmptyState(onAddMedication)
            return@Column
        }
        HojePanel(plan, onSlotClick = { openSlot = it })
        if (lowStock.isNotEmpty()) {
            Spacer(Modifier.height(40.dp))
            ReporEmBrevePanel(lowStock, onItemClick = { refillId = it.medication.id })
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

@Composable
private fun EmptyState(onAddMedication: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(Res.string.empty_title), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(Res.string.empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = RemedioColors.TextSecondary,
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = onAddMedication) { Text(stringResource(Res.string.action_add_medication)) }
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
    val slotPlan = plan.slot(slot)
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = RemedioColors.Surface) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.slot_sheet_title, stringResource(slot.label), slotPlan.time.hhmm()),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                if (slotPlan.doses.count { it.status == null } > 1) {
                    Button(onClick = { onLogSlot(slot) }) { Text(stringResource(Res.string.action_take_all)) }
                }
            }
            if (slotPlan.doses.isEmpty()) {
                Text(stringResource(Res.string.slot_empty), color = RemedioColors.TextSecondary)
            }
            slotPlan.doses.forEach { dose ->
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

@Composable
private fun DoseRow(
    dose: PlannedDose,
    onTake: () -> Unit,
    onSkip: () -> Unit,
    onUndo: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        PillIcon(dose.medication.shape, dose.medication.color, height = 28.dp, alpha = if (dose.status == null) 1f else 0.4f)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(dose.medication.displayName, style = MaterialTheme.typography.titleMedium)
            val detail =
                when (dose.status) {
                    null -> pluralStringResource(Res.plurals.units, dose.quantity, dose.quantity)
                    DoseStatus.TAKEN -> stringResource(Res.string.status_taken_at, dose.loggedAtEpochMillis.localTime())
                    DoseStatus.SKIPPED -> stringResource(Res.string.status_skipped)
                }
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = RemedioColors.TextSecondary)
        }
        if (dose.status == null) {
            TextButton(onClick = onSkip) { Text(stringResource(Res.string.action_skip)) }
            Button(onClick = onTake) { Text(stringResource(Res.string.action_taken)) }
        } else {
            OutlinedButton(onClick = onUndo) { Text(stringResource(Res.string.action_undo)) }
        }
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
        title = { Text(stringResource(Res.string.refill_dialog_title, medication.name)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { value -> text = value.filter(Char::isDigit).take(4) },
                label = { Text(stringResource(Res.string.refill_dialog_units)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = {
            Button(onClick = { units?.let(onConfirm) }, enabled = units != null) {
                Text(stringResource(Res.string.action_refill))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    )
}
