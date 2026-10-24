package org.mariacf.tempusremedium.core

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.mariacf.tempusremedium.core.model.AppData
import org.mariacf.tempusremedium.core.model.Course
import org.mariacf.tempusremedium.core.model.CourseEnd
import org.mariacf.tempusremedium.core.model.DoseUnit
import org.mariacf.tempusremedium.core.model.Medication
import org.mariacf.tempusremedium.core.model.PillColor
import org.mariacf.tempusremedium.core.model.PillShape
import org.mariacf.tempusremedium.core.model.Schedule
import org.mariacf.tempusremedium.core.schedule.upsertMedication

/**
 * A course next to the reference data, for tests and screenshots: amoxicillin 250 mg/5 mL,
 * 6 mL every 8 hours for 7 days from 08:00 on the reference day (the dose for 12 kg).
 * Not part of the design reference; [ReferenceFixtures] stays untouched.
 */
object CourseFixtures {
    val amoxicilina =
        Medication(
            id = "amoxicilina",
            name = "Amoxicilina",
            strength = "250 mg/5 mL",
            shape = PillShape.CAPSULE,
            color = PillColor.YELLOW,
            schedule = Schedule(doses = emptyMap(), startDate = ReferenceFixtures.date),
            course =
                Course(
                    firstDose = LocalDateTime(ReferenceFixtures.date, LocalTime(8, 0)),
                    intervalHours = 8,
                    amount = 6.0,
                    unit = DoseUnit.ML,
                    end = CourseEnd.AfterDays(7),
                ),
        )

    val data: AppData = ReferenceFixtures.data.upsertMedication(amoxicilina)
}
