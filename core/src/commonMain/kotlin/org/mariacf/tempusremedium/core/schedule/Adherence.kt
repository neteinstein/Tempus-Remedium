package org.mariacf.tempusremedium.core.schedule

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.minus
import org.mariacf.tempusremedium.core.model.AppData
import org.mariacf.tempusremedium.core.model.DoseStatus

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
 * current day only doses whose slot (or course dose) time has passed, or that
 * were already logged, count as due.
 */
fun AppData.adherence(
    now: LocalDateTime,
    days: Int = 30,
): AdherenceReport =
    AdherenceReport(
        (0 until days).map { offset ->
            val date = now.date.minus(DatePeriod(days = offset))
            val dueDoses =
                planFor(date)
                    .slots
                    .flatMap { slot ->
                        val slotHasPassed = date < now.date || slot.time <= now.time
                        slot.doses.filter { slotHasPassed || it.status != null }
                    }.map { it.status }
            val dueCourseDoses =
                courseDosesOn(date)
                    .filter { it.at <= now || it.status != null }
                    .map { it.status }
            val statuses = dueDoses + dueCourseDoses
            DaySummary(
                date = date,
                due = statuses.size,
                taken = statuses.count { it == DoseStatus.TAKEN },
                skipped = statuses.count { it == DoseStatus.SKIPPED },
            )
        },
    )
