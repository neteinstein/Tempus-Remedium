package org.mariacf.tempusremedium.core.schedule

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.plus
import org.mariacf.tempusremedium.core.model.AppData
import org.mariacf.tempusremedium.core.model.TimeSlot

/**
 * What the next reminder alarm is for: a HOJE [slot] with pending doses, course doses due at
 * [at], or both when they fall at the same minute.
 */
data class Reminder(
    val date: LocalDate,
    /** Null when only course doses are due. */
    val slot: TimeSlot?,
    val at: LocalDateTime,
    val courseDoses: List<CourseDose> = emptyList(),
)

/**
 * The first slot strictly after [after] that still has pending doses (looking at most
 * [horizonDays] days ahead) or the next pending course dose, whichever comes first.
 * Null when reminders are off or nothing is due.
 */
fun AppData.nextReminder(
    after: LocalDateTime,
    horizonDays: Int = 8,
): Reminder? {
    if (!settings.remindersEnabled) return null
    val slot = nextSlotReminder(after, horizonDays)
    val courseDoses = nextCourseDoses(after)
    val courseAt = courseDoses.firstOrNull()?.at
    return when {
        courseAt == null -> slot
        slot == null || courseAt < slot.at -> Reminder(courseAt.date, null, courseAt, courseDoses)
        courseAt == slot.at -> slot.copy(courseDoses = courseDoses)
        else -> slot
    }
}

private fun AppData.nextSlotReminder(
    after: LocalDateTime,
    horizonDays: Int,
): Reminder? {
    for (offset in 0..horizonDays) {
        val date = after.date.plus(DatePeriod(days = offset))
        for (slot in planFor(date).slots) {
            val at = LocalDateTime(date, slot.time)
            if (at > after && slot.doses.any { it.status == null }) return Reminder(date, slot.slot, at)
        }
    }
    return null
}

/**
 * Doses of [now]'s day whose time has come and that are not logged yet, HOJE slots and courses
 * alike: the count on the app icon's badge.
 */
fun AppData.dueDoseCount(now: LocalDateTime): Int {
    val slotDoses =
        planFor(now.date)
            .slots
            .filter { it.time <= now.time }
            .sumOf { slot -> slot.doses.count { it.status == null } }
    val courseDoses = courseDosesOn(now.date).count { it.at <= now && it.status == null }
    return slotDoses + courseDoses
}

/** Medications that ran low between [before] and this state, to warn about once. */
fun AppData.newlyLowStock(before: AppData): List<LowStockItem> {
    val wasLow = before.lowStock().map { it.medication.id }.toSet()
    return lowStock().filterNot { it.medication.id in wasLow }
}
