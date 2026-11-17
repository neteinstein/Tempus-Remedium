package org.mariacf.tempusremedium.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.mariacf.tempusremedium.core.input.FieldInput
import org.mariacf.tempusremedium.core.model.Symptom
import org.mariacf.tempusremedium.core.model.newSymptomId
import org.mariacf.tempusremedium.core.schedule.newestFirstByDay
import org.mariacf.tempusremedium.ui.components.BrandButton
import org.mariacf.tempusremedium.ui.components.GroupCard
import org.mariacf.tempusremedium.ui.components.GroupDivider
import org.mariacf.tempusremedium.ui.components.ScreenHeader
import org.mariacf.tempusremedium.ui.components.dayLabel
import org.mariacf.tempusremedium.ui.components.hhmm
import org.mariacf.tempusremedium.ui.components.longDateLabel
import org.mariacf.tempusremedium.ui.resources.Res
import org.mariacf.tempusremedium.ui.resources.action_add_symptom
import org.mariacf.tempusremedium.ui.resources.action_cancel
import org.mariacf.tempusremedium.ui.resources.action_delete
import org.mariacf.tempusremedium.ui.resources.action_save
import org.mariacf.tempusremedium.ui.resources.day_today
import org.mariacf.tempusremedium.ui.resources.day_yesterday
import org.mariacf.tempusremedium.ui.resources.field_time
import org.mariacf.tempusremedium.ui.resources.nav_symptoms
import org.mariacf.tempusremedium.ui.resources.symptom_diarrhoea
import org.mariacf.tempusremedium.ui.resources.symptom_dizziness
import org.mariacf.tempusremedium.ui.resources.symptom_drowsiness
import org.mariacf.tempusremedium.ui.resources.symptom_edit
import org.mariacf.tempusremedium.ui.resources.symptom_fever
import org.mariacf.tempusremedium.ui.resources.symptom_field_name
import org.mariacf.tempusremedium.ui.resources.symptom_headache
import org.mariacf.tempusremedium.ui.resources.symptom_itching
import org.mariacf.tempusremedium.ui.resources.symptom_nausea
import org.mariacf.tempusremedium.ui.resources.symptom_new
import org.mariacf.tempusremedium.ui.resources.symptom_stomach_ache
import org.mariacf.tempusremedium.ui.resources.symptom_vomiting
import org.mariacf.tempusremedium.ui.resources.symptom_when
import org.mariacf.tempusremedium.ui.resources.symptoms_count
import org.mariacf.tempusremedium.ui.resources.symptoms_empty
import org.mariacf.tempusremedium.ui.theme.TempusTheme

/** Common symptoms offered as chips; the name stays free text. */
private val SuggestedSymptoms: List<StringResource> =
    listOf(
        Res.string.symptom_nausea,
        Res.string.symptom_vomiting,
        Res.string.symptom_dizziness,
        Res.string.symptom_headache,
        Res.string.symptom_stomach_ache,
        Res.string.symptom_diarrhoea,
        Res.string.symptom_drowsiness,
        Res.string.symptom_fever,
        Res.string.symptom_itching,
    )

/** Sintomas: what the user felt and when, the most recent first. Tapping one edits it. */
@Composable
fun SymptomsScreen(
    symptoms: List<Symptom>,
    now: LocalDateTime,
    onSave: (Symptom) -> Unit,
    onDelete: (Symptom) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The symptom in the dialog; one whose id is not in [symptoms] is being added.
    var editing by remember { mutableStateOf<Symptom?>(null) }
    val days = remember(symptoms) { symptoms.newestFirstByDay() }
    Box(modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ScreenHeader(
                    eyebrow = pluralStringResource(Res.plurals.symptoms_count, symptoms.size, symptoms.size),
                    title = stringResource(Res.string.nav_symptoms),
                    modifier = Modifier.padding(bottom = 12.dp),
                )
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
                            SymptomRow(symptom, onClick = { editing = symptom })
                        }
                    }
                }
            }
        }
        BrandButton(
            text = "+  " + stringResource(Res.string.action_add_symptom),
            onClick = { editing = Symptom(newSymptomId(), name = "", at = LocalDateTime(now.date, LocalTime(now.hour, now.minute))) },
            modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
        )
    }
    editing?.let { symptom ->
        val isNew = symptoms.none { it.id == symptom.id }
        SymptomDialog(
            symptom = symptom,
            isNew = isNew,
            today = now.date,
            onDismiss = { editing = null },
            onSave = {
                onSave(it)
                editing = null
            },
            onDelete =
                if (isNew) {
                    null
                } else {
                    {
                        onDelete(symptom)
                        editing = null
                    }
                },
        )
    }
}

@Composable
private fun SymptomRow(
    symptom: Symptom,
    onClick: () -> Unit,
) {
    val palette = TempusTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(symptom.at.time.hhmm(), style = TempusTheme.type.rowTitle, color = palette.accent, modifier = Modifier.width(56.dp))
        Spacer(Modifier.width(8.dp))
        Text(symptom.name, style = TempusTheme.type.rowTitle, color = palette.ink, modifier = Modifier.weight(1f))
    }
}

/** Name (typed or picked from the common ones), day and time of a symptom. */
@Composable
private fun SymptomDialog(
    symptom: Symptom,
    isNew: Boolean,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSave: (Symptom) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by remember { mutableStateOf(symptom.name) }
    var date by remember { mutableStateOf(symptom.at.date) }
    var time by remember { mutableStateOf(FieldInput.timeToDigits(symptom.at.time)) }
    val yesterday = today.minus(DatePeriod(days = 1))
    val parsedTime = FieldInput.parseTimeDigits(time)
    val canSave = name.isNotBlank() && parsedTime != null
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TempusTheme.colors.background,
        title = { Text(stringResource(if (isNew) Res.string.symptom_new else Res.string.symptom_edit), style = TempusTheme.type.title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(Res.string.symptom_field_name)) },
                    singleLine = true,
                    shape = FieldShape,
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SuggestedSymptoms.forEach { suggestion ->
                        val label = stringResource(suggestion)
                        FrequencyChip(label, selected = name.trim() == label) { name = label }
                    }
                }
                FieldTitle(stringResource(Res.string.symptom_when))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FrequencyChip(stringResource(Res.string.day_today), date == today) { date = today }
                    FrequencyChip(stringResource(Res.string.day_yesterday), date == yesterday) { date = yesterday }
                    // An older symptom keeps its own day as a third choice.
                    val original = symptom.at.date
                    if (original != today && original != yesterday) {
                        FrequencyChip(dayLabel(original), date == original) { date = original }
                    }
                }
                TimeField(
                    digits = time,
                    onDigitsChange = { time = it },
                    label = stringResource(Res.string.field_time),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            BrandButton(
                stringResource(Res.string.action_save),
                onClick = { parsedTime?.let { onSave(symptom.copy(name = name.trim(), at = LocalDateTime(date, it))) } },
                enabled = canSave,
            )
        },
        dismissButton = {
            Row {
                onDelete?.let {
                    TextButton(onClick = it) {
                        Text(stringResource(Res.string.action_delete), style = TempusTheme.type.button, color = TempusTheme.colors.danger)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.action_cancel), style = TempusTheme.type.button, color = TempusTheme.colors.inkSoft)
                }
            }
        },
    )
}
