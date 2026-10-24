package io.github.neteinstein.remedioemdia.core.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable
enum class DoseStatus {
    TAKEN,
    SKIPPED,
}

/** What happened to one scheduled dose. There is at most one log per medication, date and slot. */
@Serializable
data class DoseLog(
    val medicationId: String,
    val date: LocalDate,
    val slot: TimeSlot,
    val status: DoseStatus,
    val quantity: Int,
    val loggedAtEpochMillis: Long,
)

@Serializable
data class Settings(
    val slotTimes: Map<TimeSlot, LocalTime> = TimeSlot.entries.associateWith { it.defaultTime },
    val remindersEnabled: Boolean = true,
    val hideNamesInReminders: Boolean = false,
) {
    fun timeOf(slot: TimeSlot): LocalTime = slotTimes[slot] ?: slot.defaultTime
}

/** Everything the app persists, stored as one JSON document. */
@Serializable
data class AppData(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val medications: List<Medication> = emptyList(),
    val logs: List<DoseLog> = emptyList(),
    val settings: Settings = Settings(),
) {
    val activeMedications: List<Medication>
        get() = medications.filterNot { it.archived }

    fun medication(id: String): Medication? = medications.firstOrNull { it.id == id }

    fun log(
        medicationId: String,
        date: LocalDate,
        slot: TimeSlot,
    ): DoseLog? = logs.firstOrNull { it.medicationId == medicationId && it.date == date && it.slot == slot }

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}
