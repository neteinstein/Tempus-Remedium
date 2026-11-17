package org.mariacf.tempusremedium.core

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.mariacf.tempusremedium.core.data.MedicationRepository
import org.mariacf.tempusremedium.core.model.AppData
import org.mariacf.tempusremedium.core.model.Symptom
import org.mariacf.tempusremedium.core.schedule.EditorOutcome
import org.mariacf.tempusremedium.core.schedule.byDayInOrder
import org.mariacf.tempusremedium.core.schedule.deleteSymptom
import org.mariacf.tempusremedium.core.schedule.deleteSymptoms
import org.mariacf.tempusremedium.core.schedule.discardedPhotos
import org.mariacf.tempusremedium.core.schedule.newestFirstByDay
import org.mariacf.tempusremedium.core.schedule.upsertSymptom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SymptomsTest {
    private val vomiting = Symptom("a", "Vómitos", LocalDateTime(2026, 10, 22, 9, 15))
    private val dizziness = Symptom("b", "Tonturas", LocalDateTime(2026, 10, 21, 22, 0))
    private val headache = Symptom("c", "Dor de cabeça", LocalDateTime(2026, 10, 22, 7, 40))

    private val data = AppData().upsertSymptom(vomiting).upsertSymptom(dizziness).upsertSymptom(headache)

    @Test
    fun addsSymptoms() {
        assertEquals(listOf(vomiting, dizziness, headache), data.symptoms)
    }

    @Test
    fun listsTheMostRecentFirst() {
        assertEquals(
            listOf(
                LocalDate(2026, 10, 22) to listOf(vomiting, headache),
                LocalDate(2026, 10, 21) to listOf(dizziness),
            ),
            data.symptoms.newestFirstByDay(),
        )
    }

    @Test
    fun listsADayInOrder() {
        val days = data.symptoms.byDayInOrder()
        assertEquals(listOf(headache, vomiting), days[LocalDate(2026, 10, 22)])
        assertEquals(listOf(dizziness), days[LocalDate(2026, 10, 21)])
        assertNull(days[LocalDate(2026, 10, 20)])
    }

    @Test
    fun editingKeepsTheIdAndTrimsTheName() {
        val edited = data.upsertSymptom(vomiting.copy(name = "  Náuseas ", at = LocalDateTime(2026, 10, 22, 10, 0)))
        assertEquals(3, edited.symptoms.size)
        assertEquals(Symptom("a", "Náuseas", LocalDateTime(2026, 10, 22, 10, 0)), edited.symptoms.first { it.id == "a" })
    }

    @Test
    fun ignoresABlankName() {
        assertSame(data, data.upsertSymptom(Symptom("d", "   ", LocalDateTime(2026, 10, 22, 12, 0))))
    }

    @Test
    fun deletes() {
        val after = data.deleteSymptom(dizziness.id)
        assertEquals(listOf(vomiting, headache), after.symptoms)
        assertEquals(after.symptoms, after.deleteSymptom("missing").symptoms)
    }

    @Test
    fun deletesASelection() {
        assertEquals(listOf(dizziness), data.deleteSymptoms(setOf(vomiting.id, headache.id, "missing")).symptoms)
        assertTrue(data.deleteSymptoms(data.symptoms.map { it.id }.toSet()).symptoms.isEmpty())
    }

    @Test
    fun keepsAPhoto() {
        val withPhoto = data.upsertSymptom(vomiting.copy(photo = "p1.jpg"))
        assertEquals("p1.jpg", withPhoto.symptoms.first { it.id == vomiting.id }.photo)
        val json = MedicationRepository.json.encodeToString(AppData.serializer(), withPhoto)
        assertEquals(withPhoto, MedicationRepository.json.decodeFromString(AppData.serializer(), json))
    }

    @Test
    fun savingDropsTheReplacedPhoto() {
        assertEquals(setOf("old"), discardedPhotos("old", "new", EditorOutcome.SAVED))
        assertEquals(setOf("old"), discardedPhotos("old", null, EditorOutcome.SAVED))
        assertEquals(emptySet(), discardedPhotos("old", "old", EditorOutcome.SAVED))
        assertEquals(emptySet(), discardedPhotos(null, "new", EditorOutcome.SAVED))
    }

    @Test
    fun closingDropsOnlyTheNewPhoto() {
        assertEquals(setOf("new"), discardedPhotos("old", "new", EditorOutcome.CLOSED))
        assertEquals(setOf("new"), discardedPhotos(null, "new", EditorOutcome.CLOSED))
        assertEquals(emptySet(), discardedPhotos("old", "old", EditorOutcome.CLOSED))
        assertEquals(emptySet(), discardedPhotos("old", null, EditorOutcome.CLOSED))
    }

    @Test
    fun deletingDropsBoth() {
        assertEquals(setOf("old", "new"), discardedPhotos("old", "new", EditorOutcome.DELETED))
        assertEquals(setOf("old"), discardedPhotos("old", "old", EditorOutcome.DELETED))
        assertEquals(emptySet(), discardedPhotos(null, null, EditorOutcome.DELETED))
    }

    @Test
    fun symptomsStayOutOfHistoryByDefault() {
        assertFalse(AppData().settings.symptomsInHistory)
    }

    @Test
    fun oldDocumentsLoadWithoutSymptoms() {
        val loaded = MedicationRepository.json.decodeFromString(AppData.serializer(), """{"schemaVersion":1,"settings":{}}""")
        assertTrue(loaded.symptoms.isEmpty())
        assertFalse(loaded.settings.symptomsInHistory)
    }

    @Test
    fun survivesARoundTrip() {
        val json = MedicationRepository.json.encodeToString(AppData.serializer(), data)
        assertEquals(data, MedicationRepository.json.decodeFromString(AppData.serializer(), json))
    }
}
