package org.mariacf.tempusremedium.core

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus
import org.mariacf.tempusremedium.core.data.MedicationRepository
import org.mariacf.tempusremedium.core.model.AppData
import org.mariacf.tempusremedium.core.model.Frequency
import org.mariacf.tempusremedium.core.model.Medication
import org.mariacf.tempusremedium.core.model.PillColor
import org.mariacf.tempusremedium.core.model.PillShape
import org.mariacf.tempusremedium.core.model.Schedule
import org.mariacf.tempusremedium.core.model.Stock
import org.mariacf.tempusremedium.core.model.TimeSlot
import org.mariacf.tempusremedium.core.schedule.Inventory
import org.mariacf.tempusremedium.core.schedule.isDueOn
import org.mariacf.tempusremedium.core.schedule.lastDueDate
import org.mariacf.tempusremedium.core.schedule.nextReminder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecurrenceTest {
    /** Thursday. */
    private val start = LocalDate(2026, 10, 22)

    private fun schedule(
        frequency: Frequency,
        startDate: LocalDate = start,
        endDate: LocalDate? = null,
        occurrences: Int? = null,
    ) = Schedule(mapOf(TimeSlot.MORNING to 1), frequency, startDate, endDate, occurrences)

    /** Day offsets from [from] in the first [days] days that are due. */
    private fun Schedule.dueOffsets(
        days: Int = 30,
        from: LocalDate = startDate,
    ): List<Int> = (0 until days).filter { isDueOn(from.plus(DatePeriod(days = it))) }

    @Test
    fun patchEveryThreeDays() {
        assertEquals(listOf(0, 3, 6, 9, 12), schedule(Frequency.EveryNDays(3)).dueOffsets(days = 15))
    }

    @Test
    fun everyOtherWeekOnChosenDays() {
        val schedule = schedule(Frequency.Weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), everyWeeks = 2))
        // Thu 22 (start week), skip the week of Mon 26, then Mon 2 Nov and Thu 5 Nov, then Mon 16.
        assertEquals(listOf(0, 11, 14, 25, 28), schedule.dueOffsets())
    }

    @Test
    fun weeklyBeforeTheStartIsNeverDue() {
        val schedule = schedule(Frequency.Weekdays(setOf(DayOfWeek.MONDAY)))
        // From Thu 15: Mon 19 is before the start, Mon 26 is the first due day.
        assertEquals(listOf(11), schedule.dueOffsets(days = 14, from = LocalDate(2026, 10, 15)))
    }

    @Test
    fun monthlyOnTheStartDay() {
        val schedule = schedule(Frequency.Monthly(), startDate = LocalDate(2026, 1, 15))
        val due = (0 until 366).map { LocalDate(2026, 1, 1).plus(DatePeriod(days = it)) }.filter(schedule::isDueOn)
        assertEquals(12, due.size)
        assertEquals(List(12) { 15 }, due.map { it.day })
    }

    @Test
    fun monthlyOnThe31stFallsOnTheLastDayOfShortMonths() {
        val schedule = schedule(Frequency.Monthly(), startDate = LocalDate(2026, 1, 31))
        val due = (0 until 120).map { LocalDate(2026, 1, 1).plus(DatePeriod(days = it)) }.filter(schedule::isDueOn)
        assertEquals(listOf(LocalDate(2026, 1, 31), LocalDate(2026, 2, 28), LocalDate(2026, 3, 31), LocalDate(2026, 4, 30)), due)
    }

    @Test
    fun everyThreeMonths() {
        val schedule = schedule(Frequency.Monthly(everyMonths = 3), startDate = LocalDate(2026, 1, 10))
        val due = (0 until 365).map { LocalDate(2026, 1, 1).plus(DatePeriod(days = it)) }.filter(schedule::isDueOn)
        assertEquals(listOf(1, 4, 7, 10), due.map { it.month.ordinal + 1 })
    }

    @Test
    fun cycleOfThreeOnTwoOff() {
        assertEquals(listOf(0, 1, 2, 5, 6, 7), schedule(Frequency.Cycle(daysOn = 3, daysOff = 2)).dueOffsets(days = 10))
    }

    @Test
    fun contraceptivePillTakesThreeWeeksThenPausesOne() {
        val due = schedule(Frequency.Contraceptive()).dueOffsets(days = 56)
        assertEquals((0 until 21) + (28 until 49), due)
    }

    @Test
    fun contraceptiveWithoutPauseIsTakenEveryDay() {
        assertEquals((0 until 10).toList(), schedule(Frequency.Contraceptive(daysOn = 28, daysOff = 0)).dueOffsets(days = 10))
    }

    @Test
    fun contraceptiveCycleStartedOnAnEarlierPack() {
        // Pack started 25 days ago: today is in the pause week, the next pack starts in 3 days.
        val schedule = schedule(Frequency.Contraceptive(daysOn = 21, daysOff = 7), startDate = start.plus(DatePeriod(days = -25)))
        assertEquals(listOf(3, 4, 5), schedule.dueOffsets(days = 6, from = start))
    }

    @Test
    fun contraceptiveSurvivesTheJsonDocument() {
        val data =
            AppData(
                medications =
                    listOf(
                        Medication(
                            "p",
                            "Pílula",
                            shape = PillShape.ROUND_SCORED,
                            color = PillColor.PINK,
                            schedule = schedule(Frequency.Contraceptive(28, 7)),
                        ),
                    ),
            )
        val json = MedicationRepository.json.encodeToString(AppData.serializer(), data)
        assertTrue("\"contraceptive\"" in json)
        assertEquals(data, MedicationRepository.json.decodeFromString(AppData.serializer(), json))
    }

    @Test
    fun endsOnDate() {
        val schedule = schedule(Frequency.EveryNDays(3), endDate = start.plus(DatePeriod(days = 7)))
        assertEquals(listOf(0, 3, 6), schedule.dueOffsets())
        assertEquals(start.plus(DatePeriod(days = 7)), schedule.lastDueDate())
    }

    @Test
    fun endsAfterOccurrences() {
        val schedule = schedule(Frequency.EveryNDays(3), occurrences = 4)
        assertEquals(listOf(0, 3, 6, 9), schedule.dueOffsets())
        assertEquals(start.plus(DatePeriod(days = 9)), schedule.lastDueDate())
        assertNull(schedule(Frequency.Daily).lastDueDate())
    }

    @Test
    fun averageUsageFollowsTheNewFrequencies() {
        fun usage(frequency: Frequency) =
            Inventory.averageDailyUnits(
                Medication("m", "M", shape = PillShape.PATCH, color = PillColor.WHITE, schedule = schedule(frequency)),
            )
        assertEquals(1.0 / 7, usage(Frequency.Weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), everyWeeks = 2)), 1e-9)
        assertEquals(21.0 / 28, usage(Frequency.Cycle(21, 7)), 1e-9)
        assertEquals(21.0 / 28, usage(Frequency.Contraceptive()), 1e-9)
        assertEquals(12 / 365.25, usage(Frequency.Monthly()), 1e-9)
    }

    @Test
    fun reminderReachesAMonthlyDoseWeeksAhead() {
        val monthly =
            Medication(
                "m",
                "Injeção",
                shape = PillShape.INJECTION,
                color = PillColor.WHITE,
                schedule = schedule(Frequency.Monthly(), startDate = LocalDate(2026, 10, 1)),
                stock = Stock(3),
            )
        val next = AppData(medications = listOf(monthly)).nextReminder(LocalDateTime(start, LocalTime(9, 0)))!!
        assertEquals(LocalDate(2026, 11, 1), next.date)
        assertEquals(TimeSlot.MORNING, next.slot)
    }
}
