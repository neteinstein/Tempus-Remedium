package io.github.neteinstein.remedioemdia.core

import io.github.neteinstein.remedioemdia.core.ReferenceFixtures.date
import io.github.neteinstein.remedioemdia.core.ReferenceFixtures.omega3
import io.github.neteinstein.remedioemdia.core.model.AppData
import io.github.neteinstein.remedioemdia.core.model.DoseStatus
import io.github.neteinstein.remedioemdia.core.model.Settings
import io.github.neteinstein.remedioemdia.core.model.Stock
import io.github.neteinstein.remedioemdia.core.model.TimeSlot
import io.github.neteinstein.remedioemdia.core.schedule.logDose
import io.github.neteinstein.remedioemdia.core.schedule.logSlot
import io.github.neteinstein.remedioemdia.core.schedule.newlyLowStock
import io.github.neteinstein.remedioemdia.core.schedule.nextReminder
import io.github.neteinstein.remedioemdia.core.schedule.upsertMedication
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
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
}
