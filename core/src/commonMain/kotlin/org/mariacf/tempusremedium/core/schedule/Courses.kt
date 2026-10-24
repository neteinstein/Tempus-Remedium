package org.mariacf.tempusremedium.core.schedule

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.mariacf.tempusremedium.core.model.AppData
import org.mariacf.tempusremedium.core.model.Course
import org.mariacf.tempusremedium.core.model.CourseEnd
import org.mariacf.tempusremedium.core.model.DoseStatus
import org.mariacf.tempusremedium.core.model.Medication
import kotlin.time.Duration.Companion.hours

/*
 * Courses taken every few hours (see Course). Times are wall-clock: dose k is k × interval
 * hours after the first one on the clock, whatever the time zone does in between.
 */

/** Dose number [index] of a course. [status] is null while it is still pending. */
data class CourseDose(
    val medication: Medication,
    val index: Int,
    val at: LocalDateTime,
    val status: DoseStatus?,
    val loggedAtEpochMillis: Long? = null,
)

/** Where a course stands at a given moment. */
data class CourseProgress(
    val medication: Medication,
    /** Doses in the whole course. */
    val total: Int,
    val taken: Int,
    val skipped: Int,
    /** The dose to take now or next (it can be overdue); null when the course is over. */
    val next: CourseDose?,
    /** Doses still to take, counting [next]. Overdue doses older than [next] are not counted. */
    val remaining: Int,
    /** The most recently logged dose, so it can be undone. */
    val lastLogged: CourseDose?,
    val lastDoseAt: LocalDateTime?,
) {
    val isOver: Boolean
        get() = next == null

    /** Calendar days with doses left, today included. */
    fun remainingDays(today: LocalDate): Int =
        if (isOver || lastDoseAt == null) 0 else (today.daysUntil(lastDoseAt.date) + 1).coerceAtLeast(0)
}

fun Course.doseAt(index: Int): LocalDateTime = firstDose.plusHours(index.toLong() * step)

/** Doses in the course when none is skipped. */
fun Course.plannedDoseCount(): Int =
    when (val end = end) {
        is CourseEnd.AfterDays -> ceilDiv(end.days * MINUTES_PER_DAY, stepMinutes)
        is CourseEnd.AfterDoses -> end.doses.toLong()
        is CourseEnd.OnDate -> ceilDiv(firstDose.minutesUntil(end.date.plusDays(1).atMidnight()), stepMinutes)
        is CourseEnd.FinishPack -> dosesIn(end.packAmount).toLong()
    }.coerceIn(0, MAX_DOSES.toLong()).toInt()

/** When the last dose is due if none is skipped, or null for an empty course. */
fun Course.plannedLastDose(): LocalDateTime? = plannedDoseCount().takeIf { it > 0 }?.let { doseAt(it - 1) }

/** Doses in [medication]'s course. Skipping a dose of a [CourseEnd.FinishPack] course adds one at the end. */
fun AppData.courseDoseCount(medication: Medication): Int {
    val course = medication.course ?: return 0
    val planned = course.plannedDoseCount()
    if (course.end !is CourseEnd.FinishPack) return planned
    var count = 0
    var used = 0
    while (used < planned && count < MAX_DOSES) {
        if (courseLog(medication.id, count)?.status != DoseStatus.SKIPPED) used++
        count++
    }
    return count
}

fun AppData.courseDose(
    medication: Medication,
    index: Int,
): CourseDose {
    val course = requireNotNull(medication.course) { "${medication.id} has no course" }
    val log = courseLog(medication.id, index)
    return CourseDose(medication, index, course.doseAt(index), log?.status, log?.loggedAtEpochMillis)
}

/** Every dose of [medication]'s course, in order. */
fun AppData.courseDoses(medication: Medication): List<CourseDose> = (0 until courseDoseCount(medication)).map { courseDose(medication, it) }

