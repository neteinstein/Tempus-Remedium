package org.mariacf.tempusremedium.core

import org.mariacf.tempusremedium.core.ReferenceFixtures.date
import org.mariacf.tempusremedium.core.ReferenceFixtures.magnesio
import org.mariacf.tempusremedium.core.ReferenceFixtures.metformina
import org.mariacf.tempusremedium.core.ReferenceFixtures.omega3
import org.mariacf.tempusremedium.core.data.MedicationRepository
import org.mariacf.tempusremedium.core.data.exportMedications
import org.mariacf.tempusremedium.core.data.importMedications
import org.mariacf.tempusremedium.core.data.parseMedicationsFile
import org.mariacf.tempusremedium.core.model.AppData
import org.mariacf.tempusremedium.core.model.DoseStatus
import org.mariacf.tempusremedium.core.model.TimeSlot
import org.mariacf.tempusremedium.core.schedule.acknowledgeStockAlert
import org.mariacf.tempusremedium.core.schedule.logDose
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MedicationTransferTest {
    @Test
    fun exportAndParseRoundTrip() {
        val text = exportMedications(ReferenceFixtures.data.medications)
        assertEquals(ReferenceFixtures.data.medications, parseMedicationsFile(text))
    }

    @Test
    fun silencedStockAlertStaysBehind() {
        val data = ReferenceFixtures.data.acknowledgeStockAlert(metformina.id, date)
        val exported = parseMedicationsFile(exportMedications(data.medications))!!.first()
        assertEquals(metformina.stock, exported.stock)
    }

    @Test
    fun otherFilesAreRejected() {
        assertNull(parseMedicationsFile("not json"))
        assertNull(parseMedicationsFile("{}"))
        assertNull(parseMedicationsFile("""{"format":"something-else","medications":[]}"""))
        // The app's own document is not an export.
        assertNull(parseMedicationsFile(MedicationRepository.json.encodeToString(AppData.serializer(), ReferenceFixtures.data)))
    }

    @Test
    fun importAddsNewMedications() {
        val data = AppData(medications = listOf(metformina)).importMedications(listOf(omega3, magnesio))
        assertEquals(listOf(metformina, omega3, magnesio), data.medications)
    }

    @Test
    fun importReplacesTheSameMedicationAndKeepsItsHistory() {
        val logged = ReferenceFixtures.data.logDose(metformina.id, date, TimeSlot.MORNING, DoseStatus.TAKEN, 1L)
        val renamed = metformina.copy(notes = "Com a refeição")
        val data = logged.importMedications(listOf(renamed))
        assertEquals(renamed, data.medication(metformina.id))
        assertEquals(ReferenceFixtures.data.medications.size, data.medications.size)
        assertEquals(logged.logs, data.logs)
    }
}
