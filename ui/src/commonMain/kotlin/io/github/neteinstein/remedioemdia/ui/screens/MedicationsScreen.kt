package io.github.neteinstein.remedioemdia.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.neteinstein.remedioemdia.core.model.Frequency
import io.github.neteinstein.remedioemdia.core.model.Medication
import io.github.neteinstein.remedioemdia.core.model.TimeSlot
import io.github.neteinstein.remedioemdia.core.schedule.Inventory
import io.github.neteinstein.remedioemdia.ui.components.SectionHeader
import io.github.neteinstein.remedioemdia.ui.components.glass
import io.github.neteinstein.remedioemdia.ui.components.label
import io.github.neteinstein.remedioemdia.ui.components.shortLabel
import io.github.neteinstein.remedioemdia.ui.pill.PillIcon
import io.github.neteinstein.remedioemdia.ui.resources.Res
import io.github.neteinstein.remedioemdia.ui.resources.action_add_medication
import io.github.neteinstein.remedioemdia.ui.resources.empty_title
import io.github.neteinstein.remedioemdia.ui.resources.frequency_daily
import io.github.neteinstein.remedioemdia.ui.resources.frequency_every_n_days
import io.github.neteinstein.remedioemdia.ui.resources.medications_count
import io.github.neteinstein.remedioemdia.ui.resources.nav_medications
import io.github.neteinstein.remedioemdia.ui.resources.units_left
import io.github.neteinstein.remedioemdia.ui.theme.RemedioColors
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun MedicationsScreen(
    medications: List<Medication>,
    onEdit: (Medication) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                SectionHeader(
                    title = stringResource(Res.string.nav_medications),
                    meta = pluralStringResource(Res.plurals.medications_count, medications.size, medications.size),
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            if (medications.isEmpty()) {
                item {
                    Text(
                        stringResource(Res.string.empty_title),
                        color = RemedioColors.TextSecondary,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
            items(medications, key = { it.id }) { medication ->
                MedicationRow(medication, onClick = { onEdit(medication) })
            }
        }
        ExtendedFloatingActionButton(
            onClick = onAdd,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text(stringResource(Res.string.action_add_medication)) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
        )
    }
}

@Composable
private fun MedicationRow(
    medication: Medication,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .glass(shape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PillIcon(medication.shape, medication.color, height = 28.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(medication.displayName, style = MaterialTheme.typography.titleMedium, color = RemedioColors.TextPrimary)
            Text(scheduleSummary(medication), style = MaterialTheme.typography.bodyMedium, color = RemedioColors.TextSecondary)
            medication.stock?.let { stock ->
                Text(
                    pluralStringResource(Res.plurals.units_left, stock.remaining, stock.remaining),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (Inventory.isLow(medication)) RemedioColors.Amber else RemedioColors.TextSecondary,
                )
            }
        }
    }
}

/** "Manhã, Deitar · Todos os dias" */
@Composable
private fun scheduleSummary(medication: Medication): String {
    val slots =
        TimeSlot.entries
            .filter { (medication.schedule.doses[it] ?: 0) > 0 }
            .map { slot ->
                val quantity = medication.schedule.doses[slot] ?: 0
                val name = stringResource(slot.label)
                if (quantity > 1) "$name ×$quantity" else name
            }
    val frequency =
        when (val frequency = medication.schedule.frequency) {
            Frequency.Daily -> {
                stringResource(Res.string.frequency_daily)
            }

            is Frequency.Weekdays -> {
                frequency.days
                    .sorted()
                    .map { stringResource(it.shortLabel) }
                    .joinToString(" ")
            }

            is Frequency.EveryNDays -> {
                stringResource(Res.string.frequency_every_n_days, frequency.interval)
            }
        }
    return (slots.joinToString(", ") + " · " + frequency)
}
