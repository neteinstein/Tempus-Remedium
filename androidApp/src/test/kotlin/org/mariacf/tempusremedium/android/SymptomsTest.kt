package org.mariacf.tempusremedium.android

import android.app.Application
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mariacf.tempusremedium.core.ReferenceFixtures
import org.mariacf.tempusremedium.core.data.InMemoryDocumentStorage
import org.mariacf.tempusremedium.core.data.MedicationRepository
import org.mariacf.tempusremedium.core.model.AppData
import org.mariacf.tempusremedium.core.model.Symptom
import org.mariacf.tempusremedium.ui.App
import org.mariacf.tempusremedium.ui.AppViewModel
import org.mariacf.tempusremedium.ui.PhotoResult
import org.mariacf.tempusremedium.ui.PlatformUi
import org.robolectric.annotation.Config
import kotlin.time.Clock
import kotlin.time.Instant

/** The Sintomas tab, driven through the whole App at 16:37 on the reference day. */
@RunWith(AndroidJUnit4::class)
// A phone's size: the default screen is so short that the bottom button covers the second row.
@Config(sdk = [36], qualifiers = "pt-rPT-w360dp-h760dp", application = Application::class)
class SymptomsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val dizziness = Symptom("dizziness", "Tonturas", LocalDateTime(ReferenceFixtures.date, LocalTime(9, 15)))

    private fun show(
        data: AppData,
        photos: FakePhotoStore? = null,
    ): MedicationRepository {
        val repository = MedicationRepository(InMemoryDocumentStorage(MedicationRepository.json.encodeToString(AppData.serializer(), data)))
        runBlocking { repository.load() }
        val instant = ReferenceFixtures.now.toInstant(TimeZone.UTC)
        val clock =
            object : Clock {
                override fun now(): Instant = instant
            }
        composeRule.setContent {
            val viewModel = remember { AppViewModel(repository, clock) { TimeZone.UTC } }
            App(repository = repository, viewModel = viewModel, platform = PlatformUi(photos = photos))
        }
        composeRule.waitForIdle()
        return repository
    }

    @Test
    fun theTabSitsLeftOfHistorico() {
        show(ReferenceFixtures.data)
        val symptoms = composeRule.onNodeWithText("Sintomas").fetchSemanticsNode().boundsInRoot
        val history = composeRule.onNodeWithText("Histórico").fetchSemanticsNode().boundsInRoot
        val medications = composeRule.onNodeWithText("Medicamentos").fetchSemanticsNode().boundsInRoot
        assertTrue(medications.right <= symptoms.left)
        assertTrue(symptoms.right <= history.left)
    }

    @Test
    fun addsASymptomFromTheCommonOnes() {
        val repository = show(ReferenceFixtures.data)
        composeRule.onNodeWithText("Sintomas").performClick()
        composeRule.onNodeWithText("Ainda sem sintomas", substring = true).assertExists()
        composeRule.onNodeWithText("+  Registar sintoma").performClick()
        composeRule.onNodeWithText("Vómitos").performScrollTo().performClick()
        composeRule.onNodeWithText("Guardar").performClick()
        composeRule.waitForIdle()

        val saved =
            repository.data.value.symptoms
                .single()
        assertEquals("Vómitos", saved.name)
        assertEquals(ReferenceFixtures.now, saved.at)
        composeRule.onNodeWithText("Vómitos").assertExists()
        composeRule.onNodeWithText("16:37").assertExists()
        composeRule.onNodeWithText("1 REGISTO").assertExists()
    }

    @Test
    fun addsATypedSymptomYesterdayAtAnotherHour() {
        val repository = show(ReferenceFixtures.data)
        composeRule.onNodeWithText("Sintomas").performClick()
        composeRule.onNodeWithText("+  Registar sintoma").performClick()
        composeRule.onNodeWithText("Sintoma").performTextReplacement("Formigueiro")
        composeRule.onNodeWithText("Ontem").performScrollTo().performClick()
        composeRule.onNodeWithText("Hora (HH:MM)").performScrollTo().performTextReplacement("2310")
        composeRule.onNodeWithText("Guardar").performClick()
        composeRule.waitForIdle()

        assertEquals(
            LocalDateTime(LocalDate(2026, 10, 21), LocalTime(23, 10)),
            repository.data.value.symptoms
                .single()
                .at,
        )
        assertEquals(
            "Formigueiro",
            repository.data.value.symptoms
                .single()
                .name,
        )
    }

    @Test
    fun picksAnOlderDayFromTheCalendar() {
        val repository = show(ReferenceFixtures.data)
        composeRule.onNodeWithText("Sintomas").performClick()
        composeRule.onNodeWithText("+  Registar sintoma").performClick()
        composeRule.onNodeWithText("Sintoma").performTextReplacement("Formigueiro")
        composeRule.onNodeWithText("Escolher dia").performScrollTo().performClick()
        // Tomorrow cannot be chosen.
        composeRule.onNode(hasText("sexta-feira, 23 de outubro de 2026") and hasAnyAncestor(isDialog())).assertIsNotEnabled()
        composeRule.onNode(hasText("quinta-feira, 15 de outubro de 2026") and hasAnyAncestor(isDialog())).performClick()
        composeRule.onNode(hasText("OK") and hasAnyAncestor(isDialog())).performClick()
        composeRule.onNodeWithText("quinta-feira, 15 de outubro").assertExists()
        composeRule.onNodeWithText("Guardar").performClick()
        composeRule.waitForIdle()

        assertEquals(
            LocalDate(2026, 10, 15),
            repository.data.value.symptoms
                .single()
                .at.date,
        )
    }

    @Test
    fun editsAndDeletesASymptom() {
        val repository = show(ReferenceFixtures.data.copy(symptoms = listOf(dizziness)))
        composeRule.onNodeWithText("Sintomas").performClick()
        composeRule.onNodeWithText("Tonturas").performClick()
        composeRule.onNodeWithText("Editar sintoma").assertExists()
        composeRule.onNodeWithText("Dor de cabeça").performScrollTo().performClick()
        composeRule.onNodeWithText("Guardar").performClick()
        composeRule.waitForIdle()
        assertEquals(listOf(dizziness.copy(name = "Dor de cabeça")), repository.data.value.symptoms)

        composeRule.onNodeWithText("Dor de cabeça").performClick()
        composeRule.onNodeWithText("Apagar").performScrollTo().performClick()
        composeRule.onNodeWithText("Apagar 1 sintoma?").assertExists()
        composeRule.onNode(hasText("Apagar") and hasAnyAncestor(isDialog())).performClick()
        composeRule.waitForIdle()
        assertTrue(
            repository.data.value.symptoms
                .isEmpty(),
        )
    }

    @Test
    fun historicoLeavesSymptomsOutByDefault() {
        val repository = show(ReferenceFixtures.data.copy(symptoms = listOf(dizziness)))
        assertFalse(repository.data.value.settings.symptomsInHistory)
        composeRule.onNodeWithText("Histórico").performClick()
        composeRule.onNodeWithText("09:15 · Tonturas").assertDoesNotExist()
    }

    @Test
    fun definicoesShowsThemInHistorico() {
        val repository = show(ReferenceFixtures.data.copy(symptoms = listOf(dizziness)))
        composeRule.onNodeWithText("Definições").performClick()
        composeRule.onNodeWithText("Mostrar no Histórico").performScrollTo()
        // The switch sits after the four of Horários a mostrar.
        composeRule.onAllNodes(isToggleable())[4].performClick()
        composeRule.waitForIdle()
        assertTrue(repository.data.value.settings.symptomsInHistory)

        composeRule.onNodeWithText("Histórico").performClick()
        composeRule.onNodeWithText("09:15 · Tonturas").assertExists()
    }

    @Test
    fun offersCommonSymptomsInAlphabeticalOrder() {
        show(ReferenceFixtures.data)
        composeRule.onNodeWithText("Sintomas").performClick()
        composeRule.onNodeWithText("+  Registar sintoma").performClick()
        val expected =
            listOf(
                "Boca seca",
                "Cansaço",
                "Comichão",
                "Diarreia",
                "Dor de barriga",
                "Dor de cabeça",
                "Dores musculares",
                "Falta de apetite",
                "Falta de ar",
                "Febre",
                "Insónia",
                "Manchas na pele",
                "Náuseas",
                "Obstipação",
                "Palpitações",
                "Sonolência",
                "Tonturas",
                "Tosse",
                "Vómitos",
            )
        // Reading order: row by row, left to right.
        val positions = expected.map { composeRule.onNodeWithText(it).fetchSemanticsNode().positionInRoot }
        positions.zipWithNext().forEachIndexed { index, (a, b) ->
            assertTrue("${expected[index]} before ${expected[index + 1]}", b.y > a.y || (b.y == a.y && b.x > a.x))
        }
    }

    @Test
    fun attachesAPhoto() {
        val photos = FakePhotoStore(results = listOf(PhotoResult.Saved("rash.jpg")))
        val repository = show(ReferenceFixtures.data, photos)
        composeRule.onNodeWithText("Sintomas").performClick()
        composeRule.onNodeWithText("+  Registar sintoma").performClick()
        composeRule.onNodeWithText("Manchas na pele").performScrollTo().performClick()
        composeRule.onNodeWithText("Tirar foto").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Foto: Manchas na pele").assertExists()
        composeRule.onNodeWithText("Guardar").performClick()
        composeRule.waitForIdle()

        assertEquals(
            "rash.jpg",
            repository.data.value.symptoms
                .single()
                .photo,
        )
        assertTrue(photos.deleted.isEmpty())
        // The list shows its thumbnail.
        composeRule.onNodeWithContentDescription("Foto: Manchas na pele").assertExists()
    }

    @Test
    fun withoutACameraOnlyThePickerShows() {
        show(ReferenceFixtures.data, FakePhotoStore(canTakePhoto = false))
        composeRule.onNodeWithText("Sintomas").performClick()
        composeRule.onNodeWithText("+  Registar sintoma").performClick()
        composeRule.onNodeWithText("Escolher foto").performScrollTo().assertExists()
        composeRule.onNodeWithText("Tirar foto").assertDoesNotExist()
    }

    @Test
    fun aFailedPhotoSaysSo() {
        show(ReferenceFixtures.data, FakePhotoStore(results = listOf(PhotoResult.Failed)))
        composeRule.onNodeWithText("Sintomas").performClick()
        composeRule.onNodeWithText("+  Registar sintoma").performClick()
        composeRule.onNodeWithText("Escolher foto").performScrollTo().performClick()
        composeRule.onNodeWithText("Não foi possível guardar a foto.").assertExists()
    }

    @Test
    fun replacingAndRemovingPhotosKeepsNoStrays() {
        val withPhoto = dizziness.copy(photo = "old.jpg")
        val photos =
            FakePhotoStore(
                results = listOf(PhotoResult.Saved("new1.jpg"), PhotoResult.Saved("new2.jpg")),
                stored = mutableMapOf("old.jpg" to samplePhoto()),
            )
        val repository = show(ReferenceFixtures.data.copy(symptoms = listOf(withPhoto)), photos)
        composeRule.onNodeWithText("Sintomas").performClick()
        composeRule.onNodeWithText("Tonturas").performClick()
        // Two new photos in a row: the first one, never saved, goes at once.
        composeRule.onNodeWithText("Escolher foto").performScrollTo().performClick()
        composeRule.onNodeWithText("Escolher foto").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(listOf("new1.jpg"), photos.deleted)
        composeRule.onNodeWithText("Guardar").performClick()
        composeRule.waitForIdle()
        // Saving drops the photo it replaced.
        assertEquals(
            "new2.jpg",
            repository.data.value.symptoms
                .single()
                .photo,
        )
        assertEquals(listOf("new1.jpg", "old.jpg"), photos.deleted)

        // Removing the photo and leaving without saving keeps it.
        composeRule.onNodeWithText("Tonturas").performClick()
        composeRule.onNodeWithText("Remover foto").performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("Voltar").performClick()
        composeRule.waitForIdle()
        assertEquals(
            "new2.jpg",
            repository.data.value.symptoms
                .single()
                .photo,
        )
        assertEquals(listOf("new1.jpg", "old.jpg"), photos.deleted)
    }

    @Test
    fun deletesASelectionWithItsPhotos() {
        val headache = Symptom("headache", "Dor de cabeça", LocalDateTime(ReferenceFixtures.date, LocalTime(14, 30)), photo = "h.jpg")
        val vomiting = Symptom("vomiting", "Vómitos", LocalDateTime(LocalDate(2026, 10, 21), LocalTime(22, 0)), photo = "v.jpg")
        val photos = FakePhotoStore(stored = mutableMapOf("h.jpg" to samplePhoto(), "v.jpg" to samplePhoto()))
        val repository = show(ReferenceFixtures.data.copy(symptoms = listOf(dizziness, headache, vomiting)), photos)
        composeRule.onNodeWithText("Sintomas").performClick()
        composeRule.onNodeWithContentDescription("Apagar sintomas").performClick()
        composeRule.onNodeWithText("0 SELECIONADOS").assertExists()
        composeRule.onNodeWithText("Tonturas").performClick()
        composeRule.onNodeWithText("1 SELECIONADO").assertExists()
        // Scrolling takes the header out of the list; the button keeps the count.
        composeRule.onNodeWithText("Vómitos").performScrollTo().performClick()
        composeRule.onNodeWithText("Apagar (2)").performClick()
        composeRule.onNodeWithText("Apagar 2 sintomas?").assertExists()
        composeRule.onNodeWithText("As fotos anexadas também são apagadas.").assertExists()
        composeRule.onNode(hasText("Apagar") and hasAnyAncestor(isDialog())).performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(headache), repository.data.value.symptoms)
        assertEquals(listOf("v.jpg"), photos.deleted)
        // Back to the list.
        composeRule.onNodeWithText("1 REGISTO").assertExists()
        composeRule.onNode(hasContentDescription("Apagar sintomas")).assertExists()
    }

    @Test
    fun selectsAllAndDeletesEverything() {
        val headache = Symptom("headache", "Dor de cabeça", LocalDateTime(ReferenceFixtures.date, LocalTime(14, 30)))
        val repository = show(ReferenceFixtures.data.copy(symptoms = listOf(dizziness, headache)))
        composeRule.onNodeWithText("Sintomas").performClick()
        composeRule.onNodeWithContentDescription("Apagar sintomas").performClick()
        composeRule.onNodeWithText("Selecionar todos").performClick()
        composeRule.onNodeWithText("2 SELECIONADOS").assertExists()
        // Again: none.
        composeRule.onNodeWithText("Selecionar todos").performClick()
        composeRule.onNodeWithText("0 SELECIONADOS").assertExists()
        composeRule.onNodeWithText("Selecionar todos").performClick()
        composeRule.onNodeWithText("Apagar (2)").performClick()
        composeRule.onNode(hasText("Apagar") and hasAnyAncestor(isDialog())).performClick()
        composeRule.waitForIdle()
        assertTrue(
            repository.data.value.symptoms
                .isEmpty(),
        )
        composeRule.onNodeWithText("Ainda sem sintomas", substring = true).assertExists()
    }

    @Test
    fun cancellingTheSelectionDeletesNothing() {
        val repository = show(ReferenceFixtures.data.copy(symptoms = listOf(dizziness)))
        composeRule.onNodeWithText("Sintomas").performClick()
        composeRule.onNodeWithContentDescription("Apagar sintomas").performClick()
        composeRule.onNodeWithText("Tonturas").performClick()
        composeRule.onNodeWithText("Cancelar").performClick()
        composeRule.waitForIdle()
        assertEquals(listOf(dizziness), repository.data.value.symptoms)
        composeRule.onNodeWithText("1 REGISTO").assertExists()
        // Tapping a symptom edits it again.
        composeRule.onNodeWithText("Tonturas").performClick()
        composeRule.onNodeWithText("Editar sintoma").assertExists()
    }
}
