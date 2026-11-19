package org.mariacf.tempusremedium.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.mariacf.tempusremedium.core.input.Collation
import org.mariacf.tempusremedium.core.input.FieldInput
import org.mariacf.tempusremedium.core.model.Symptom
import org.mariacf.tempusremedium.core.model.newSymptomId
import org.mariacf.tempusremedium.core.schedule.EditorOutcome
import org.mariacf.tempusremedium.core.schedule.discardedPhotos
import org.mariacf.tempusremedium.ui.PhotoResult
import org.mariacf.tempusremedium.ui.PhotoStore
import org.mariacf.tempusremedium.ui.PlatformBackHandler
import org.mariacf.tempusremedium.ui.components.BrandButton
import org.mariacf.tempusremedium.ui.components.SoftButton
import org.mariacf.tempusremedium.ui.components.SymptomPhoto
import org.mariacf.tempusremedium.ui.components.brandBackground
import org.mariacf.tempusremedium.ui.components.longDateLabel
import org.mariacf.tempusremedium.ui.resources.Res
import org.mariacf.tempusremedium.ui.resources.action_back
import org.mariacf.tempusremedium.ui.resources.action_cancel
import org.mariacf.tempusremedium.ui.resources.action_choose_photo
import org.mariacf.tempusremedium.ui.resources.action_delete
import org.mariacf.tempusremedium.ui.resources.action_ok
import org.mariacf.tempusremedium.ui.resources.action_remove_photo
import org.mariacf.tempusremedium.ui.resources.action_save
import org.mariacf.tempusremedium.ui.resources.action_take_photo
import org.mariacf.tempusremedium.ui.resources.day_pick
import org.mariacf.tempusremedium.ui.resources.day_today
import org.mariacf.tempusremedium.ui.resources.day_yesterday
import org.mariacf.tempusremedium.ui.resources.field_time
import org.mariacf.tempusremedium.ui.resources.symptom_appetite_loss
import org.mariacf.tempusremedium.ui.resources.symptom_breathlessness
import org.mariacf.tempusremedium.ui.resources.symptom_constipation
import org.mariacf.tempusremedium.ui.resources.symptom_cough
import org.mariacf.tempusremedium.ui.resources.symptom_diarrhoea
import org.mariacf.tempusremedium.ui.resources.symptom_dizziness
import org.mariacf.tempusremedium.ui.resources.symptom_drowsiness
import org.mariacf.tempusremedium.ui.resources.symptom_dry_mouth
import org.mariacf.tempusremedium.ui.resources.symptom_edit
import org.mariacf.tempusremedium.ui.resources.symptom_fever
import org.mariacf.tempusremedium.ui.resources.symptom_field_name
import org.mariacf.tempusremedium.ui.resources.symptom_headache
import org.mariacf.tempusremedium.ui.resources.symptom_insomnia
import org.mariacf.tempusremedium.ui.resources.symptom_itching
import org.mariacf.tempusremedium.ui.resources.symptom_muscle_pain
import org.mariacf.tempusremedium.ui.resources.symptom_nausea
import org.mariacf.tempusremedium.ui.resources.symptom_new
import org.mariacf.tempusremedium.ui.resources.symptom_palpitations
import org.mariacf.tempusremedium.ui.resources.symptom_photo
import org.mariacf.tempusremedium.ui.resources.symptom_photo_description
import org.mariacf.tempusremedium.ui.resources.symptom_photo_failed
import org.mariacf.tempusremedium.ui.resources.symptom_skin_rash
import org.mariacf.tempusremedium.ui.resources.symptom_stomach_ache
import org.mariacf.tempusremedium.ui.resources.symptom_tiredness
import org.mariacf.tempusremedium.ui.resources.symptom_vomiting
import org.mariacf.tempusremedium.ui.resources.symptom_when
import org.mariacf.tempusremedium.ui.resources.symptoms_delete_confirm
import org.mariacf.tempusremedium.ui.resources.symptoms_delete_photos
import org.mariacf.tempusremedium.ui.theme.TempusTheme

/** Common symptoms, offered as chips in alphabetical order of the language in use; the name stays free text. */
private val SuggestedSymptoms: List<StringResource> =
    listOf(
        Res.string.symptom_nausea,
        Res.string.symptom_vomiting,
        Res.string.symptom_dizziness,
        Res.string.symptom_headache,
        Res.string.symptom_stomach_ache,
        Res.string.symptom_diarrhoea,
        Res.string.symptom_constipation,
        Res.string.symptom_drowsiness,
        Res.string.symptom_insomnia,
        Res.string.symptom_tiredness,
        Res.string.symptom_fever,
        Res.string.symptom_itching,
        Res.string.symptom_skin_rash,
        Res.string.symptom_dry_mouth,
        Res.string.symptom_appetite_loss,
        Res.string.symptom_cough,
        Res.string.symptom_breathlessness,
        Res.string.symptom_palpitations,
        Res.string.symptom_muscle_pain,
    )

