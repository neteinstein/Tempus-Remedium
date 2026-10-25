package org.mariacf.tempusremedium.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.mariacf.tempusremedium.core.model.Medication
import org.mariacf.tempusremedium.core.model.TimeSlot
import org.mariacf.tempusremedium.core.schedule.Inventory
import org.mariacf.tempusremedium.ui.components.BrandButton
import org.mariacf.tempusremedium.ui.components.ScreenHeader
import org.mariacf.tempusremedium.ui.components.SoftButton
import org.mariacf.tempusremedium.ui.components.doseLabel
import org.mariacf.tempusremedium.ui.components.glass
import org.mariacf.tempusremedium.ui.components.intervalLabel
import org.mariacf.tempusremedium.ui.components.label
import org.mariacf.tempusremedium.ui.components.scheduleLabel
import org.mariacf.tempusremedium.ui.resources.Res
import org.mariacf.tempusremedium.ui.resources.action_add_course
import org.mariacf.tempusremedium.ui.resources.action_add_medication
import org.mariacf.tempusremedium.ui.resources.course_schedule
import org.mariacf.tempusremedium.ui.resources.empty_body
import org.mariacf.tempusremedium.ui.resources.frequency_daily
import org.mariacf.tempusremedium.ui.resources.frequency_every_n_days
import org.mariacf.tempusremedium.ui.resources.medications_count
import org.mariacf.tempusremedium.ui.resources.nav_medications
import org.mariacf.tempusremedium.ui.resources.units_left
import org.mariacf.tempusremedium.ui.theme.TempusTheme

@Composable
fun MedicationsScreen(
    medications: List<Medication>,
    onEdit: (Medication) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    onAddCourse: () -> Unit = {},
) {
    Box(modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 168.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ScreenHeader(
                    eyebrow = pluralStringResource(Res.plurals.medications_count, medications.size, medications.size),
                    title = stringResource(Res.string.nav_medications),
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
            if (medications.isEmpty()) {
                item {
                    Text(
                        stringResource(Res.string.empty_body),
                        style = TempusTheme.type.bodyText,
                        color = TempusTheme.colors.inkSoft,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
            items(medications, key = { it.id }) { medication ->
                MedicationRow(medication, onClick = { onEdit(medication) })
            }
        }
        Column(
            Modifier.align(Alignment.BottomCenter).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SoftButton(text = "+  " + stringResource(Res.string.action_add_course), onClick = onAddCourse)
            BrandButton(text = "+  " + stringResource(Res.string.action_add_medication), onClick = onAdd)
        }
    }
}

@Composable
private fun MedicationRow(
    medication: Medication,
    onClick: () -> Unit,
) {
    val palette = TempusTheme.colors
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .glass(shape, palette, elevation = 6.dp)
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PillChip(medication)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(medication.displayName, style = TempusTheme.type.rowTitle, color = palette.ink)
            Text(scheduleSummary(medication), style = TempusTheme.type.bodySmall, color = palette.inkMute)
        }
        medication.stock?.let { stock ->
            Spacer(Modifier.width(8.dp))
            val low = Inventory.isLow(medication)
            Text(
                pluralStringResource(Res.plurals.units_left, medication.stockTablets ?: 0, medication.stockTablets ?: 0),
                style = TempusTheme.type.bodySmall,
                color = if (low) palette.stock else palette.inkMute,
                modifier =
                    Modifier
                        .then(if (low) Modifier.background(palette.stockSoft, RoundedCornerShape(50)) else Modifier)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

/** "Manhã, Deitar · Todos os dias" */
@Composable
private fun scheduleSummary(medication: Medication): String {
    medication.course?.let { course ->
        // "De 8 em 8 horas · 6 mL"
        return stringResource(Res.string.course_schedule, intervalLabel(course.intervalHours), course.doseLabel())
    }
    val slots =
        TimeSlot.entries
            .filter { (medication.schedule.doses[it] ?: 0) > 0 }
            .map { slot ->
                val quantity = medication.schedule.doses[slot] ?: 0
                val name = stringResource(slot.label)
                if (quantity > 1) "$name ×$quantity" else name
            }
    val frequency = scheduleLabel(medication.schedule)
    return slots.joinToString(", ") + " · " + frequency
}
