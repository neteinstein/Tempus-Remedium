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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.mariacf.tempusremedium.core.model.Symptom
import org.mariacf.tempusremedium.core.schedule.newestFirstByDay
import org.mariacf.tempusremedium.ui.PhotoStore
import org.mariacf.tempusremedium.ui.PlatformBackHandler
import org.mariacf.tempusremedium.ui.components.BrandButton
import org.mariacf.tempusremedium.ui.components.GroupCard
import org.mariacf.tempusremedium.ui.components.GroupDivider
import org.mariacf.tempusremedium.ui.components.ScreenHeader
import org.mariacf.tempusremedium.ui.components.SymptomPhoto
import org.mariacf.tempusremedium.ui.components.hhmm
import org.mariacf.tempusremedium.ui.components.longDateLabel
import org.mariacf.tempusremedium.ui.resources.Res
import org.mariacf.tempusremedium.ui.resources.action_add_symptom
import org.mariacf.tempusremedium.ui.resources.action_cancel
import org.mariacf.tempusremedium.ui.resources.action_delete_selected
import org.mariacf.tempusremedium.ui.resources.nav_symptoms
import org.mariacf.tempusremedium.ui.resources.symptom_photo_description
import org.mariacf.tempusremedium.ui.resources.symptoms_count
import org.mariacf.tempusremedium.ui.resources.symptoms_empty
import org.mariacf.tempusremedium.ui.resources.symptoms_select
import org.mariacf.tempusremedium.ui.resources.symptoms_select_all
import org.mariacf.tempusremedium.ui.resources.symptoms_selected
import org.mariacf.tempusremedium.ui.theme.TempusTheme

/**
 * Sintomas: what the user felt and when, the most recent first. Tapping one edits it; the bin at
 * the top right turns the list into a selection to delete several at once.
 */
@Composable
fun SymptomsScreen(
    symptoms: List<Symptom>,
    onAdd: () -> Unit,
    onEdit: (Symptom) -> Unit,
    onDelete: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
    photos: PhotoStore? = null,
    /** Opens in selection mode (screenshots). */
    initialSelection: Set<String>? = null,
) {
    var selecting by rememberSaveable { mutableStateOf(initialSelection != null) }
    var selected by remember { mutableStateOf(initialSelection.orEmpty()) }
    var confirmDelete by remember { mutableStateOf(false) }
    val days = remember(symptoms) { symptoms.newestFirstByDay() }
    val ids = remember(symptoms) { symptoms.map { it.id }.toSet() }

    fun stopSelecting() {
        selecting = false
        selected = emptySet()
    }

    // Symptoms deleted elsewhere leave the selection; with none left there is nothing to select.
    LaunchedEffect(ids) {
        selected = selected intersect ids
        if (ids.isEmpty()) stopSelecting()
    }
    PlatformBackHandler(enabled = selecting, onBack = ::stopSelecting)

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(Modifier.padding(bottom = 12.dp), verticalAlignment = Alignment.Top) {
                    ScreenHeader(
                        eyebrow =
                            if (selecting) {
                                pluralStringResource(Res.plurals.symptoms_selected, selected.size, selected.size)
                            } else {
                                pluralStringResource(Res.plurals.symptoms_count, symptoms.size, symptoms.size)
                            },
                        title = stringResource(Res.string.nav_symptoms),
                        modifier = Modifier.weight(1f),
                    )
                    if (selecting) {
                        TextButton(onClick = ::stopSelecting) {
                            Text(
                                stringResource(Res.string.action_cancel),
                                style = TempusTheme.type.button,
                                color = TempusTheme.colors.accent,
                            )
                        }
                    } else if (symptoms.isNotEmpty()) {
                        IconButton(onClick = { selecting = true }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(Res.string.symptoms_select),
                                tint = TempusTheme.colors.inkSoft,
                            )
                        }
                    }
                }
            }
            if (symptoms.isEmpty()) {
                item {
                    Text(
                        stringResource(Res.string.symptoms_empty),
                        style = TempusTheme.type.bodyText,
                        color = TempusTheme.colors.inkSoft,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
            if (selecting) {
                item {
                    GroupCard {
                        val all = selected.size == ids.size
                        SelectableRow(
                            checked = all,
                            onToggle = { selected = if (all) emptySet() else ids },
                        ) {
                            Text(
                                stringResource(Res.string.symptoms_select_all),
                                style = TempusTheme.type.rowTitle,
                                color = TempusTheme.colors.ink,
                            )
                        }
                    }
                }
            }
            items(days, key = { (date, _) -> date.toString() }) { (date, entries) ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        longDateLabel(date).uppercase(),
                        style = TempusTheme.type.eyebrow,
                        color = TempusTheme.colors.inkMute,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                    )
                    GroupCard {
                        entries.forEachIndexed { index, symptom ->
                            if (index > 0) GroupDivider()
                            if (selecting) {
                                val checked = symptom.id in selected
                                SelectableRow(
                                    checked = checked,
                                    onToggle = { selected = if (checked) selected - symptom.id else selected + symptom.id },
                                ) { SymptomLine(symptom, photos) }
                            } else {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { onEdit(symptom) }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) { SymptomLine(symptom, photos) }
                            }
                        }
                    }
                }
            }
        }
        val bottom = Modifier.align(Alignment.BottomCenter).padding(24.dp)
        if (selecting) {
            DangerButton(
                text = stringResource(Res.string.action_delete_selected, selected.size),
                enabled = selected.isNotEmpty(),
                onClick = { confirmDelete = true },
                modifier = bottom,
            )
        } else {
            BrandButton(text = "+  " + stringResource(Res.string.action_add_symptom), onClick = onAdd, modifier = bottom)
        }
    }

    if (confirmDelete) {
        DeleteSymptomsDialog(
            count = selected.size,
            withPhotos = symptoms.any { it.id in selected && it.photo != null },
            onConfirm = {
                confirmDelete = false
                onDelete(selected)
                stopSelecting()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

/** "14:30  Dor de cabeça", with the photo's thumbnail at the end. */
@Composable
private fun RowScope.SymptomLine(
    symptom: Symptom,
    photos: PhotoStore?,
) {
    val palette = TempusTheme.colors
    Text(symptom.at.time.hhmm(), style = TempusTheme.type.rowTitle, color = palette.accent, modifier = Modifier.width(56.dp))
    Spacer(Modifier.width(8.dp))
    Text(symptom.name, style = TempusTheme.type.rowTitle, color = palette.ink, modifier = Modifier.weight(1f))
    val photo = symptom.photo
    if (photo != null && photos != null) {
        Spacer(Modifier.width(8.dp))
        SymptomPhoto(
            id = photo,
            photos = photos,
            contentDescription = stringResource(Res.string.symptom_photo_description, symptom.name),
            modifier = Modifier.size(40.dp),
            corner = 10.dp,
        )
    }
}

@Composable
internal fun SelectableRow(
    checked: Boolean,
    onToggle: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    val palette = TempusTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier.padding(12.dp),
            colors = CheckboxDefaults.colors(checkedColor = palette.accent, uncheckedColor = palette.inkMute, checkmarkColor = Color.White),
        )
        content()
    }
}

/** A pill-shaped button in the danger red, for deleting. */
@Composable
internal fun DangerButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .alpha(if (enabled) 1f else 0.45f)
            .clip(CircleShape)
            .background(TempusTheme.colors.danger)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = TempusTheme.type.button, color = Color.White, maxLines = 1)
    }
}