/** Course doses due on [date], by time, for every active medication. */
fun AppData.courseDosesOn(date: LocalDate): List<CourseDose> =
    activeMedications
        .mapNotNull { medication -> medication.course?.let { medication to it } }
        .flatMap { (medication, course) ->
            val first = course.firstIndexAtOrAfter(date.atMidnight())
            val end = minOf(course.firstIndexAtOrAfter(date.plusDays(1).atMidnight()), courseDoseCount(medication))
            (first until end).map { courseDose(medication, it) }
        }.sortedWith(compareBy<CourseDose> { it.at }.thenBy { it.medication.name })

fun AppData.courseProgress(
    medication: Medication,
    now: LocalDateTime,
): CourseProgress {
    val doses = courseDoses(medication)
    val course = medication.course
    // The dose whose time has come most recently; earlier pending doses count as missed.
    val current = course?.let { (it.lastIndexAtOrBefore(now)).coerceAtLeast(0) } ?: 0
    val upcoming = doses.drop(current).filter { it.status == null }
    return CourseProgress(
        medication = medication,
        total = doses.size,
        taken = doses.count { it.status == DoseStatus.TAKEN },
        skipped = doses.count { it.status == DoseStatus.SKIPPED },
        next = upcoming.firstOrNull(),
        remaining = upcoming.size,
        lastLogged = doses.filter { it.status != null }.maxByOrNull { it.loggedAtEpochMillis ?: 0L },
        lastDoseAt = doses.lastOrNull()?.at,
    )
}

/** Courses with doses still to take, the next dose first. */
fun AppData.activeCourses(now: LocalDateTime): List<CourseProgress> =
    activeMedications
        .filter { it.course != null }
        .map { courseProgress(it, now) }
        .filterNot { it.isOver }
        .sortedWith(compareBy<CourseProgress> { it.next!!.at }.thenBy { it.medication.name })

/** The pending course doses due soonest strictly after [after], all at the same time. */
fun AppData.nextCourseDoses(after: LocalDateTime): List<CourseDose> {
    val candidates =
        activeMedications.mapNotNull { medication ->
            val course = medication.course ?: return@mapNotNull null
            val count = courseDoseCount(medication)
            var index = course.lastIndexAtOrBefore(after) + 1
            while (index < count && courseLog(medication.id, index) != null) index++
            if (index < count) courseDose(medication, index) else null
        }
    val earliest = candidates.minOfOrNull { it.at } ?: return emptyList()
    return candidates.filter { it.at == earliest }.sortedBy { it.medication.name }
}

private val Course.step: Int
    get() = intervalHours.coerceAtLeast(1)

private val Course.stepMinutes: Long
    get() = step * 60L

/** Index of the first dose due at or after [moment]. */
private fun Course.firstIndexAtOrAfter(moment: LocalDateTime): Int =
    ceilDiv(firstDose.minutesUntil(moment), stepMinutes).coerceIn(0, MAX_DOSES.toLong()).toInt()

/** Index of the last dose due at or before [moment], or -1 before the first one. */
private fun Course.lastIndexAtOrBefore(moment: LocalDateTime): Int {
    val minutes = firstDose.minutesUntil(moment)
    return if (minutes < 0) -1 else (minutes / stepMinutes).coerceAtMost(MAX_DOSES.toLong()).toInt()
}

private fun ceilDiv(
    value: Long,
    divisor: Long,
): Long = if (value <= 0) 0 else (value + divisor - 1) / divisor

private fun LocalDateTime.plusHours(hours: Long): LocalDateTime = toInstant(TimeZone.UTC).plus(hours.hours).toLocalDateTime(TimeZone.UTC)

private fun LocalDateTime.minutesUntil(other: LocalDateTime): Long =
    (other.toInstant(TimeZone.UTC) - toInstant(TimeZone.UTC)).inWholeMinutes

private fun LocalDate.plusDays(days: Int): LocalDate = plus(DatePeriod(days = days))

private fun LocalDate.atMidnight(): LocalDateTime = LocalDateTime(this, LocalTime(0, 0))

private const val MINUTES_PER_DAY = 24L * 60

/** A year of hourly doses: guards against runaway end dates. */
private const val MAX_DOSES = 24 * 366
