package org.mariacf.tempusremedium.core

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.mariacf.tempusremedium.core.model.AppData
import org.mariacf.tempusremedium.core.model.Medication
import org.mariacf.tempusremedium.core.model.PillColor
import org.mariacf.tempusremedium.core.model.PillShape
import org.mariacf.tempusremedium.core.model.Schedule
import org.mariacf.tempusremedium.core.model.Stock
import org.mariacf.tempusremedium.core.model.TimeSlot

/**
 * The data shown in docs/design/reference/hoje.png and repor-em-breve.png, used by
 * tests, previews and screenshot tests. Do not "tidy" these values: they must keep
 * reproducing the reference exactly (see docs/design/REFERENCE.md).
 */
object ReferenceFixtures {
    /** quinta-feira, 22 de outubro. */
    val date: LocalDate = LocalDate(2026, 10, 22)

    /** 16:37, when the HOJE reference was captured. */
    val now: LocalDateTime = LocalDateTime(date, LocalTime(16, 37))

    private val start = LocalDate(2026, 10, 1)

    val metformina =
        Medication(
            id = "metformina",
            name = "Metformina",
            strength = "500mg",
            shape = PillShape.ROUND_SCORED,
            color = PillColor.TEAL,
            schedule = Schedule(doses = mapOf(TimeSlot.MORNING to 1, TimeSlot.BEDTIME to 1), startDate = start),
            stock = Stock(remaining = 6),
        )

    val vitaminaD =
        Medication(
            id = "vitamina-d",
            name = "Vitamina D",
            strength = "1000IU",
            shape = PillShape.SQUARE,
            color = PillColor.PINK,
            schedule = Schedule(doses = mapOf(TimeSlot.MORNING to 1), startDate = start),
        )

    val omega3 =
        Medication(
            id = "omega-3",
            name = "Ómega 3",
            strength = "1000mg",
            shape = PillShape.CAPSULE,
            color = PillColor.PEACH,
            schedule = Schedule(doses = mapOf(TimeSlot.MIDDAY to 1), startDate = start),
            stock = Stock(remaining = 3),
        )

    val magnesio =
        Medication(
            id = "magnesio",
            name = "Magnésio",
            strength = "250mg",
            shape = PillShape.SOFTGEL,
            color = PillColor.BLUE,
            schedule = Schedule(doses = mapOf(TimeSlot.EVENING to 1), startDate = start),
        )

    val data: AppData = AppData(medications = listOf(metformina, vitaminaD, omega3, magnesio))
}