/**
 * Full-screen form for a new ([symptom] == null) or existing symptom: name, day, time and photo.
 * Photos are stored as soon as they are picked, so [onDiscardPhotos] gets the ones left behind
 * when the form closes (see discardedPhotos).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SymptomEditor(
    symptom: Symptom?,
    now: LocalDateTime,
    onSave: (Symptom) -> Unit,
    onDelete: (Symptom) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    photos: PhotoStore? = null,
    onDiscardPhotos: (Set<String>) -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val today = now.date
    val yesterday = today.minus(DatePeriod(days = 1))
    val startedAt = symptom?.at ?: LocalDateTime(today, LocalTime(now.hour, now.minute))
    val original = symptom?.photo

    var name by rememberSaveable { mutableStateOf(symptom?.name.orEmpty()) }
    var date by rememberSaveable { mutableStateOf(startedAt.date.toString()) }
    var time by rememberSaveable { mutableStateOf(FieldInput.timeToDigits(startedAt.time)) }
    var photo by rememberSaveable { mutableStateOf(original) }
    var busy by remember { mutableStateOf(false) }
    var photoFailed by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var pickingDay by remember { mutableStateOf(false) }

    val day = LocalDate.parse(date)
    val parsedTime = FieldInput.parseTimeDigits(time)
    val canSave = name.isNotBlank() && parsedTime != null && !busy

    fun finish(outcome: EditorOutcome) {
        onDiscardPhotos(discardedPhotos(original, photo, outcome))
    }

    fun close() {
        focusManager.clearFocus()
        finish(EditorOutcome.CLOSED)
        onClose()
    }

    fun attach(request: suspend PhotoStore.() -> PhotoResult) {
        val store = photos ?: return
        focusManager.clearFocus()
        busy = true
        photoFailed = false
        scope.launch {
            val result = store.request()
            busy = false
            when (result) {
                is PhotoResult.Saved -> {
                    // A photo picked earlier in this form, and now replaced, goes at once.
                    photo?.takeIf { it != original }?.let { store.delete(it) }
                    photo = result.id
                }

                PhotoResult.Failed -> {
                    photoFailed = true
                }

                PhotoResult.Cancelled -> {}
            }
        }
    }

    PlatformBackHandler(enabled = true, onBack = ::close)

    Column(modifier.fillMaxSize().brandBackground(TempusTheme.colors)) {
        TopAppBar(
            title = {
                Text(
                    stringResource(if (symptom == null) Res.string.symptom_new else Res.string.symptom_edit),
                    style = TempusTheme.type.title.copy(fontSize = 21.sp),
                    color = TempusTheme.colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            navigationIcon = {
                IconButton(onClick = ::close) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.action_back))
                }
            },
            actions = {
                BrandButton(
                    stringResource(Res.string.action_save),
                    onClick = {
                        focusManager.clearFocus()
                        parsedTime?.let { at ->
                            onSave(
                                Symptom(
                                    id = symptom?.id ?: newSymptomId(),
                                    name = name.trim(),
                                    at = LocalDateTime(day, at),
                                    photo = photo,
                                ),
                            )
                            finish(EditorOutcome.SAVED)
                        }
                    },
                    enabled = canSave,
                    modifier = Modifier.padding(end = 12.dp),
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(Res.string.symptom_field_name)) },
                singleLine = true,
                shape = FieldShape,
                modifier = Modifier.fillMaxWidth(),
            )
            val suggestions = SuggestedSymptoms.map { stringResource(it) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Collation.sort(suggestions) { it }.forEach { label ->
                    FrequencyChip(label, selected = name.trim() == label) { name = label }
                }
            }

            FieldTitle(stringResource(Res.string.symptom_when))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FrequencyChip(stringResource(Res.string.day_today), day == today) { date = today.toString() }
                FrequencyChip(stringResource(Res.string.day_yesterday), day == yesterday) { date = yesterday.toString() }
                // Any other day comes from the calendar; once chosen, the chip shows it.
                val otherDay = day != today && day != yesterday
                FrequencyChip(
                    if (otherDay) longDateLabel(day) else stringResource(Res.string.day_pick),
                    selected = otherDay,
                ) { pickingDay = true }
            }
            TimeField(
                digits = time,
                onDigitsChange = { time = it },
                label = stringResource(Res.string.field_time),
                modifier = Modifier.fillMaxWidth(),
            )

            if (photos != null) {
                FieldTitle(stringResource(Res.string.symptom_photo))
                photo?.let { id ->
                    SymptomPhoto(
                        id = id,
                        photos = photos,
                        contentDescription = stringResource(Res.string.symptom_photo_description, name.trim()),
                        modifier = Modifier.fillMaxWidth().height(240.dp),
                        corner = 18.dp,
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    if (photos.canTakePhoto) {
                        SoftButton(stringResource(Res.string.action_take_photo), onClick = { if (!busy) attach { take() } })
                    }
                    SoftButton(stringResource(Res.string.action_choose_photo), onClick = { if (!busy) attach { pick() } })
                    if (photo != null) {
                        TextButton(
                            onClick = {
                                photo?.takeIf { it != original }?.let { id -> scope.launch { photos.delete(id) } }
                                photo = null
                            },
                        ) {
                            Text(
                                stringResource(Res.string.action_remove_photo),
                                style = TempusTheme.type.button,
                                color = TempusTheme.colors.danger,
                            )
                        }
                    }
                }
                if (photoFailed) {
                    Text(
                        stringResource(Res.string.symptom_photo_failed),
                        style = TempusTheme.type.bodySmall,
                        color = TempusTheme.colors.danger,
                    )
                }
            }

            if (symptom != null) {
                TextButton(
                    onClick = {
                        focusManager.clearFocus()
                        confirmDelete = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.action_delete), style = TempusTheme.type.button, color = TempusTheme.colors.danger)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (pickingDay) {
        SymptomDayPicker(
            initial = day,
            today = today,
            onPick = {
                date = it.toString()
                pickingDay = false
            },
            onDismiss = { pickingDay = false },
        )
    }

    if (confirmDelete && symptom != null) {
        DeleteSymptomsDialog(
            count = 1,
            withPhotos = symptom.photo != null || photo != null,
            onConfirm = {
                confirmDelete = false
                onDelete(symptom)
                finish(EditorOutcome.DELETED)
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

private const val MILLIS_PER_DAY = 86_400_000L

/** Calendar for the symptom's day: from a year before [today] up to [today]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SymptomDayPicker(
    initial: LocalDate,
    today: LocalDate,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val earliest = today.minus(DatePeriod(years = 1))
    val firstMillis = earliest.toEpochDays() * MILLIS_PER_DAY
    val lastMillis = today.toEpochDays() * MILLIS_PER_DAY
    val state =
        rememberDatePickerState(
            initialSelectedDateMillis = initial.toEpochDays() * MILLIS_PER_DAY,
            yearRange = earliest.year..today.year,
            selectableDates =
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis in firstMillis..lastMillis

                    override fun isSelectableYear(year: Int): Boolean = year in earliest.year..today.year
                },
        )
    val palette = TempusTheme.colors
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.let { onPick(LocalDate.fromEpochDays(it / MILLIS_PER_DAY)) } },
                enabled = state.selectedDateMillis != null,
            ) {
                Text(stringResource(Res.string.action_ok), style = TempusTheme.type.button, color = palette.accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel), style = TempusTheme.type.button, color = palette.inkSoft)
            }
        },
        colors = DatePickerDefaults.colors(containerColor = palette.background),
    ) {
        DatePicker(
            state = state,
            showModeToggle = false,
            colors =
                DatePickerDefaults.colors(
                    containerColor = palette.background,
                    selectedDayContainerColor = palette.accent,
                    todayDateBorderColor = palette.accent,
                    todayContentColor = palette.accent,
                    selectedYearContainerColor = palette.accent,
                ),
        )
    }
}

/** "Apagar 2 sintomas?", and that their photos go too. */
@Composable
internal fun DeleteSymptomsDialog(
    count: Int,
    withPhotos: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TempusTheme.colors.background,
        title = { Text(pluralStringResource(Res.plurals.symptoms_delete_confirm, count, count), style = TempusTheme.type.title) },
        text =
            if (withPhotos) {
                { Text(stringResource(Res.string.symptoms_delete_photos), style = TempusTheme.type.bodyText) }
            } else {
                null
            },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(Res.string.action_delete), style = TempusTheme.type.button, color = TempusTheme.colors.danger)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.action_cancel), style = TempusTheme.type.button, color = TempusTheme.colors.inkSoft)
                }
            }
        },
    )
}
