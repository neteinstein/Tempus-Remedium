package io.github.neteinstein.remedioemdia.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.neteinstein.remedioemdia.core.data.MedicationRepository
import io.github.neteinstein.remedioemdia.core.model.AppData
import io.github.neteinstein.remedioemdia.core.model.DoseStatus
import io.github.neteinstein.remedioemdia.core.model.Medication
import io.github.neteinstein.remedioemdia.core.model.Settings
import io.github.neteinstein.remedioemdia.core.model.TimeSlot
import io.github.neteinstein.remedioemdia.core.schedule.deleteMedication
import io.github.neteinstein.remedioemdia.core.schedule.logDose
import io.github.neteinstein.remedioemdia.core.schedule.logSlot
import io.github.neteinstein.remedioemdia.core.schedule.refill
import io.github.neteinstein.remedioemdia.core.schedule.undoDose
import io.github.neteinstein.remedioemdia.core.schedule.upsertMedication
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class AppViewModel(
    private val repository: MedicationRepository,
    private val clock: Clock = Clock.System,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) : ViewModel() {
    val data: StateFlow<AppData> = repository.data

    /** Current local time, refreshed every half minute so "today" rolls over at midnight. */
    val now: StateFlow<LocalDateTime> =
        flow {
            while (true) {
                emit(currentTime())
                delay(30_000)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), currentTime())

    init {
        viewModelScope.launch { repository.load() }
    }

    fun logDose(
        medicationId: String,
        date: LocalDate,
        slot: TimeSlot,
        status: DoseStatus,
    ) = update { it.logDose(medicationId, date, slot, status, nowMillis()) }

    fun undoDose(
        medicationId: String,
        date: LocalDate,
        slot: TimeSlot,
    ) = update { it.undoDose(medicationId, date, slot) }

    fun logSlot(
        date: LocalDate,
        slot: TimeSlot,
    ) = update { it.logSlot(date, slot, DoseStatus.TAKEN, nowMillis()) }

    fun refill(
        medicationId: String,
        units: Int,
    ) = update { it.refill(medicationId, units) }

    fun saveMedication(medication: Medication) = update { it.upsertMedication(medication) }

    fun deleteMedication(id: String) = update { it.deleteMedication(id) }

    fun updateSettings(settings: Settings) = update { it.copy(settings = settings) }

    private fun update(transform: (AppData) -> AppData) {
        viewModelScope.launch { repository.update(transform) }
    }

    private fun nowMillis() = clock.now().toEpochMilliseconds()

    private fun currentTime() = clock.now().toLocalDateTime(timeZone())
}
