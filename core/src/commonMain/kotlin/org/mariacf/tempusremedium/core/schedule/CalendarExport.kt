package org.mariacf.tempusremedium.core.schedule

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.mariacf.tempusremedium.core.model.AppData
import org.mariacf.tempusremedium.core.model.TimeSlot
import kotlin.time.Instant

/**
 * The web's way to remind with the app closed: the reminders of the coming days as calendar
 * events with an alert, which the phone's calendar fires itself. It is a snapshot, so the export
 * covers a limited stretch and ends with an event that asks to export again.
 */
const val CALENDAR_EXPORT_DAYS = 90

/** One reminder of the export: a time with [doseCount] doses still to take. */
data class CalendarReminder(
    val at: LocalDateTime,
    /** Null when only course doses are due at [at]. */
    val slot: TimeSlot?,
    val doseCount: Int,
)

/**
 * Every time strictly after [from] and within [days] days with doses not yet logged: HOJE slots
 * and course doses, merged when they fall at the same minute, as [nextReminder] would ring them.
 */
fun AppData.calendarReminders(
    from: LocalDateTime,
    days: Int = CALENDAR_EXPORT_DAYS,
): List<CalendarReminder> =
    (0 until days).flatMap { offset ->
        val date = from.date.plus(DatePeriod(days = offset))
        val slots =
            planFor(date)
                .slots
                .filter { it.hasPending }
                .map { slot -> CalendarReminder(LocalDateTime(date, slot.time), slot.slot, slot.doses.count { it.status == null }) }
        val courses =
            courseDosesOn(date)
                .filter { it.status == null }
                .groupBy { it.at }
                .map { (at, doses) -> CalendarReminder(at, null, doses.size) }
        (slots + courses)
            .groupBy { it.at }
            .map { (at, same) -> CalendarReminder(at, same.firstNotNullOfOrNull { it.slot }, same.sumOf { it.doseCount }) }
            .filter { it.at > from }
            .sortedBy { it.at }
    }

/** One VEVENT of [icsCalendar]. Times are floating: the phone reads them in its own time zone. */
data class IcsEvent(
    /** Stable across exports, so a calendar can tell an event it already has. */
    val uid: String,
    val start: LocalDateTime,
    val summary: String,
    val description: String = "",
    val durationMinutes: Int = 5,
    /** An alert at the start of the event. */
    val alarm: Boolean = true,
)

/** An iCalendar (RFC 5545) document holding [events]: CRLF lines folded at 75 octets. */
fun icsCalendar(
    name: String,
    events: List<IcsEvent>,
    stamp: Instant,
): String {
    val dtStamp = stamp.toLocalDateTime(TimeZone.UTC).ics() + "Z"
    val lines =
        buildList {
            add("BEGIN:VCALENDAR")
            add("VERSION:2.0")
            add("PRODID:-//mariacf.pt//Tempus Remedium//PT")
            add("CALSCALE:GREGORIAN")
            add("METHOD:PUBLISH")
            add("X-WR-CALNAME:${name.icsText()}")
            events.forEach { event ->
                add("BEGIN:VEVENT")
                add("UID:${event.uid.icsText()}")
                add("DTSTAMP:$dtStamp")
                add("DTSTART:${event.start.ics()}")
                add("DURATION:PT${event.durationMinutes}M")
                add("SUMMARY:${event.summary.icsText()}")
                if (event.description.isNotEmpty()) add("DESCRIPTION:${event.description.icsText()}")
                add("TRANSP:TRANSPARENT")
                if (event.alarm) {
                    add("BEGIN:VALARM")
                    add("ACTION:DISPLAY")
                    add("DESCRIPTION:${event.summary.icsText()}")
                    add("TRIGGER:PT0S")
                    add("END:VALARM")
                }
                add("END:VEVENT")
            }
            add("END:VCALENDAR")
        }
    return lines.joinToString("") { it.folded() + "\r\n" }
}

/** "20261028T080000". */
private fun LocalDateTime.ics(): String =
    year.toString().padStart(4, '0') + month.number.two() + day.two() + "T" + hour.two() + minute.two() + second.two()

private fun Int.two(): String = toString().padStart(2, '0')

private fun String.icsText(): String =
    replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace("\r\n", "\\n")
        .replace("\n", "\\n")

/** Content lines longer than 75 octets continue on lines that start with a space; never inside a character. */
private fun String.folded(): String {
    val out = StringBuilder()
    var octets = 0
    var limit = 75
    var i = 0
    while (i < length) {
        val size = if (this[i].isHighSurrogate() && i + 1 < length) 2 else 1
        val char = substring(i, i + size)
        val bytes = char.encodeToByteArray().size
        if (octets + bytes > limit) {
            out.append("\r\n ")
            octets = 0
            // The leading space counts towards the next line's 75.
            limit = 74
        }
        out.append(char)
        octets += bytes
        i += size
    }
    return out.toString()
}

/** The last day the export covers, for its "export again" event. */
fun calendarExportLastDay(
    from: LocalDate,
    days: Int = CALENDAR_EXPORT_DAYS,
): LocalDate = from.plus(DatePeriod(days = days - 1))
