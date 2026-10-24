package io.github.neteinstein.remedioemdia.core

import io.github.neteinstein.remedioemdia.core.ReferenceFixtures.date
import io.github.neteinstein.remedioemdia.core.ReferenceFixtures.metformina
import io.github.neteinstein.remedioemdia.core.ReferenceFixtures.vitaminaD
import io.github.neteinstein.remedioemdia.core.model.AppData
import io.github.neteinstein.remedioemdia.core.model.DoseStatus
import io.github.neteinstein.remedioemdia.core.model.TimeSlot
import io.github.neteinstein.remedioemdia.core.schedule.deleteMedication
import io.github.neteinstein.remedioemdia.core.schedule.logDose
import io.github.neteinstein.remedioemdia.core.schedule.logSlot
import io.github.neteinstein.remedioemdia.core.schedule.planFor
import io.github.neteinstein.remedioemdia.core.schedule.refill
import io.github.neteinstein.remedioemdia.core.schedule.undoDose
import io.github.neteinstein.remedioemdia.core.schedule.upsertMedication
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
