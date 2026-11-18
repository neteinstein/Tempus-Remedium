package org.mariacf.tempusremedium.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.mariacf.tempusremedium.core.model.Medication
import org.mariacf.tempusremedium.ui.PlatformBackHandler
import org.mariacf.tempusremedium.ui.components.Eyebrow
import org.mariacf.tempusremedium.ui.components.brandBackground
import org.mariacf.tempusremedium.ui.resources.Res
import org.mariacf.tempusremedium.ui.resources.action_back
import org.mariacf.tempusremedium.ui.resources.action_restore_selected
import org.mariacf.tempusremedium.ui.resources.archive_count
import org.mariacf.tempusremedium.ui.resources.archive_intro
import org.mariacf.tempusremedium.ui.resources.archive_title
import org.mariacf.tempusremedium.ui.resources.medications_select
import org.mariacf.tempusremedium.ui.resources.symptoms_selected
import org.mariacf.tempusremedium.ui.theme.TempusTheme

/**
 * Arquivo: medications taken off Medicamentos. They ring no reminder and raise no alert; their
 * earlier doses stay in Histórico. Selecting them (the pencil) reactivates or deletes them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    medications: List<Medication>,
    onEdit: (Medication) -> Unit,
    onRestore: (Set<String>) -> Unit,
    onDelete: (Set<String>) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    /** Opens in selection mode (screenshots). */
    initialSelection: Set<String>? = null,
) {
    val selection = rememberMedicationSelection(medications, initialSelection)
    // Registered first, so that while selecting the selection's own back handler wins.
    PlatformBackHandler(enabled = !selection.selecting, onBack = onClose)
    Column(modifier.fillMaxSize().brandBackground(TempusTheme.colors)) {
        TopAppBar(
            title = {
                Text(
                    stringResource(Res.string.archive_title),
                    style = TempusTheme.type.title.copy(fontSize = 21.sp),
                    color = TempusTheme.colors.ink,
                    maxLines = 1,
                )
            },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.action_back))
                }
            },
            actions = { SelectionActions(selection, editDescription = stringResource(Res.string.medications_select)) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        )
        Box(Modifier.weight(1f)) {
            LazyColumn(
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "intro") {
                    Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Eyebrow(
                            if (selection.selecting) {
                                pluralStringResource(Res.plurals.symptoms_selected, selection.selected.size, selection.selected.size)
                            } else {
                                pluralStringResource(Res.plurals.archive_count, medications.size, medications.size)
                            },
                        )
                        Text(
                            stringResource(Res.string.archive_intro),
                            style = TempusTheme.type.bodyText,
                            color = TempusTheme.colors.inkSoft,
                        )
                    }
                }
                medicationItems(medications, selection, onEdit)
            }
            if (selection.selecting) {
                SelectionButtons(
                    selection = selection,
                    primary = stringResource(Res.string.action_restore_selected, selection.selected.size),
                    onPrimary = {
                        onRestore(selection.selected)
                        selection.stop()
                    },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
                )
            }
        }
    }
    DeleteMedicationsDialog(selection, onDelete)
}
