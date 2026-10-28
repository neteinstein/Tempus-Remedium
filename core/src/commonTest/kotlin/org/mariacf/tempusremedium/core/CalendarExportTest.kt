package org.mariacf.tempusremedium.core

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.mariacf.tempusremedium.core.ReferenceFixtures.date
import org.mariacf.tempusremedium.core.model.DoseStatus
import org.mariacf.tempusremedium.core.model.TimeSlot
import org.mariacf.tempusremedium.core.schedule.CALENDAR_EXPORT_DAYS
import org.mariacf.tempusremedium.core.schedule.CalendarReminder
import org.mariacf.tempusremedium.core.schedule.IcsEvent
import org.mariacf.tempusremedium.core.schedule.calendarExportLastDay
import org.mariacf.tempusremedium.core.schedule.calendarReminders
import org.mariacf.tempusremedium.core.schedule.icsCalendar
import org.mariacf.tempusremedium.core.schedule.logSlot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class CalendarExportTest {
    private val tomorrow = LocalDate(2026, 10, 23)

    private fun at(
        day: LocalDate,
        hour: Int,
    ) = LocalDateTime(day, LocalTime(hour, 0))

    @Test
    fun remindersStartAfterNowAndSkipEmptySlots() {
        val reminders = ReferenceFixtures.data.calendarReminders(ReferenceFixtures.now, days = 2)
        assertEquals(
            listOf(
                CalendarReminder(at(date, 19), TimeSlot.EVENING, 1),
                CalendarReminder(at(date, 22), TimeSlot.BEDTIME, 1),
                CalendarReminder(at(tomorrow, 8), TimeSlot.MORNING, 2),
                CalendarReminder(at(tomorrow, 13), TimeSlot.MIDDAY, 1),
                CalendarReminder(at(tomorrow, 19), TimeSlot.EVENING, 1),
                CalendarReminder(at(tomorrow, 22), TimeSlot.BEDTIME, 1),
            ),
            reminders,
        )
    }

    @Test
    fun loggedSlotsAreLeftOut() {
        val logged = ReferenceFixtures.data.logSlot(date, TimeSlot.EVENING, DoseStatus.TAKEN, 1L)
        assertEquals(TimeSlot.BEDTIME, logged.calendarReminders(ReferenceFixtures.now, days = 1).single().slot)
    }

    @Test
    fun courseDosesMergeWithASlotAtTheSameMinute() {
        val reminders = CourseFixtures.data.calendarReminders(ReferenceFixtures.now, days = 2)
        // Amoxicillin every 8 h from 08:00: 00:00 on its own, 08:00 with MANHÃ, 16:00 on its own.
        assertTrue(CalendarReminder(at(tomorrow, 0), null, 1) in reminders)
        assertTrue(CalendarReminder(at(tomorrow, 8), TimeSlot.MORNING, 3) in reminders)
        assertTrue(CalendarReminder(at(tomorrow, 16), null, 1) in reminders)
        assertEquals(reminders.sortedBy { it.at }, reminders)
    }

    @Test
    fun theExportCoversItsDays() {
        val reminders = ReferenceFixtures.data.calendarReminders(ReferenceFixtures.now)
        val last = calendarExportLastDay(date)
        assertEquals(LocalDate(2027, 1, 19), last)
        assertEquals(last, reminders.last().at.date)
        // Four slots a day, less MANHÃ and ALMOÇO of the first day, which are past.
        assertEquals(CALENDAR_EXPORT_DAYS * 4 - 2, reminders.size)
    }

    @Test
    fun icsHasOneEventWithAnAlertPerReminder() {
        val ics =
            icsCalendar(
                name = "Tempus Remedium",
                events = listOf(IcsEvent(uid = "a@b", start = at(tomorrow, 8), summary = "Manhã · 08:00", description = "2 doses; tomar")),
                stamp = Instant.fromEpochSeconds(1_798_000_000),
            )
        val lines = ics.split("\r\n")
        assertEquals("BEGIN:VCALENDAR", lines.first())
        assertEquals("", lines.last())
        assertTrue("DTSTART:20261023T080000" in lines)
        assertTrue("DTSTAMP:20261223T042640Z" in lines)
        assertTrue("SUMMARY:Manhã · 08:00" in lines)
        assertTrue("DESCRIPTION:2 doses\\; tomar" in lines)
        assertTrue("TRIGGER:PT0S" in lines)
        assertEquals(1, lines.count { it == "BEGIN:VALARM" })
    }

    @Test
    fun longLinesFoldAt75OctetsWithoutSplittingCharacters() {
        val summary = "ã".repeat(60)
        val ics = icsCalendar("x", listOf(IcsEvent("u", at(tomorrow, 8), summary, alarm = false)), Instant.fromEpochSeconds(0))
        val lines = ics.split("\r\n").dropLast(1)
        assertTrue(lines.all { it.encodeToByteArray().size <= 75 })
        val unfolded = ics.replace("\r\n ", "")
        assertTrue("SUMMARY:$summary\r\n" in unfolded)
    }
}
