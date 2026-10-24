package org.mariacf.tempusremedium.core.schedule

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.daysUntil
import org.mariacf.tempusremedium.core.model.AppData
import org.mariacf.tempusremedium.core.model.DoseStatus
import org.mariacf.tempusremedium.core.model.Frequency
import org.mariacf.tempusremedium.core.model.Medication
import org.mariacf.tempusremedium.core.model.TimeSlot

/** One medication to take at one slot of one day. [status] is null while it is still pending. */
data class PlannedDose(
    val medication: Medication,
    val quantity: Int,
    val status: DoseStatus?,
    val loggedAtEpochMillis: Long? = null,
)

data class SlotPlan(
    val slot: TimeSlot,
    val time: LocalTime,
    val doses: List<PlannedDose>,
) {
    val isDone: Boolean
        get() = doses.isNotEmpty() && doses.all { it.status != null }
}

/** The HOJE panel for one date: always the four slots, in order. */
data class DayPlan(
    val date: LocalDate,
    val slots: List<SlotPlan>,
) {
    /** "5 doses": one per medication per slot, regardless of quantity. */
    val doseCount: Int
        get() = slots.sumOf { it.doses.size }

    /** Legend entries in order of first appearance across the slots. */
    val legend: List<Medication>
        get() =
            slots
                .flatMap { slot -> slot.doses.map { it.medication } }
                .distinctBy { it.id }

    fun slot(slot: TimeSlot): SlotPlan = slots.first { it.slot == slot }
}

fun Medication.isDueOn(date: LocalDate): Boolean {
    if (archived) return false
    if (date < schedule.startDate) return false
    schedule.endDate?.let { if (date > it) return false }
    return when (val frequency = schedule.frequency) {
        Frequency.Daily -> true
        is Frequency.Weekdays -> date.dayOfWeek in frequency.days
        is Frequency.EveryNDays -> schedule.startDate.daysUntil(date) % frequency.interval.coerceAtLeast(1) == 0
    }
}

fun Medication.quantityAt(slot: TimeSlot): Int = schedule.doses[slot] ?: 0

fun AppData.planFor(date: LocalDate): DayPlan =
    DayPlan(
        date = date,
        slots =
            TimeSlot.entries.map { slot ->
                SlotPlan(
                    slot = slot,
                    time = settings.timeOf(slot),
                    doses =
                        activeMedications
                            .filter { it.quantityAt(slot) > 0 && it.isDueOn(date) }
                            .map { medication ->
                                val log = log(medication.id, date, slot)
                                PlannedDose(
                                    medication = medication,
                                    quantity = log?.quantity ?: medication.quantityAt(slot),
                                    status = log?.status,
                                    loggedAtEpochMillis = log?.loggedAtEpochMillis,
                                )
                            },
                )
            },
    )
