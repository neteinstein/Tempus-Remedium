package org.mariacf.tempusremedium.core.schedule

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import org.mariacf.tempusremedium.core.model.Frequency
import org.mariacf.tempusremedium.core.model.Schedule

/**
 * Whether [date] is a due day of the schedule: on or after the start, before the end
 * (date or number of occurrences) and matching the frequency.
 */
fun Schedule.isDueOn(date: LocalDate): Boolean {
    if (!matchesFrequency(date)) return false
    endDate?.let { if (date > it) return false }
    val limit = occurrences ?: return true
    // Count the due days up to this one; stops as soon as the limit is passed.
    var count = 0
    var day = startDate
    while (day <= date) {
        if (matchesFrequency(day) && ++count > limit) return false
        day = day.plus(DatePeriod(days = 1))
    }
    return true
}

/**
 * The last due day, or null when the schedule never ends. With [Schedule.occurrences] this is
 * the day of the last occurrence (or [Schedule.endDate] when that comes first).
 */
fun Schedule.lastDueDate(): LocalDate? {
    val limit = occurrences ?: return endDate
    var count = 0
    var day = startDate
    // Ten years is far beyond any schedule the editor can make.
    val horizon = startDate.plus(DatePeriod(years = 10))
    while (day <= horizon && (endDate == null || day <= endDate)) {
        if (matchesFrequency(day) && ++count == limit) return day
        day = day.plus(DatePeriod(days = 1))
    }
    return endDate ?: day
}

/** Share of days that are due according to the frequency alone (1.0 for daily, 1/3 every 3 days). */
fun Frequency.dueDaysPerDay(): Double =
    when (this) {
        Frequency.Daily -> 1.0
        is Frequency.Weekdays -> days.size / 7.0 / everyWeeks.coerceAtLeast(1)
        is Frequency.EveryNDays -> 1.0 / interval.coerceAtLeast(1)
        is Frequency.Monthly -> 12.0 / 365.25 / everyMonths.coerceAtLeast(1)
        is Frequency.Cycle -> cycleShare(daysOn, daysOff)
        is Frequency.Contraceptive -> cycleShare(daysOn, daysOff)
    }

private fun cycleShare(
    daysOn: Int,
    daysOff: Int,
): Double = daysOn.coerceAtLeast(0).toDouble() / (daysOn + daysOff).coerceAtLeast(1)

private fun Schedule.matchesFrequency(date: LocalDate): Boolean {
    if (date < startDate) return false
    return when (val frequency = frequency) {
        Frequency.Daily -> {
            true
        }

        is Frequency.Weekdays -> {
            val weeks = startDate.mondayOfWeek().daysUntil(date.mondayOfWeek()) / 7
            date.dayOfWeek in frequency.days && weeks % frequency.everyWeeks.coerceAtLeast(1) == 0
        }

        is Frequency.EveryNDays -> {
            startDate.daysUntil(date) % frequency.interval.coerceAtLeast(1) == 0
        }

        is Frequency.Monthly -> {
            val months = (date.year - startDate.year) * 12 + (date.month.number - startDate.month.number)
            months % frequency.everyMonths.coerceAtLeast(1) == 0 &&
                date.day == minOf(startDate.day, date.lengthOfMonth())
        }

        is Frequency.Cycle -> {
            inCycle(date, frequency.daysOn, frequency.daysOff)
        }

        is Frequency.Contraceptive -> {
            inCycle(date, frequency.daysOn, frequency.daysOff)
        }
    }
}

private fun Schedule.inCycle(
    date: LocalDate,
    daysOn: Int,
    daysOff: Int,
): Boolean = startDate.daysUntil(date) % (daysOn + daysOff).coerceAtLeast(1) < daysOn

private fun LocalDate.mondayOfWeek(): LocalDate = minus(DatePeriod(days = dayOfWeek.ordinal - DayOfWeek.MONDAY.ordinal))

private fun LocalDate.lengthOfMonth(): Int {
    val first = LocalDate(year, month, 1)
    return first.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1)).day
}
