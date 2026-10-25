package org.mariacf.tempusremedium.core

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.mariacf.tempusremedium.core.ReferenceFixtures.date
import org.mariacf.tempusremedium.core.ReferenceFixtures.metformina
import org.mariacf.tempusremedium.core.ReferenceFixtures.omega3
import org.mariacf.tempusremedium.core.model.AppData
import org.mariacf.tempusremedium.core.model.DoseStatus
import org.mariacf.tempusremedium.core.model.Stock
import org.mariacf.tempusremedium.core.model.TimeSlot
import org.mariacf.tempusremedium.core.schedule.acknowledgeStockAlert
import org.mariacf.tempusremedium.core.schedule.logDose
import org.mariacf.tempusremedium.core.schedule.planFor
import org.mariacf.tempusremedium.core.schedule.runningOut
import org.mariacf.tempusremedium.core.schedule.stockAlerts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AlertsTest {
    private fun at(time: LocalTime) = LocalDateTime(date, time)

    @Test
    fun slotsAreOverdueOnceTheirTimePassesWithDosesPending() {
        val plan = ReferenceFixtures.data.planFor(date)
        assertTrue(plan.overdueSlots(at(LocalTime(7, 59))).isEmpty())
        assertEquals(setOf(TimeSlot.MORNING), plan.overdueSlots(at(LocalTime(8, 0))))
        assertEquals(setOf(TimeSlot.MORNING, TimeSlot.MIDDAY), plan.overdueSlots(at(LocalTime(13, 30))))
    }

    @Test
    fun takingOrSkippingEveryDoseClearsTheSlot() {
        val data =
            ReferenceFixtures.data
                .logDose(metformina.id, date, TimeSlot.MORNING, DoseStatus.TAKEN, 0)
        assertEquals(setOf(TimeSlot.MORNING), data.planFor(date).overdueSlots(at(LocalTime(9, 0))))
        val done = data.logDose(ReferenceFixtures.vitaminaD.id, date, TimeSlot.MORNING, DoseStatus.SKIPPED, 0)
        assertTrue(done.planFor(date).overdueSlots(at(LocalTime(9, 0))).isEmpty())
    }

    @Test
    fun aPastDayHasEveryPendingSlotOverdue() {
        val plan = ReferenceFixtures.data.planFor(date)
        assertEquals(4, plan.overdueSlots(LocalDateTime(date.year, date.month, date.day + 1, 0, 0)).size)
    }

    @Test
    fun referenceStockLastsBeyondTomorrow() {
        assertTrue(ReferenceFixtures.data.runningOut(date).isEmpty())
    }

    @Test
    fun stockRunsOutWhenItDoesNotCoverTodayAndTomorrow() {
        // Ómega 3: one a day at midday, so today + tomorrow need 2.
        val two = AppData(medications = listOf(omega3.copy(stock = Stock(remaining = 2))))
        assertEquals(listOf(omega3.id), two.runningOut(date).map { it.id })
        val three = AppData(medications = listOf(omega3.copy(stock = Stock(remaining = 3))))
        assertTrue(three.runningOut(date).isEmpty())
        val empty = AppData(medications = listOf(omega3.copy(stock = Stock(remaining = 0))))
        assertEquals(listOf(omega3.id), empty.runningOut(date).map { it.id })
    }

    @Test
    fun dosesAlreadyTakenTodayAreNotCountedAgain() {
        val data =
            AppData(medications = listOf(omega3.copy(stock = Stock(remaining = 2))))
                .logDose(omega3.id, date, TimeSlot.MIDDAY, DoseStatus.TAKEN, 0)
        // Took today's (1 left), tomorrow needs 1.
        assertEquals(listOf(omega3.id), data.runningOut(date).map { it.id })
        val refilled = data.copy(medications = data.medications.map { it.copy(stock = Stock(remaining = 2)) })
        assertTrue(refilled.runningOut(date).isEmpty())
    }

    @Test
    fun tappingTheAlertSilencesItForTheRestOfTheDay() {
        val data = AppData(medications = listOf(omega3.copy(stock = Stock(remaining = 1))))
        assertEquals(setOf(omega3.id), data.stockAlerts(date))
        val seen = data.acknowledgeStockAlert(omega3.id, date)
        assertTrue(seen.stockAlerts(date).isEmpty())
        val nextDay = LocalDateTime(date.year, date.month, date.day + 1, 9, 0).date
        assertEquals(setOf(omega3.id), seen.stockAlerts(nextDay))
    }
}
