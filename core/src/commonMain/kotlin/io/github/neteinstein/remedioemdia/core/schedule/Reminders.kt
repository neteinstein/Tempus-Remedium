package io.github.neteinstein.remedioemdia.core.schedule

import io.github.neteinstein.remedioemdia.core.model.AppData
import io.github.neteinstein.remedioemdia.core.model.TimeSlot
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.plus

/** A slot of a given day that should trigger a reminder. */
data class ReminderSlot(
    val date: LocalDate,
    val slot: TimeSlot,
    val at: LocalDateTime,
)

/**
 * The first slot strictly after [after] that still has pending doses, looking at
 * most [horizonDays] days ahead. Null when reminders are off or nothing is due.
 */
fun AppData.nextReminder(
    after: LocalDateTime,
    horizonDays: Int = 8,
): ReminderSlot? {
    if (!settings.remindersEnabled) return null
    for (offset in 0..horizonDays) {
        val date = after.date.plus(DatePeriod(days = offset))
        for (slot in planFor(date).slots) {
            val at = LocalDateTime(date, slot.time)
            if (at > after && slot.doses.any { it.status == null }) return ReminderSlot(date, slot.slot, at)
        }
    }
    return null
}

/** Medications that ran low between [before] and this state, to warn about once. */
fun AppData.newlyLowStock(before: AppData): List<LowStockItem> {
    val wasLow = before.lowStock().map { it.medication.id }.toSet()
    return lowStock().filterNot { it.medication.id in wasLow }
}
