package org.mariacf.tempusremedium.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.mariacf.tempusremedium.core.data.MEDICATIONS_FILE_NAME
import org.mariacf.tempusremedium.core.data.MEDICATIONS_FILE_TYPE
import org.mariacf.tempusremedium.core.data.MEDICATIONS_PDF_NAME
import org.mariacf.tempusremedium.core.data.exportMedications
import org.mariacf.tempusremedium.core.data.parseMedicationsFile
import org.mariacf.tempusremedium.core.model.Medication
import org.mariacf.tempusremedium.core.model.Settings
import org.mariacf.tempusremedium.ui.FileTransfer
import org.mariacf.tempusremedium.ui.components.BrandButton
import org.mariacf.tempusremedium.ui.components.GroupCard
import org.mariacf.tempusremedium.ui.components.GroupDivider
import org.mariacf.tempusremedium.ui.pill.PillIcon
import org.mariacf.tempusremedium.ui.resources.Res
import org.mariacf.tempusremedium.ui.resources.action_cancel
import org.mariacf.tempusremedium.ui.resources.action_export
import org.mariacf.tempusremedium.ui.resources.action_import
import org.mariacf.tempusremedium.ui.resources.action_ok
import org.mariacf.tempusremedium.ui.resources.export_file
import org.mariacf.tempusremedium.ui.resources.export_file_body
import org.mariacf.tempusremedium.ui.resources.export_format
import org.mariacf.tempusremedium.ui.resources.export_pdf
import org.mariacf.tempusremedium.ui.resources.export_pdf_body
import org.mariacf.tempusremedium.ui.resources.import_done
import org.mariacf.tempusremedium.ui.resources.import_invalid
import org.mariacf.tempusremedium.ui.resources.settings_export
import org.mariacf.tempusremedium.ui.resources.settings_export_body
import org.mariacf.tempusremedium.ui.resources.settings_import
import org.mariacf.tempusremedium.ui.resources.settings_import_body
import org.mariacf.tempusremedium.ui.resources.settings_transfer
import org.mariacf.tempusremedium.ui.resources.transfer_all
import org.mariacf.tempusremedium.ui.resources.transfer_which
import org.mariacf.tempusremedium.ui.sheet.sheetPages
import org.mariacf.tempusremedium.ui.sheet.sheetStyle
import org.mariacf.tempusremedium.ui.sheet.sheetTexts
import org.mariacf.tempusremedium.ui.theme.TempusTheme

/** What "Exportar" makes. */
enum class ExportFormat {
    /** The medications sheet, to read and print. */
    PDF,

    /** A file another phone imports. */
    FILE,
}

/** Exportar e importar: medications to a PDF or a file, and back from a file. */
@Composable
fun MedicationTransferSection(
    medications: List<Medication>,
    settings: Settings,
    today: LocalDate,
    files: FileTransfer,
    onImport: (List<Medication>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var exporting by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf<List<Medication>?>(null) }
    // After an import: how many came in, or 0 when the file was not an export.
    var imported by remember { mutableStateOf<Int?>(null) }
    var pdfOf by remember { mutableStateOf<List<Medication>?>(null) }

    SettingsSection(stringResource(Res.string.settings_transfer), modifier) {
        GroupCard {
            SettingsRow(
                title = stringResource(Res.string.settings_export),
                body = stringResource(Res.string.settings_export_body),
                value = if (medications.isEmpty()) "" else "›",
                onClick = if (medications.isEmpty()) null else ({ exporting = true }),
            )
            GroupDivider()
            SettingsRow(
                title = stringResource(Res.string.settings_import),
                body = stringResource(Res.string.settings_import_body),
                value = "›",
                onClick = {
                    // Undispatched: the web's file picker only opens within the tap itself.
                    scope.launch(start = CoroutineStart.UNDISPATCHED) {
                        val text = files.open() ?: return@launch
                        val found = parseMedicationsFile(text)
                        if (found.isNullOrEmpty()) imported = 0 else importing = found
                    }
                },
            )
        }
    }

    if (exporting) {
        ExportMedicationsDialog(
            medications = medications,
            onDismiss = { exporting = false },
            onExport = { format, chosen ->
                exporting = false
                when (format) {
                    ExportFormat.PDF -> {
                        pdfOf = chosen
                    }

                    ExportFormat.FILE -> {
                        scope.launch {
                            files.save(MEDICATIONS_FILE_NAME, MEDICATIONS_FILE_TYPE, exportMedications(chosen).encodeToByteArray())
                        }
                    }
                }
            },
        )
    }

    importing?.let { found ->
        ImportMedicationsDialog(
            medications = found,
            onDismiss = { importing = null },
            onConfirm = { chosen ->
                importing = null
                onImport(chosen)
                imported = chosen.size
            },
        )
    }

    pdfOf?.let { chosen ->
        // Texts resolve here, in the app's language; the pages are laid out and saved off the frame.
        val texts by rememberUpdatedState(sheetTexts(chosen, settings, today))
        val style = sheetStyle()
        val measurer = rememberTextMeasurer()
        LaunchedEffect(chosen) {
            try {
                val ready = snapshotFlow { texts }.first { it.isResolved }
                files.save(MEDICATIONS_PDF_NAME, "application/pdf", files.pdf(sheetPages(ready, style, measurer)))
            } finally {
                pdfOf = null
            }
        }
    }

    imported?.let { count ->
        val text =
            if (count == 0) stringResource(Res.string.import_invalid) else pluralStringResource(Res.plurals.import_done, count, count)
        AlertDialog(
            onDismissRequest = { imported = null },
            containerColor = TempusTheme.colors.background,
            text = { Text(text, style = TempusTheme.type.bodyText, color = TempusTheme.colors.inkSoft) },
            confirmButton = { BrandButton(stringResource(Res.string.action_ok), onClick = { imported = null }) },
        )
    }
}

