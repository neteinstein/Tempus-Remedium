package org.mariacf.tempusremedium.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import org.mariacf.tempusremedium.core.data.MedicationRepository
import org.mariacf.tempusremedium.core.model.AppData
import org.mariacf.tempusremedium.core.model.DoseStatus
import org.mariacf.tempusremedium.core.model.Medication
import org.mariacf.tempusremedium.core.model.Settings
import org.mariacf.tempusremedium.core.model.TimeSlot
import org.mariacf.tempusremedium.core.schedule.acknowledgeStockAlert
import org.mariacf.tempusremedium.core.schedule.deleteMedication
import org.mariacf.tempusremedium.core.schedule.logCourseDose
import org.mariacf.tempusremedium.core.schedule.logDose
import org.mariacf.tempusremedium.core.schedule.logSlot
import org.mariacf.tempusremedium.core.schedule.refill
import org.mariacf.tempusremedium.core.schedule.undoCourseDose
import org.mariacf.tempusremedium.core.schedule.undoDose
import org.mariacf.tempusremedium.core.schedule.upsertMedication
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

    fun logCourseDose(
        medicationId: String,
        index: Int,
        status: DoseStatus,
    ) = update { it.logCourseDose(medicationId, index, status, nowMillis()) }

    fun undoCourseDose(
        medicationId: String,
        index: Int,
    ) = update { it.undoCourseDose(medicationId, index) }

    fun refill(
        medicationId: String,
        units: Int,
    ) = update { it.refill(medicationId, units) }

    fun acknowledgeStockAlert(
        medicationId: String,
        today: LocalDate,
    ) = update { it.acknowledgeStockAlert(medicationId, today) }

    fun saveMedication(medication: Medication) = update { it.upsertMedication(medication) }

    fun deleteMedication(id: String) = update { it.deleteMedication(id) }

    fun updateSettings(settings: Settings) = update { it.copy(settings = settings) }

    private fun update(transform: (AppData) -> AppData) {
        viewModelScope.launch { repository.update(transform) }
    }

    private fun nowMillis() = clock.now().toEpochMilliseconds()

    private fun currentTime() = clock.now().toLocalDateTime(timeZone())
}
