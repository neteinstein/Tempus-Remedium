package org.mariacf.tempusremedium.core

import org.mariacf.tempusremedium.core.ReferenceFixtures.date
import org.mariacf.tempusremedium.core.ReferenceFixtures.metformina
import org.mariacf.tempusremedium.core.ReferenceFixtures.vitaminaD
import org.mariacf.tempusremedium.core.model.AppData
import org.mariacf.tempusremedium.core.model.DoseStatus
import org.mariacf.tempusremedium.core.model.TimeSlot
import org.mariacf.tempusremedium.core.schedule.deleteMedication
import org.mariacf.tempusremedium.core.schedule.logDose
import org.mariacf.tempusremedium.core.schedule.logSlot
import org.mariacf.tempusremedium.core.schedule.planFor
import org.mariacf.tempusremedium.core.schedule.refill
import org.mariacf.tempusremedium.core.schedule.undoDose
import org.mariacf.tempusremedium.core.schedule.upsertMedication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MutationsTest {
    private val data = ReferenceFixtures.data

    private fun AppData.metforminaLeft() = medication(metformina.id)!!.stock!!.remaining

    @Test
    fun takingADoseUsesStock() {
        val after = data.logDose(metformina.id, date, TimeSlot.MORNING, DoseStatus.TAKEN, 1L)
        assertEquals(5, after.metforminaLeft())
        assertEquals(
            DoseStatus.TAKEN,
            after
                .planFor(date)
                .slot(TimeSlot.MORNING)
                .doses[0]
                .status,
        )
    }

    @Test
    fun takingTwiceDoesNotDoubleCount() {
        val after =
            data
                .logDose(metformina.id, date, TimeSlot.MORNING, DoseStatus.TAKEN, 1L)
                .logDose(metformina.id, date, TimeSlot.MORNING, DoseStatus.TAKEN, 2L)
        assertEquals(5, after.metforminaLeft())
        assertEquals(1, after.logs.size)
    }

    @Test
    fun changingToSkippedGivesStockBack() {
        val after =
            data
                .logDose(metformina.id, date, TimeSlot.MORNING, DoseStatus.TAKEN, 1L)
                .logDose(metformina.id, date, TimeSlot.MORNING, DoseStatus.SKIPPED, 2L)
        assertEquals(6, after.metforminaLeft())
        assertEquals(DoseStatus.SKIPPED, after.logs.single().status)
    }

    @Test
    fun undoRemovesLogAndRestoresStock() {
        val after =
            data
                .logDose(metformina.id, date, TimeSlot.MORNING, DoseStatus.TAKEN, 1L)
                .undoDose(metformina.id, date, TimeSlot.MORNING)
        assertEquals(6, after.metforminaLeft())
        assertTrue(after.logs.isEmpty())
        assertSame(data, data.undoDose(metformina.id, date, TimeSlot.MORNING))
    }

    @Test
    fun loggingASlotLogsEveryPendingDose() {
        val after = data.logSlot(date, TimeSlot.MORNING, DoseStatus.TAKEN, 1L)
        assertEquals(setOf(metformina.id, vitaminaD.id), after.logs.map { it.medicationId }.toSet())
        assertTrue(after.planFor(date).slot(TimeSlot.MORNING).isDone)
        assertEquals(5, after.metforminaLeft())
    }

    @Test
    fun stockNeverGoesNegative() {
        val empty = data.upsertMedication(metformina.copy(stock = metformina.stock!!.copy(remaining = 0)))
        assertEquals(0, empty.logDose(metformina.id, date, TimeSlot.MORNING, DoseStatus.TAKEN, 1L).metforminaLeft())
    }

    @Test
    fun refillAddsUnits() {
        assertEquals(36, data.refill(metformina.id, 30).metforminaLeft())
        // Untracked stock stays untracked.
        assertNull(data.refill(vitaminaD.id, 30).medication(vitaminaD.id)!!.stock)
    }

    @Test
    fun unknownMedicationIsIgnored() {
        assertSame(data, data.logDose("nope", date, TimeSlot.MORNING, DoseStatus.TAKEN, 1L))
    }

    @Test
    fun upsertReplacesOrAppends() {
        val renamed = data.upsertMedication(metformina.copy(name = "Metformina XR"))
        assertEquals(4, renamed.medications.size)
        assertEquals("Metformina XR", renamed.medications.first().name)
        val added = data.upsertMedication(metformina.copy(id = "new"))
        assertEquals(5, added.medications.size)
    }

    @Test
    fun deleteRemovesMedicationAndHistory() {
        val after =
            data
                .logDose(metformina.id, date, TimeSlot.MORNING, DoseStatus.TAKEN, 1L)
                .deleteMedication(metformina.id)
        assertNull(after.medication(metformina.id))
        assertTrue(after.logs.isEmpty())
    }
}
