package io.github.neteinstein.remedioemdia.core

import io.github.neteinstein.remedioemdia.core.data.InMemoryDocumentStorage
import io.github.neteinstein.remedioemdia.core.data.MedicationRepository
import io.github.neteinstein.remedioemdia.core.model.AppData
import io.github.neteinstein.remedioemdia.core.model.Frequency
import io.github.neteinstein.remedioemdia.core.model.newMedicationId
import io.github.neteinstein.remedioemdia.core.schedule.upsertMedication
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class MedicationRepositoryTest {
    @Test
    fun startsEmptyWithoutADocument() =
        runTest {
            val repository = MedicationRepository(InMemoryDocumentStorage())
            repository.load()
            assertEquals(AppData(), repository.data.value)
        }

    @Test
    fun updatesArePersistedAndReloaded() =
        runTest {
            val storage = InMemoryDocumentStorage()
            MedicationRepository(storage).update { ReferenceFixtures.data }

            val reloaded = MedicationRepository(storage)
            reloaded.load()
            assertEquals(ReferenceFixtures.data, reloaded.data.value)
        }

    @Test
    fun everyFrequencySurvivesARoundTrip() =
        runTest {
            val storage = InMemoryDocumentStorage()
            val weekly =
                ReferenceFixtures.omega3.copy(
                    id = "weekly",
                    schedule =
                        ReferenceFixtures.omega3.schedule.copy(
                            frequency = Frequency.Weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)),
                        ),
                )
            val everyThree =
                ReferenceFixtures.omega3.copy(
                    id = "every-three",
                    schedule = ReferenceFixtures.omega3.schedule.copy(frequency = Frequency.EveryNDays(3)),
                )
            val data = ReferenceFixtures.data.upsertMedication(weekly).upsertMedication(everyThree)
            MedicationRepository(storage).update { data }

            val reloaded = MedicationRepository(storage)
            reloaded.load()
            assertEquals(data, reloaded.data.value)
        }

    @Test
    fun unchangedDataIsNotRewritten() =
        runTest {
            val storage = InMemoryDocumentStorage()
            val repository = MedicationRepository(storage)
            repository.update { it }
            assertNull(storage.content)
        }

    @Test
    fun unreadableDocumentIsBackedUpNotLost() =
        runTest {
            val storage = InMemoryDocumentStorage("{not json")
            val repository = MedicationRepository(storage)
            repository.load()
            assertEquals(AppData(), repository.data.value)
            assertEquals("{not json", storage.backup)
        }

    @Test
    fun unknownFieldsFromNewerVersionsAreIgnored() =
        runTest {
            val storage = InMemoryDocumentStorage("""{"schemaVersion":1,"medications":[],"future":true}""")
            val repository = MedicationRepository(storage)
            repository.load()
            assertEquals(AppData(), repository.data.value)
        }

    @Test
    fun idsAreUnique() {
        assertNotEquals(newMedicationId(), newMedicationId())
    }
}
