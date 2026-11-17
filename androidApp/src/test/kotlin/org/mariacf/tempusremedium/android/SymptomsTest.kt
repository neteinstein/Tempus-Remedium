package org.mariacf.tempusremedium.android

import android.app.Application
import androidx.compose.runtime.remember
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
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
import org.robolectric.annotation.Config
import kotlin.time.Clock
import kotlin.time.Instant

/** The Sintomas tab, driven through the whole App at 16:37 on the reference day. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "pt-rPT", application = Application::class)
class SymptomsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val dizziness = Symptom("dizziness", "Tonturas", LocalDateTime(ReferenceFixtures.date, LocalTime(9, 15)))

    private fun show(data: AppData): MedicationRepository {
        val repository = MedicationRepository(InMemoryDocumentStorage(MedicationRepository.json.encodeToString(AppData.serializer(), data)))
        runBlocking { repository.load() }
        val instant = ReferenceFixtures.now.toInstant(TimeZone.UTC)
        val clock =
            object : Clock {
                override fun now(): Instant = instant
            }
        composeRule.setContent {
            val viewModel = remember { AppViewModel(repository, clock) { TimeZone.UTC } }
            App(repository = repository, viewModel = viewModel)
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
        composeRule.onNodeWithText("Vómitos").performClick()
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
        composeRule.onNodeWithText("Sintoma").performTextReplacement("Falta de ar")
        composeRule.onNodeWithText("Ontem").performClick()
        composeRule.onNodeWithText("Hora (HH:MM)").performTextReplacement("2310")
        composeRule.onNodeWithText("Guardar").performClick()
        composeRule.waitForIdle()

        assertEquals(
            LocalDateTime(LocalDate(2026, 10, 21), LocalTime(23, 10)),
            repository.data.value.symptoms
                .single()
                .at,
        )
        assertEquals(
            "Falta de ar",
            repository.data.value.symptoms
                .single()
                .name,
        )
    }

    @Test
    fun editsAndDeletesASymptom() {
        val repository = show(ReferenceFixtures.data.copy(symptoms = listOf(dizziness)))
        composeRule.onNodeWithText("Sintomas").performClick()
        composeRule.onNodeWithText("Tonturas").performClick()
        composeRule.onNodeWithText("Editar sintoma").assertExists()
        composeRule.onNodeWithText("Dor de cabeça").performClick()
        composeRule.onNodeWithText("Guardar").performClick()
        composeRule.waitForIdle()
        assertEquals(listOf(dizziness.copy(name = "Dor de cabeça")), repository.data.value.symptoms)

        composeRule.onNodeWithText("Dor de cabeça").performClick()
        composeRule.onNodeWithText("Apagar").performClick()
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
}
