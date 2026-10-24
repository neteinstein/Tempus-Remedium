package io.github.neteinstein.remedioemdia.core.schedule

import io.github.neteinstein.remedioemdia.core.model.AppData
import io.github.neteinstein.remedioemdia.core.model.DoseStatus
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.minus

data class DaySummary(
    val date: LocalDate,
    /** Doses whose time has come (all of them for past days). */
    val due: Int,
    val taken: Int,
    val skipped: Int,
) {
    val missed: Int
        get() = due - taken - skipped
}

data class AdherenceReport(
    val days: List<DaySummary>,
) {
    val due: Int get() = days.sumOf { it.due }
    val taken: Int get() = days.sumOf { it.taken }

    /** Share of due doses that were taken, or null when nothing was due. */
    val rate: Double?
        get() = if (due == 0) null else taken.toDouble() / due
}

/**
 * Adherence for the [days] days ending at [now]'s date, newest first. On the
 * current day only doses whose slot time has passed, or that were already
 * logged, count as due.
 */
fun AppData.adherence(
    now: LocalDateTime,
    days: Int = 30,
): AdherenceReport =
    AdherenceReport(
        (0 until days).map { offset ->
            val date = now.date.minus(DatePeriod(days = offset))
            val dueDoses =
                planFor(date).slots.flatMap { slot ->
                    val slotHasPassed = date < now.date || slot.time <= now.time
                    slot.doses.filter { slotHasPassed || it.status != null }
                }
            DaySummary(
                date = date,
                due = dueDoses.size,
                taken = dueDoses.count { it.status == DoseStatus.TAKEN },
                skipped = dueDoses.count { it.status == DoseStatus.SKIPPED },
            )
        },
    )
