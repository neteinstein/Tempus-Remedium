package org.mariacf.tempusremedium.core

import kotlinx.datetime.DayOfWeek
import org.mariacf.tempusremedium.core.ReferenceFixtures.metformina
import org.mariacf.tempusremedium.core.ReferenceFixtures.omega3
import org.mariacf.tempusremedium.core.model.AppData
import org.mariacf.tempusremedium.core.model.Frequency
import org.mariacf.tempusremedium.core.model.PillPortion
import org.mariacf.tempusremedium.core.model.Stock
import org.mariacf.tempusremedium.core.model.TimeSlot
import org.mariacf.tempusremedium.core.schedule.Inventory
import org.mariacf.tempusremedium.core.schedule.lowStock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InventoryTest {
    @Test
    fun referenceRefillListIsOmegaThenMetformina() {
        val items = ReferenceFixtures.data.lowStock()
        assertEquals(listOf("Ómega 3" to 3, "Metformina" to 6), items.map { it.medication.name to it.remaining })
    }

    @Test
    fun defaultThresholdIsAWeekOfSupply() {
        assertEquals(14, Inventory.threshold(metformina)) // 2 a day
        assertEquals(7, Inventory.threshold(omega3)) // 1 a day
    }

    @Test
    fun customThresholdWins() {
        val med = metformina.copy(stock = Stock(remaining = 6, lowThreshold = 5))
        assertFalse(Inventory.isLow(med))
        assertTrue(Inventory.isLow(med.copy(stock = Stock(remaining = 5, lowThreshold = 5))))
    }

    @Test
    fun averageUsageFollowsFrequency() {
        val weekly =
            omega3.copy(schedule = omega3.schedule.copy(frequency = Frequency.Weekdays(setOf(DayOfWeek.MONDAY))))
        assertEquals(1.0 / 7, Inventory.averageDailyUnits(weekly))
        val everyOtherDay = metformina.copy(schedule = metformina.schedule.copy(frequency = Frequency.EveryNDays(2)))
        assertEquals(1.0, Inventory.averageDailyUnits(everyOtherDay))
    }

    @Test
    fun untrackedAndArchivedMedicationsAreNeverLow() {
        assertFalse(Inventory.isLow(ReferenceFixtures.vitaminaD))
        assertEquals(0, Inventory.threshold(ReferenceFixtures.vitaminaD))
        val data = AppData(medications = listOf(omega3.copy(archived = true)))
        assertTrue(data.lowStock().isEmpty())
    }

    @Test
    fun halfTabletsCountStockInPortionsAndReportWholeTablets() {
        // 10 tablets in halves = 20 portions; 2 portions a day (1 tablet) -> threshold of 14 portions.
        val halves = omega3.copy(portion = PillPortion.HALF, stock = Stock(remaining = 20))
        assertEquals(10, halves.stockTablets)
        assertEquals(14, Inventory.threshold(halves.copy(schedule = halves.schedule.copy(doses = mapOf(TimeSlot.MORNING to 2)))))
        assertEquals(listOf(3), AppData(medications = listOf(halves.copy(stock = Stock(remaining = 6)))).lowStock().map { it.remaining })
    }
}
