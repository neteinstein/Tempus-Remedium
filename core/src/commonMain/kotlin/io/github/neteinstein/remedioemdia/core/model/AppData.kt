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

/** How REPOR EM BREVE (and its widget) draws the units left. */
@Serializable
enum class StockDisplay {
    /** A glass jar with the pills piled at the bottom, as in the reference. */
    JARS,

    /** A blister pack with full and popped cells. */
    BLISTERS,
}

@Serializable
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

@Serializable
data class Settings(
    val slotTimes: Map<TimeSlot, LocalTime> = TimeSlot.entries.associateWith { it.defaultTime },
    val remindersEnabled: Boolean = true,
    val hideNamesInReminders: Boolean = false,
    val stockDisplay: StockDisplay = StockDisplay.JARS,
    val theme: ThemeMode = ThemeMode.SYSTEM,
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