/** PDF or file, of all the medications or the ones ticked. */
@Composable
fun ExportMedicationsDialog(
    medications: List<Medication>,
    onDismiss: () -> Unit,
    onExport: (ExportFormat, List<Medication>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var format by remember { mutableStateOf(ExportFormat.PDF) }
    var chosen by remember(medications) { mutableStateOf(medications.map { it.id }.toSet()) }
    TransferDialog(
        title = stringResource(Res.string.settings_export),
        confirm = stringResource(Res.string.action_export),
        confirmEnabled = chosen.isNotEmpty(),
        onDismiss = onDismiss,
        onConfirm = { onExport(format, medications.filter { it.id in chosen }) },
        modifier = modifier,
    ) {
        DialogLabel(stringResource(Res.string.export_format))
        FormatOption(
            title = stringResource(Res.string.export_pdf),
            body = stringResource(Res.string.export_pdf_body),
            selected = format == ExportFormat.PDF,
            onClick = { format = ExportFormat.PDF },
        )
        FormatOption(
            title = stringResource(Res.string.export_file),
            body = stringResource(Res.string.export_file_body),
            selected = format == ExportFormat.FILE,
            onClick = { format = ExportFormat.FILE },
        )
        MedicationChoice(medications, chosen, onChange = { chosen = it })
    }
}

/** The medications of an imported file, all or some of them. */
@Composable
fun ImportMedicationsDialog(
    medications: List<Medication>,
    onDismiss: () -> Unit,
    onConfirm: (List<Medication>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var chosen by remember(medications) { mutableStateOf(medications.map { it.id }.toSet()) }
    TransferDialog(
        title = stringResource(Res.string.settings_import),
        confirm = stringResource(Res.string.action_import),
        confirmEnabled = chosen.isNotEmpty(),
        onDismiss = onDismiss,
        onConfirm = { onConfirm(medications.filter { it.id in chosen }) },
        modifier = modifier,
    ) {
        MedicationChoice(medications, chosen, onChange = { chosen = it })
    }
}

@Composable
private fun TransferDialog(
    title: String,
    confirm: String,
    confirmEnabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = TempusTheme.colors.background,
        title = { Text(title, style = TempusTheme.type.title, color = TempusTheme.colors.ink) },
        text = {
            Column(
                Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) { content() }
        },
        confirmButton = { BrandButton(confirm, onClick = onConfirm, enabled = confirmEnabled) },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel), style = TempusTheme.type.button, color = TempusTheme.colors.inkSoft)
            }
        },
    )
}

@Composable
private fun DialogLabel(text: String) {
    Text(text.uppercase(), style = TempusTheme.type.eyebrow, color = TempusTheme.colors.inkMute, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun FormatOption(
    title: String,
    body: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = TempusTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(if (selected) 2.dp else 1.dp, if (selected) palette.accent else palette.line, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = palette.accent))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = TempusTheme.type.rowTitle, color = if (selected) palette.accent else palette.ink)
            Text(body, style = TempusTheme.type.bodySmall, color = palette.inkMute)
        }
    }
}

/** "Todos" and a box per medication when there is more than one; the single one otherwise. */
@Composable
private fun MedicationChoice(
    medications: List<Medication>,
    chosen: Set<String>,
    onChange: (Set<String>) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DialogLabel(stringResource(Res.string.transfer_which))
        if (medications.size == 1) {
            MedicationLine(medications.single(), checked = null, onToggle = {})
        } else {
            val all = chosen.size == medications.size
            CheckLine(
                checked = all,
                onToggle = { onChange(if (all) emptySet() else medications.map { it.id }.toSet()) },
            ) {
                Text(stringResource(Res.string.transfer_all), style = TempusTheme.type.rowTitle, color = TempusTheme.colors.ink)
            }
            GroupDivider()
            medications.forEach { medication ->
                val checked = medication.id in chosen
                MedicationLine(
                    medication,
                    checked = checked,
                    onToggle = { onChange(if (checked) chosen - medication.id else chosen + medication.id) },
                )
            }
        }
    }
}

@Composable
private fun MedicationLine(
    medication: Medication,
    checked: Boolean?,
    onToggle: () -> Unit,
) {
    CheckLine(checked = checked, onToggle = onToggle) {
        PillIcon(medication.shape, medication.color, height = 22.dp)
        Spacer(Modifier.width(10.dp))
        Text(medication.displayName, style = TempusTheme.type.rowTitle, color = TempusTheme.colors.ink)
    }
}

/** A row with a checkbox, or without one when [checked] is null. */
@Composable
private fun CheckLine(
    checked: Boolean?,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    val palette = TempusTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (checked !=
                    null
                ) {
                    Modifier.toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
                } else {
                    Modifier
                },
            ).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (checked != null) {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                colors =
                    CheckboxDefaults.colors(
                        checkedColor = palette.accent,
                        uncheckedColor = palette.inkMute,
                        checkmarkColor = Color.White,
                    ),
            )
            Spacer(Modifier.width(10.dp))
        }
        content()
    }
}
