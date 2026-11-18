package org.mariacf.tempusremedium.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.mariacf.tempusremedium.core.model.Medication
import org.mariacf.tempusremedium.core.model.TimeSlot
import org.mariacf.tempusremedium.core.schedule.Inventory
import org.mariacf.tempusremedium.ui.PlatformBackHandler
import org.mariacf.tempusremedium.ui.components.ArchiveIcons
import org.mariacf.tempusremedium.ui.components.BrandButton
import org.mariacf.tempusremedium.ui.components.GroupCard
import org.mariacf.tempusremedium.ui.components.ScreenHeader
import org.mariacf.tempusremedium.ui.components.doseLabel
import org.mariacf.tempusremedium.ui.components.glass
import org.mariacf.tempusremedium.ui.components.intervalLabel
import org.mariacf.tempusremedium.ui.components.label
import org.mariacf.tempusremedium.ui.components.scheduleLabel
import org.mariacf.tempusremedium.ui.resources.Res
import org.mariacf.tempusremedium.ui.resources.action_add_course
import org.mariacf.tempusremedium.ui.resources.action_add_medication
import org.mariacf.tempusremedium.ui.resources.action_archive_selected
import org.mariacf.tempusremedium.ui.resources.action_cancel
import org.mariacf.tempusremedium.ui.resources.action_delete
import org.mariacf.tempusremedium.ui.resources.action_delete_selected
import org.mariacf.tempusremedium.ui.resources.archive_open
import org.mariacf.tempusremedium.ui.resources.course_schedule
import org.mariacf.tempusremedium.ui.resources.empty_body
import org.mariacf.tempusremedium.ui.resources.medications_count
import org.mariacf.tempusremedium.ui.resources.medications_delete_confirm
import org.mariacf.tempusremedium.ui.resources.medications_select
import org.mariacf.tempusremedium.ui.resources.nav_medications
import org.mariacf.tempusremedium.ui.resources.symptoms_select_all
import org.mariacf.tempusremedium.ui.resources.symptoms_selected
import org.mariacf.tempusremedium.ui.resources.units_left
import org.mariacf.tempusremedium.ui.theme.TempusTheme

/**
 * Medicamentos: the medications in use. The pencil at the top right turns the list into a
 * selection, to archive or delete one, several or all; the box next to it opens the archive and
 * only shows while something is archived.
 */
@Composable
fun MedicationsScreen(
    medications: List<Medication>,
    onEdit: (Medication) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    onAddCourse: () -> Unit = {},
    archivedCount: Int = 0,
    onOpenArchive: () -> Unit = {},
    onArchive: (Set<String>) -> Unit = {},
    onDelete: (Set<String>) -> Unit = {},
    /** Opens in selection mode (screenshots). */
    initialSelection: Set<String>? = null,
) {
    val selection = rememberMedicationSelection(medications, initialSelection)
    Box(modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 168.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                // The buttons sit on the eyebrow line, so that the title keeps the full width.
                Box(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    ScreenHeader(
                        eyebrow =
                            if (selection.selecting) {
                                pluralStringResource(Res.plurals.symptoms_selected, selection.selected.size, selection.selected.size)
                            } else {
                                pluralStringResource(Res.plurals.medications_count, medications.size, medications.size)
                            },
                        title = stringResource(Res.string.nav_medications),
                    )
                    Row(Modifier.align(Alignment.TopEnd).offset(y = (-12).dp), verticalAlignment = Alignment.CenterVertically) {
                        SelectionActions(selection, editDescription = stringResource(Res.string.medications_select)) {
                            if (archivedCount > 0) {
                                IconButton(onClick = onOpenArchive) {
                                    Icon(
                                        ArchiveIcons.Archive,
                                        contentDescription = stringResource(Res.string.archive_open),
                                        tint = TempusTheme.colors.inkSoft,
                                    )
                                }
                            }
                        }
                    }
                }
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
            medicationItems(medications, selection, onEdit)
        }
        val bottom = Modifier.align(Alignment.BottomCenter).padding(24.dp)
        if (selection.selecting) {
            SelectionButtons(
                selection = selection,
                primary = stringResource(Res.string.action_archive_selected, selection.selected.size),
                onPrimary = {
                    onArchive(selection.selected)
                    selection.stop()
                },
                modifier = bottom,
            )
        } else {
            Column(
                bottom,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BrandButton(text = "+  " + stringResource(Res.string.action_add_course), onClick = onAddCourse)
                BrandButton(text = "+  " + stringResource(Res.string.action_add_medication), onClick = onAdd)
            }
        }
    }
    DeleteMedicationsDialog(selection, onDelete)
}

/** Which medications of a list are selected, and whether the list is selecting at all. */
@Stable
internal class MedicationSelection(
    initial: Set<String>?,
) {
    var selecting by mutableStateOf(initial != null)
    var selected by mutableStateOf(initial.orEmpty())
    var confirmDelete by mutableStateOf(false)
    var ids: Set<String> = emptySet()

    fun toggle(id: String) {
        selected = if (id in selected) selected - id else selected + id
    }

    fun stop() {
        selecting = false
        selected = emptySet()
        confirmDelete = false
    }
}

