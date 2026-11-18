package org.mariacf.tempusremedium.core

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.serialization.json.Json
import org.mariacf.tempusremedium.core.ReferenceFixtures.date
import org.mariacf.tempusremedium.core.ReferenceFixtures.metformina
import org.mariacf.tempusremedium.core.ReferenceFixtures.omega3
import org.mariacf.tempusremedium.core.ReferenceFixtures.vitaminaD
import org.mariacf.tempusremedium.core.model.AppData
import org.mariacf.tempusremedium.core.model.ArchivePeriod
import org.mariacf.tempusremedium.core.model.Course
import org.mariacf.tempusremedium.core.model.CourseEnd
import org.mariacf.tempusremedium.core.model.DoseStatus
import org.mariacf.tempusremedium.core.model.DoseUnit
import org.mariacf.tempusremedium.core.model.Medication
import org.mariacf.tempusremedium.core.model.PillColor
import org.mariacf.tempusremedium.core.model.PillShape
import org.mariacf.tempusremedium.core.model.Schedule
import org.mariacf.tempusremedium.core.model.Stock
import org.mariacf.tempusremedium.core.model.TimeSlot
import org.mariacf.tempusremedium.core.schedule.Inventory
import org.mariacf.tempusremedium.core.schedule.activeCourses
import org.mariacf.tempusremedium.core.schedule.adherence
import org.mariacf.tempusremedium.core.schedule.archiveMedications
import org.mariacf.tempusremedium.core.schedule.courseDosesOn
import org.mariacf.tempusremedium.core.schedule.deleteMedications
import org.mariacf.tempusremedium.core.schedule.dueDoseCount
import org.mariacf.tempusremedium.core.schedule.logCourseDose
import org.mariacf.tempusremedium.core.schedule.logDose
import org.mariacf.tempusremedium.core.schedule.lowStock
import org.mariacf.tempusremedium.core.schedule.nextReminder
import org.mariacf.tempusremedium.core.schedule.planFor
import org.mariacf.tempusremedium.core.schedule.unarchiveMedications
import org.mariacf.tempusremedium.core.schedule.upsertMedication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArchiveTest {
    private fun at(
        day: LocalDate,
        hour: Int,
        minute: Int = 0,
    ) = LocalDateTime(day, LocalTime(hour, minute))

    private fun day(offset: Int) = LocalDate(2026, 10, 22 + offset)

    /** Ómega 3 (13:00) and Metformina (08:00, 22:00) archived on the reference day at 16:37. */
    private val archived = ReferenceFixtures.data.archiveMedications(setOf(omega3.id, metformina.id), ReferenceFixtures.now)

    @Test
    fun archivingMovesMedicationsToTheArchive() {
        assertEquals(listOf(vitaminaD.id, ReferenceFixtures.magnesio.id), archived.activeMedications.map { it.id })
        assertEquals(listOf(metformina.id, omega3.id), archived.archivedMedications.map { it.id })
        assertTrue(archived.medication(omega3.id)!!.isArchived)
        // Archiving twice keeps the first date.
        val again = archived.archiveMedications(setOf(omega3.id), at(day(1), 9))
        assertEquals(listOf(ArchivePeriod(ReferenceFixtures.now)), again.medication(omega3.id)!!.archivePeriods)
    }

    @Test
    fun dosesBeforeTheArchiveStayInTheHistory() {
        val logged = ReferenceFixtures.data.logDose(omega3.id, date, TimeSlot.MIDDAY, DoseStatus.TAKEN, 1L)
        val data = logged.archiveMedications(setOf(omega3.id, metformina.id), ReferenceFixtures.now)
        val plan = data.planFor(date)
        // Due before 16:37: Metformina's morning dose and Ómega 3's midday one stay.
        assertEquals(
            listOf(metformina.id),
            plan
                .slot(TimeSlot.MORNING)
                .doses
                .map { it.medication.id }
                .filter { it == metformina.id },
        )
        assertEquals(listOf(omega3.id), plan.slot(TimeSlot.MIDDAY).doses.map { it.medication.id })
        // Due after it: Metformina's bedtime dose is gone.
        assertTrue(plan.slot(TimeSlot.BEDTIME).doses.isEmpty())
        // The day before is untouched, the day after has nothing from them.
        assertEquals(ReferenceFixtures.data.planFor(day(-1)).doseCount, data.planFor(day(-1)).doseCount)
        assertTrue(
            data
                .planFor(day(1))
                .slots
                .flatMap { it.doses }
                .none { it.medication.id in setOf(omega3.id, metformina.id) },
        )
        // Histórico of the previous days counts them as before.
        val before = logged.adherence(at(day(1), 0), days = 30)
        val after = data.adherence(at(day(1), 0), days = 30)
        assertEquals(before.days.drop(2), after.days.drop(2))
        // On the day of the archive, only Metformina's 22:00 dose is gone.
        assertEquals(before.days[1].due - 1, after.days[1].due)
        assertEquals(1, after.days[1].taken)
    }

    @Test
    fun archivedMedicationsRingNoReminderAndRaiseNoAlert() {
        // Without the archive the next reminder is Magnésio at 19:00, then Metformina at 22:00.
        val reminder = archived.nextReminder(at(date, 19, 30))!!
        assertEquals(at(day(1), 8), reminder.at)
        // Metformina (6 left) and Ómega 3 (3 left) are low in the reference; archived, they are not.
        assertEquals(2, ReferenceFixtures.data.lowStock().size)
        assertTrue(archived.lowStock().isEmpty())
        assertFalse(Inventory.isLow(archived.medication(omega3.id)!!))
        // Metformina's pending morning dose no longer counts on the badge.
        assertEquals(ReferenceFixtures.data.dueDoseCount(ReferenceFixtures.now) - 2, archived.dueDoseCount(ReferenceFixtures.now))
    }

    @Test
    fun reactivatingLeavesTheArchivedTimeOut() {
        val back = archived.unarchiveMedications(setOf(omega3.id), at(day(2), 10))
        val omega = back.medication(omega3.id)!!
        assertFalse(omega.isArchived)
        assertEquals(listOf(ArchivePeriod(ReferenceFixtures.now, at(day(2), 10))), omega.archivePeriods)
        assertEquals(
            listOf(omega3.id),
            back
                .planFor(day(2))
                .slot(TimeSlot.MIDDAY)
                .doses
                .map { it.medication.id },
        )
        assertTrue(
            back
                .planFor(day(1))
                .slot(TimeSlot.MIDDAY)
                .doses
                .isEmpty(),
        )
        assertTrue(back.medication(metformina.id)!!.isArchived)
        // Older documents archived with no date come back too.
        val legacy = ReferenceFixtures.data.upsertMedication(vitaminaD.copy(archived = true))
        assertTrue(legacy.medication(vitaminaD.id)!!.isArchived)
        assertTrue(
            legacy
                .planFor(date)
                .slot(TimeSlot.MORNING)
                .doses
                .none { it.medication.id == vitaminaD.id },
        )
        assertFalse(legacy.unarchiveMedications(setOf(vitaminaD.id), ReferenceFixtures.now).medication(vitaminaD.id)!!.isArchived)
    }

    @Test
    fun anArchivedCourseKeepsItsPastDosesOnly() {
        val course =
            Medication(
                id = "antibiotico",
                name = "Antibiótico",
                shape = PillShape.CAPSULE,
                color = PillColor.YELLOW,
                schedule = Schedule(doses = emptyMap(), startDate = date),
                course = Course(firstDose = at(date, 8), intervalHours = 8, unit = DoseUnit.UNITS, end = CourseEnd.Never),
            )
        val data =
            ReferenceFixtures.data
                .upsertMedication(course)
                .logCourseDose(course.id, 0, DoseStatus.TAKEN, 1L)
                .archiveMedications(setOf(course.id), at(date, 12))
        assertEquals(listOf(at(date, 8)), data.courseDosesOn(date).map { it.at })
        assertTrue(data.courseDosesOn(day(1)).isEmpty())
        assertTrue(data.activeCourses(at(date, 12)).isEmpty())
        assertNull(data.nextReminder(at(date, 20))?.courseDoses?.firstOrNull())
    }

    @Test
    fun deletingSeveralRemovesThemAndTheirHistory() {
        val logged = ReferenceFixtures.data.logDose(omega3.id, date, TimeSlot.MIDDAY, DoseStatus.TAKEN, 1L)
        val data = logged.deleteMedications(setOf(omega3.id, vitaminaD.id))
        assertEquals(listOf(metformina.id, ReferenceFixtures.magnesio.id), data.medications.map { it.id })
        assertTrue(data.logs.isEmpty())
    }

    @Test
    fun archivePeriodsSurviveTheJsonDocument() {
        val json = Json { ignoreUnknownKeys = true }
        val decoded = json.decodeFromString(AppData.serializer(), json.encodeToString(AppData.serializer(), archived))
        assertEquals(archived, decoded)
        // A document from before archive periods still reads.
        val old =
            json.decodeFromString(
                Medication.serializer(),
                """{"id":"a","name":"A","shape":"CAPSULE","color":"TEAL","schedule":{"doses":{},"startDate":"2026-10-01"},""" +
                    """"stock":{"remaining":3},"archived":false}""",
            )
        assertEquals(Stock(remaining = 3), old.stock)
        assertTrue(old.archivePeriods.isEmpty())
    }
}
