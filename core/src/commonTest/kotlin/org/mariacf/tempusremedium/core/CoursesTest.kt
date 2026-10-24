package org.mariacf.tempusremedium.core

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.mariacf.tempusremedium.core.ReferenceFixtures.date
import org.mariacf.tempusremedium.core.data.MedicationRepository
import org.mariacf.tempusremedium.core.model.AppData
import org.mariacf.tempusremedium.core.model.Course
import org.mariacf.tempusremedium.core.model.CourseEnd
import org.mariacf.tempusremedium.core.model.DoseStatus
import org.mariacf.tempusremedium.core.model.DoseUnit
import org.mariacf.tempusremedium.core.model.Medication
import org.mariacf.tempusremedium.core.model.PillColor
import org.mariacf.tempusremedium.core.model.PillShape
import org.mariacf.tempusremedium.core.model.Schedule
import org.mariacf.tempusremedium.core.model.Settings
import org.mariacf.tempusremedium.core.model.Stock
import org.mariacf.tempusremedium.core.model.TimeSlot
import org.mariacf.tempusremedium.core.schedule.Inventory
import org.mariacf.tempusremedium.core.schedule.activeCourses
import org.mariacf.tempusremedium.core.schedule.adherence
import org.mariacf.tempusremedium.core.schedule.courseDoseCount
import org.mariacf.tempusremedium.core.schedule.courseDoses
import org.mariacf.tempusremedium.core.schedule.courseDosesOn
import org.mariacf.tempusremedium.core.schedule.courseProgress
import org.mariacf.tempusremedium.core.schedule.deleteMedication
import org.mariacf.tempusremedium.core.schedule.doseAt
import org.mariacf.tempusremedium.core.schedule.logCourseDose
import org.mariacf.tempusremedium.core.schedule.logCourseDosesAt
import org.mariacf.tempusremedium.core.schedule.nextReminder
import org.mariacf.tempusremedium.core.schedule.planFor
import org.mariacf.tempusremedium.core.schedule.plannedDoseCount
import org.mariacf.tempusremedium.core.schedule.plannedLastDose
import org.mariacf.tempusremedium.core.schedule.undoCourseDose
import org.mariacf.tempusremedium.core.schedule.upsertMedication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CoursesTest {
    /** 08:00 on the reference day, every 8 hours for 7 days. */
    private val course =
        Course(
            firstDose = at(date, 8),
            intervalHours = 8,
            amount = 6.0,
            unit = DoseUnit.ML,
            end = CourseEnd.AfterDays(7),
        )

    private val amoxicilina =
        Medication(
            id = "amoxicilina",
            name = "Amoxicilina",
            strength = "250 mg/5 mL",
            shape = PillShape.CAPSULE,
            color = PillColor.YELLOW,
            schedule = Schedule(doses = emptyMap(), startDate = date),
            course = course,
        )

    private val data = ReferenceFixtures.data.upsertMedication(amoxicilina)

    private fun at(
        day: LocalDate,
        hour: Int,
        minute: Int = 0,
    ) = LocalDateTime(day, LocalTime(hour, minute))

    private fun day(offset: Int) = LocalDate(2026, 10, 22 + offset)

    private fun withEnd(end: CourseEnd) = course.copy(end = end)

    @Test
    fun everyEndRuleGivesTheRightNumberOfDoses() {
        assertEquals(21, course.plannedDoseCount())
        assertEquals(at(day(7), 0), course.plannedLastDose())
        assertEquals(6, course.copy(intervalHours = 12, firstDose = at(date, 20), end = CourseEnd.AfterDays(3)).plannedDoseCount())
        assertEquals(10, withEnd(CourseEnd.AfterDoses(10)).plannedDoseCount())
        // 22: 08, 16 · 23: 00, 08, 16 · 24: 00, 08, 16
        assertEquals(8, withEnd(CourseEnd.OnDate(day(2))).plannedDoseCount())
        assertEquals(0, withEnd(CourseEnd.OnDate(day(-1))).plannedDoseCount())
        assertNull(withEnd(CourseEnd.OnDate(day(-1))).plannedLastDose())
        // A 100 mL bottle at 6 mL per dose.
        assertEquals(16, withEnd(CourseEnd.FinishPack(100.0)).plannedDoseCount())
        assertEquals(16, course.dosesIn(100.0))
        assertEquals(0, course.copy(amount = 0.0).dosesIn(100.0))
    }

    @Test
    fun dosesFollowTheIntervalAcrossMidnight() {
        assertEquals(at(date, 8), course.doseAt(0))
        assertEquals(at(day(1), 0), course.doseAt(2))
        assertEquals(at(day(7), 0), course.doseAt(20))
        assertEquals(21, data.courseDoses(amoxicilina).size)
    }

    @Test
    fun theDayPlanListsTheCourseDosesOutsideTheSlots() {
        val plan = data.planFor(date)
        assertEquals(listOf(at(date, 8), at(date, 16)), plan.courseDoses.map { it.at })
        // The HOJE panel keeps the reference's five doses.
        assertEquals(5, plan.doseCount)
        assertEquals(listOf(at(day(1), 0), at(day(1), 8), at(day(1), 16)), data.courseDosesOn(day(1)).map { it.at })
        assertEquals(listOf(0, 1), plan.courseDoses.map { it.index })
        assertTrue(data.courseDosesOn(day(8)).isEmpty())
        assertTrue(data.courseDosesOn(day(-1)).isEmpty())
        val archived = data.upsertMedication(amoxicilina.copy(archived = true))
        assertTrue(archived.courseDosesOn(date).isEmpty())
    }

    @Test
    fun skippingADoseOfAFinishThePackCourseAddsOneAtTheEnd() {
        val bottle = amoxicilina.copy(course = withEnd(CourseEnd.FinishPack(100.0)))
        val pack = data.upsertMedication(bottle)
        assertEquals(16, pack.courseDoseCount(bottle))
        val skipped = pack.logCourseDose(bottle.id, 0, DoseStatus.SKIPPED, 1L)
        assertEquals(17, skipped.courseDoseCount(bottle))
        val taken = pack.logCourseDose(bottle.id, 0, DoseStatus.TAKEN, 1L)
        assertEquals(16, taken.courseDoseCount(bottle))
        // Other end rules do not move.
        assertEquals(21, data.logCourseDose(amoxicilina.id, 0, DoseStatus.SKIPPED, 1L).courseDoseCount(amoxicilina))
        assertEquals(0, data.courseDoseCount(ReferenceFixtures.omega3))
    }

    @Test
    fun tabletsUseTheStockAndLiquidsDoNot() {
        val tablets =
            amoxicilina.copy(
                course = course.copy(amount = 1.0, unit = DoseUnit.UNITS),
                stock = Stock(remaining = 20),
            )
        val start = data.upsertMedication(tablets)

        fun AppData.left() = medication(tablets.id)!!.stock!!.remaining

        val taken = start.logCourseDose(tablets.id, 0, DoseStatus.TAKEN, 1L)
        assertEquals(19, taken.left())
        assertEquals(DoseStatus.TAKEN, taken.courseLog(tablets.id, 0)!!.status)
        val changed = taken.logCourseDose(tablets.id, 0, DoseStatus.SKIPPED, 2L)
        assertEquals(20, changed.left())
        assertEquals(20, changed.undoCourseDose(tablets.id, 0).left())
        assertEquals(20, taken.undoCourseDose(tablets.id, 0).left())
        assertTrue(taken.undoCourseDose(tablets.id, 0).courseLogs.isEmpty())
        assertEquals(taken, taken.undoCourseDose(tablets.id, 5))

        assertEquals(1, course.copy(amount = 0.5, unit = DoseUnit.UNITS).stockUnitsPerDose)
        assertEquals(2, course.copy(amount = 1.5, unit = DoseUnit.UNITS).stockUnitsPerDose)
        assertEquals(0, course.stockUnitsPerDose)
        assertEquals(3.0, Inventory.averageDailyUnits(tablets))
        // Logging a course dose of a medication without a course changes nothing.
        assertEquals(data, data.logCourseDose(ReferenceFixtures.omega3.id, 0, DoseStatus.TAKEN, 1L))
        assertEquals(data, data.logCourseDose("missing", 0, DoseStatus.TAKEN, 1L))
    }

    @Test
    fun progressPointsAtTheDoseDueNow() {
        // 16:37: the 16:00 dose is due and still pending.
        val now = ReferenceFixtures.now
        val progress = data.courseProgress(amoxicilina, now)
        assertEquals(1, progress.next!!.index)
        assertEquals(20, progress.remaining)
        assertEquals(8, progress.remainingDays(date))
        assertEquals(21, progress.total)
        assertNull(progress.lastLogged)

        val logged =
            data
                .logCourseDose(amoxicilina.id, 0, DoseStatus.TAKEN, 1L)
                .logCourseDose(amoxicilina.id, 1, DoseStatus.SKIPPED, 2L)
        val after = logged.courseProgress(amoxicilina, now)
        assertEquals(at(day(1), 0), after.next!!.at)
        assertEquals(19, after.remaining)
        assertEquals(1, after.taken)
        assertEquals(1, after.skipped)
        assertEquals(1, after.lastLogged!!.index)

        assertEquals(0, data.courseProgress(amoxicilina, at(date, 7)).next!!.index)
    }

    @Test
    fun aCourseIsOverAfterItsLastDose() {
        val later = at(day(7), 9)
        val progress = data.courseProgress(amoxicilina, later)
        assertTrue(progress.isOver)
        assertEquals(0, progress.remainingDays(day(7)))
        assertTrue(data.activeCourses(later).isEmpty())
        assertEquals(listOf(amoxicilina.id), data.activeCourses(ReferenceFixtures.now).map { it.medication.id })
        assertFalse(data.courseProgress(amoxicilina, ReferenceFixtures.now).isOver)
    }

    @Test
    fun remindersIncludeCourseDoses() {
        // 16:37 -> NOITE at 19:00 comes before the 00:00 dose.
        val evening = data.nextReminder(ReferenceFixtures.now)!!
        assertEquals(TimeSlot.EVENING, evening.slot)
        assertTrue(evening.courseDoses.isEmpty())

        // After DEITAR the 00:00 dose is next, with no slot.
        val night = data.nextReminder(at(date, 22))!!
        assertNull(night.slot)
        assertEquals(at(day(1), 0), night.at)
        assertEquals(day(1), night.date)
        assertEquals(listOf(amoxicilina.id), night.courseDoses.map { it.medication.id })

        // A dose at a slot's time shares its reminder.
        val shared = data.upsertMedication(amoxicilina.copy(course = course.copy(firstDose = at(date, 19))))
        val both = shared.nextReminder(ReferenceFixtures.now)!!
        assertEquals(TimeSlot.EVENING, both.slot)
        assertEquals(1, both.courseDoses.size)

        // Only courses, and logged doses are not reminded again.
        val onlyCourse = AppData().upsertMedication(amoxicilina)
        assertEquals(at(date, 16), onlyCourse.nextReminder(at(date, 9))!!.at)
        val loggedAt16 = onlyCourse.logCourseDose(amoxicilina.id, 1, DoseStatus.TAKEN, 1L)
        assertEquals(at(day(1), 0), loggedAt16.nextReminder(at(date, 9))!!.at)
        assertNull(onlyCourse.nextReminder(at(day(7), 0)))
        assertNull(onlyCourse.copy(settings = Settings(remindersEnabled = false)).nextReminder(at(date, 9)))
    }

    @Test
    fun takingFromACourseReminderLogsEveryDoseAtThatTime() {
        val logged = data.logCourseDosesAt(at(date, 16), DoseStatus.TAKEN, 5L)
        assertEquals(DoseStatus.TAKEN, logged.courseLog(amoxicilina.id, 1)!!.status)
        assertNull(logged.courseLog(amoxicilina.id, 0))
        assertEquals(logged, logged.logCourseDosesAt(at(date, 16), DoseStatus.SKIPPED, 6L))
    }

    @Test
    fun adherenceCountsCourseDosesThatAreDue() {
        val withoutCourse = ReferenceFixtures.data.adherence(ReferenceFixtures.now, days = 1).due
        val withCourse = data.logCourseDose(amoxicilina.id, 0, DoseStatus.TAKEN, 1L).adherence(ReferenceFixtures.now, days = 1)
        assertEquals(withoutCourse + 2, withCourse.due)
        assertEquals(1, withCourse.taken)
    }

    @Test
    fun deletingTheMedicationDeletesItsCourseHistory() {
        val logged = data.logCourseDose(amoxicilina.id, 0, DoseStatus.TAKEN, 1L)
        assertTrue(logged.deleteMedication(amoxicilina.id).courseLogs.isEmpty())
    }

    @Test
    fun coursesSurviveTheJsonRoundTrip() {
        val ends =
            listOf(
                CourseEnd.AfterDays(7),
                CourseEnd.AfterDoses(10),
                CourseEnd.OnDate(day(3)),
                CourseEnd.FinishPack(100.0),
            )
        ends.forEach { end ->
            val stored =
                data
                    .upsertMedication(
                        amoxicilina.copy(course = withEnd(end)),
                    ).logCourseDose(amoxicilina.id, 0, DoseStatus.TAKEN, 1L)
            val json = MedicationRepository.json.encodeToString(AppData.serializer(), stored)
            assertEquals(stored, MedicationRepository.json.decodeFromString(AppData.serializer(), json))
        }
        // Documents written before courses existed still load.
        val old = """{"medications":[],"logs":[]}"""
        assertEquals(AppData(), MedicationRepository.json.decodeFromString(AppData.serializer(), old))
    }
}
