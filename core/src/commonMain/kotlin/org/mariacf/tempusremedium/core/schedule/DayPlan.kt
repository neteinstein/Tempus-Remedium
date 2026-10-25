package org.mariacf.tempusremedium.core.schedule

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.mariacf.tempusremedium.core.model.AppData
import org.mariacf.tempusremedium.core.model.DoseStatus
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

    val hasPending: Boolean
        get() = doses.any { it.status == null }
}

/** The HOJE panel for one date: always the four slots, in order, plus the course doses of the day. */
data class DayPlan(
    val date: LocalDate,
    val slots: List<SlotPlan>,
    /** Doses of courses taken every few hours (see [courseDosesOn]); not part of the HOJE panel. */
    val courseDoses: List<CourseDose> = emptyList(),
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

    /** Slots whose time has passed at [now] with doses neither taken nor skipped. */
    fun overdueSlots(now: LocalDateTime): Set<TimeSlot> =
        slots
            .filter { it.hasPending && (date < now.date || (date == now.date && it.time <= now.time)) }
            .mapTo(mutableSetOf()) { it.slot }
}

fun Medication.isDueOn(date: LocalDate): Boolean = !archived && schedule.isDueOn(date)

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
        courseDoses = courseDosesOn(date),
    )
