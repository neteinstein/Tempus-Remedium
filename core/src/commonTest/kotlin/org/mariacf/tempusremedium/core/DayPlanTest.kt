package org.mariacf.tempusremedium.core

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.mariacf.tempusremedium.core.ReferenceFixtures.magnesio
import org.mariacf.tempusremedium.core.ReferenceFixtures.metformina
import org.mariacf.tempusremedium.core.ReferenceFixtures.omega3
import org.mariacf.tempusremedium.core.ReferenceFixtures.vitaminaD
import org.mariacf.tempusremedium.core.model.AppData
import org.mariacf.tempusremedium.core.model.Frequency
import org.mariacf.tempusremedium.core.model.Settings
import org.mariacf.tempusremedium.core.model.TimeSlot
import org.mariacf.tempusremedium.core.schedule.isDueOn
import org.mariacf.tempusremedium.core.schedule.planFor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DayPlanTest {
    private val plan = ReferenceFixtures.data.planFor(ReferenceFixtures.date)

    @Test
    fun referenceDayHasFiveDosesInFourSlots() {
        assertEquals(5, plan.doseCount)
        assertEquals(TimeSlot.entries, plan.slots.map { it.slot })
        assertEquals(listOf("08:00", "13:00", "19:00", "22:00"), plan.slots.map { it.time.toString() })
    }

    @Test
    fun referenceSlotsHoldTheReferencePills() {
        assertEquals(listOf(metformina, vitaminaD), plan.slot(TimeSlot.MORNING).doses.map { it.medication })
        assertEquals(listOf(omega3), plan.slot(TimeSlot.MIDDAY).doses.map { it.medication })
        assertEquals(listOf(magnesio), plan.slot(TimeSlot.EVENING).doses.map { it.medication })
        assertEquals(listOf(metformina), plan.slot(TimeSlot.BEDTIME).doses.map { it.medication })
    }

    @Test
    fun legendFollowsFirstAppearance() {
        assertEquals(
            listOf("Metformina 500mg", "Vitamina D 1000IU", "Ómega 3 1000mg", "Magnésio 250mg"),
            plan.legend.map { it.displayName },
        )
    }

    @Test
    fun slotTimesComeFromSettings() {
        val data = ReferenceFixtures.data.copy(settings = Settings(slotTimes = mapOf(TimeSlot.MORNING to LocalTime(7, 30))))
        val slots = data.planFor(ReferenceFixtures.date).slots
        assertEquals(LocalTime(7, 30), slots[0].time)
        assertEquals(TimeSlot.MIDDAY.defaultTime, slots[1].time)
    }

    @Test
    fun weekdayScheduleOnlyOnChosenDays() {
        val med = metformina.withFrequency(Frequency.Weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)))
        assertTrue(med.isDueOn(LocalDate(2026, 10, 22))) // Thursday
        assertFalse(med.isDueOn(LocalDate(2026, 10, 23))) // Friday
        assertTrue(med.isDueOn(LocalDate(2026, 10, 26))) // Monday
    }

    @Test
    fun everyNDaysCountsFromStartDate() {
        val med = metformina.withFrequency(Frequency.EveryNDays(3))
        assertTrue(med.isDueOn(LocalDate(2026, 10, 1)))
        assertFalse(med.isDueOn(LocalDate(2026, 10, 2)))
        assertTrue(med.isDueOn(LocalDate(2026, 10, 4)))
    }

    @Test
    fun outsideStartAndEndDatesNothingIsDue() {
        val med = metformina.copy(schedule = metformina.schedule.copy(endDate = LocalDate(2026, 10, 10)))
        assertFalse(med.isDueOn(LocalDate(2026, 9, 30)))
        assertTrue(med.isDueOn(LocalDate(2026, 10, 10)))
        assertFalse(med.isDueOn(LocalDate(2026, 10, 11)))
    }

    @Test
    fun archivedMedicationsAreHidden() {
        val data = AppData(medications = listOf(metformina.copy(archived = true), vitaminaD))
        assertEquals(1, data.planFor(ReferenceFixtures.date).doseCount)
    }

    @Test
    fun emptyDayStillHasFourSlots() {
        val plan = AppData().planFor(ReferenceFixtures.date)
        assertEquals(4, plan.slots.size)
        assertEquals(0, plan.doseCount)
        assertFalse(plan.slots.any { it.isDone })
    }

    private fun org.mariacf.tempusremedium.core.model.Medication.withFrequency(frequency: Frequency) =
        copy(schedule = schedule.copy(frequency = frequency))
}