@Composable
internal fun rememberMedicationSelection(
    medications: List<Medication>,
    initial: Set<String>?,
): MedicationSelection {
    val selection = remember { MedicationSelection(initial) }
    val ids = remember(medications) { medications.map { it.id }.toSet() }
    selection.ids = ids
    // Medications that leave the list leave the selection; with none left there is nothing to select.
    LaunchedEffect(ids) {
        selection.selected = selection.selected intersect ids
        if (ids.isEmpty()) selection.stop()
    }
    PlatformBackHandler(enabled = selection.selecting, onBack = selection::stop)
    return selection
}

/** The pencil that starts selecting (after [extra]), or "Cancelar" while selecting. */
@Composable
internal fun SelectionActions(
    selection: MedicationSelection,
    editDescription: String,
    extra: @Composable () -> Unit = {},
) {
    if (selection.selecting) {
        TextButton(onClick = selection::stop) {
            Text(stringResource(Res.string.action_cancel), style = TempusTheme.type.button, color = TempusTheme.colors.accent)
        }
    } else {
        extra()
        if (selection.ids.isNotEmpty()) {
            IconButton(onClick = { selection.selecting = true }) {
                Icon(Icons.Filled.Edit, contentDescription = editDescription, tint = TempusTheme.colors.inkSoft)
            }
        }
    }
}

/** "Selecionar todos" while selecting, then one row per medication. */
internal fun LazyListScope.medicationItems(
    medications: List<Medication>,
    selection: MedicationSelection,
    onEdit: (Medication) -> Unit,
) {
    if (selection.selecting) {
        item(key = "select-all") {
            GroupCard {
                val all = selection.selected.size == selection.ids.size
                SelectableRow(checked = all, onToggle = { selection.selected = if (all) emptySet() else selection.ids }) {
                    Text(stringResource(Res.string.symptoms_select_all), style = TempusTheme.type.rowTitle, color = TempusTheme.colors.ink)
                }
            }
        }
    }
    items(medications, key = { it.id }) { medication ->
        if (selection.selecting) {
            GroupCard {
                SelectableRow(checked = medication.id in selection.selected, onToggle = { selection.toggle(medication.id) }) {
                    MedicationLine(medication)
                }
            }
        } else {
            MedicationRow(medication, onClick = { onEdit(medication) })
        }
    }
}

/** While selecting: the [primary] action (archive, restore) and delete, both for the selection. */
@Composable
internal fun SelectionButtons(
    selection: MedicationSelection,
    primary: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = selection.selected.isNotEmpty()
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        BrandButton(text = primary, onClick = onPrimary, enabled = enabled)
        DangerButton(
            text = stringResource(Res.string.action_delete_selected, selection.selected.size),
            enabled = enabled,
            // Deleting asks first, in DeleteMedicationsDialog.
            onClick = { selection.confirmDelete = true },
        )
    }
}

@Composable
internal fun DeleteMedicationsDialog(
    selection: MedicationSelection,
    onDelete: (Set<String>) -> Unit,
) {
    if (!selection.confirmDelete) return
    val count = selection.selected.size
    AlertDialog(
        onDismissRequest = { selection.confirmDelete = false },
        text = { Text(pluralStringResource(Res.plurals.medications_delete_confirm, count, count)) },
        confirmButton = {
            Button(
                onClick = {
                    onDelete(selection.selected)
                    selection.stop()
                },
                colors = ButtonDefaults.buttonColors(containerColor = TempusTheme.colors.danger),
            ) { Text(stringResource(Res.string.action_delete)) }
        },
        dismissButton = {
            TextButton(onClick = { selection.confirmDelete = false }) { Text(stringResource(Res.string.action_cancel)) }
        },
    )
}

@Composable
private fun MedicationRow(
    medication: Medication,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .glass(shape, TempusTheme.colors, elevation = 6.dp)
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { MedicationLine(medication) }
}

/** Pill, name, schedule and units left. */
@Composable
private fun RowScope.MedicationLine(medication: Medication) {
    val palette = TempusTheme.colors
    PillChip(medication)
    Spacer(Modifier.width(14.dp))
    Column(Modifier.weight(1f)) {
        Text(medication.displayName, style = TempusTheme.type.rowTitle, color = palette.ink)
        Text(scheduleSummary(medication), style = TempusTheme.type.bodySmall, color = palette.inkMute)
    }
    medication.stock?.let {
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

/** "Manhã, Deitar · Todos os dias" */
@Composable
private fun scheduleSummary(medication: Medication): String {
    medication.course?.let { course ->
        // "De 8 em 8 horas · 6 mL", or the interval alone without an amount.
        val interval = intervalLabel(course.intervalHours)
        val dose = course.doseLabel() ?: return interval
        return stringResource(Res.string.course_schedule, interval, dose)
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
