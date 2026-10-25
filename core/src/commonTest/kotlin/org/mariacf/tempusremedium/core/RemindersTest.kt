package org.mariacf.tempusremedium.core

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.mariacf.tempusremedium.core.ReferenceFixtures.date
import org.mariacf.tempusremedium.core.ReferenceFixtures.omega3
import org.mariacf.tempusremedium.core.model.AppData
import org.mariacf.tempusremedium.core.model.DoseStatus
import org.mariacf.tempusremedium.core.model.Settings
import org.mariacf.tempusremedium.core.model.Stock
import org.mariacf.tempusremedium.core.model.TimeSlot
import org.mariacf.tempusremedium.core.schedule.dueDoseCount
import org.mariacf.tempusremedium.core.schedule.logCourseDose
import org.mariacf.tempusremedium.core.schedule.logDose
import org.mariacf.tempusremedium.core.schedule.logSlot
import org.mariacf.tempusremedium.core.schedule.newlyLowStock
import org.mariacf.tempusremedium.core.schedule.nextReminder
import org.mariacf.tempusremedium.core.schedule.upsertMedication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RemindersTest {
    private val data = ReferenceFixtures.data

    @Test
    fun nextReminderIsTheNextSlotWithPendingDoses() {
        // 16:37 -> NOITE at 19:00.
        val next = data.nextReminder(ReferenceFixtures.now)!!
        assertEquals(TimeSlot.EVENING, next.slot)
        assertEquals(LocalDateTime(date, LocalTime(19, 0)), next.at)
    }

    @Test
    fun aSlotIsNotRemindedAgainAtItsOwnTime() {
        val atSeven = LocalDateTime(date, LocalTime(19, 0))
        assertEquals(TimeSlot.BEDTIME, data.nextReminder(atSeven)!!.slot)
    }

    @Test
    fun slotsAlreadyLoggedAreSkipped() {
        val logged = data.logSlot(date, TimeSlot.EVENING, DoseStatus.TAKEN, 1L)
        assertEquals(TimeSlot.BEDTIME, logged.nextReminder(ReferenceFixtures.now)!!.slot)
    }

    @Test
    fun afterBedtimeTheNextReminderIsTomorrowMorning() {
        val late = LocalDateTime(date, LocalTime(23, 0))
        val next = data.nextReminder(late)!!
        assertEquals(LocalDate(2026, 10, 23), next.date)
        assertEquals(TimeSlot.MORNING, next.slot)
    }

    @Test
    fun noReminderWhenDisabledOrEmpty() {
        assertNull(data.copy(settings = Settings(remindersEnabled = false)).nextReminder(ReferenceFixtures.now))
        assertNull(AppData().nextReminder(ReferenceFixtures.now))
    }

    @Test
    fun lowStockIsReportedOnceWhenItIsCrossed() {
        val plenty = data.upsertMedication(omega3.copy(stock = Stock(remaining = 8)))
        val taken = plenty.logDose(omega3.id, date, TimeSlot.MIDDAY, DoseStatus.TAKEN, 1L)
        assertEquals(listOf(omega3.id), taken.newlyLowStock(plenty).map { it.medication.id })
        val takenAgain = taken.logDose(omega3.id, LocalDate(2026, 10, 23), TimeSlot.MIDDAY, DoseStatus.TAKEN, 2L)
        assertTrue(takenAgain.newlyLowStock(taken).isEmpty())
    }

    @Test
    fun dueDosesAreThoseWhoseTimeHasComeAndAreNotLogged() {
        // 16:37: MANHÃ (Metformina, Vitamina D) and ALMOÇO (Ómega 3) are still open.
        assertEquals(3, data.dueDoseCount(ReferenceFixtures.now))
        assertEquals(1, data.logSlot(date, TimeSlot.MORNING, DoseStatus.TAKEN, 1L).dueDoseCount(ReferenceFixtures.now))
        // A slot counts from its own minute.
        assertEquals(4, data.dueDoseCount(LocalDateTime(date, LocalTime(19, 0))))
        assertEquals(0, data.dueDoseCount(LocalDateTime(date, LocalTime(7, 0))))
    }

    @Test
    fun dueDosesIncludeCourseDoses() {
        val withCourse = CourseFixtures.data
        // Course doses at 08:00 and 16:00 are due by 16:37; the one at 00:00 is tomorrow's.
        assertEquals(5, withCourse.dueDoseCount(ReferenceFixtures.now))
        val logged = withCourse.logCourseDose(CourseFixtures.amoxicilina.id, 0, DoseStatus.TAKEN, 1L)
        assertEquals(4, logged.dueDoseCount(ReferenceFixtures.now))
    }
}
