package io.github.neteinstein.remedioemdia.core

import io.github.neteinstein.remedioemdia.core.ReferenceFixtures.date
import io.github.neteinstein.remedioemdia.core.ReferenceFixtures.omega3
import io.github.neteinstein.remedioemdia.core.model.AppData
import io.github.neteinstein.remedioemdia.core.model.DoseStatus
import io.github.neteinstein.remedioemdia.core.model.TimeSlot
import io.github.neteinstein.remedioemdia.core.schedule.adherence
import io.github.neteinstein.remedioemdia.core.schedule.logDose
import io.github.neteinstein.remedioemdia.core.schedule.logSlot
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AdherenceTest {
    @Test
    fun todayOnlyCountsSlotsThatHavePassed() {
        // 16:37: MANHÃ (2 doses) and MEIO-DIA (1 dose) are due, NOITE and DEITAR are not yet.
        val data =
            ReferenceFixtures.data
                .logSlot(date, TimeSlot.MORNING, DoseStatus.TAKEN, 1L)
        val today = data.adherence(ReferenceFixtures.now, days = 1).days.single()
        assertEquals(3, today.due)
        assertEquals(2, today.taken)
        assertEquals(1, today.missed)
    }

    @Test
    fun earlyLogsCountEvenBeforeTheirTime() {
        val data = ReferenceFixtures.data.logSlot(date, TimeSlot.BEDTIME, DoseStatus.SKIPPED, 1L)
        val today = data.adherence(ReferenceFixtures.now, days = 1).days.single()
        assertEquals(4, today.due)
        assertEquals(1, today.skipped)
    }

    @Test
    fun rateCoversThePeriodNewestFirst() {
        val data =
            ReferenceFixtures.data
                .logDose(omega3.id, LocalDate(2026, 10, 21), TimeSlot.MIDDAY, DoseStatus.TAKEN, 1L)
        val report = data.adherence(ReferenceFixtures.now, days = 2)
        assertEquals(listOf(date, LocalDate(2026, 10, 21)), report.days.map { it.date })
        assertEquals(8, report.due) // 3 so far today + 5 yesterday
        assertEquals(1, report.taken)
        assertEquals(1.0 / 8, report.rate)
    }

    @Test
    fun noDueDosesMeansNoRate() {
        assertNull(AppData().adherence(ReferenceFixtures.now).rate)
        assertEquals(30, AppData().adherence(ReferenceFixtures.now).days.size)
    }
}
