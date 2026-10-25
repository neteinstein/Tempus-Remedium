package org.mariacf.tempusremedium.android

import android.app.Application
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mariacf.tempusremedium.core.ReferenceFixtures
import org.mariacf.tempusremedium.core.data.InMemoryDocumentStorage
import org.mariacf.tempusremedium.core.data.MedicationRepository
import org.mariacf.tempusremedium.core.model.AppData
import org.mariacf.tempusremedium.core.model.Settings
import org.mariacf.tempusremedium.ui.App
import org.mariacf.tempusremedium.ui.PlatformUi
import org.robolectric.annotation.Config

/** The red bar and the first-medication explanation, driven through the whole App. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "pt-rPT", application = Application::class)
class NotificationsNoticeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val missingText = "Faltam permissões: a app não consegue lembrar as tomas."
    private val dialogTitle = "Permissões para os lembretes"

    private fun repository(data: AppData) =
        MedicationRepository(InMemoryDocumentStorage(MedicationRepository.json.encodeToString(AppData.serializer(), data)))

    private fun show(
        data: AppData,
        missing: Boolean,
        platform: PlatformUi = PlatformUi(permissionsMissing = { missing }),
    ) {
        val repository = repository(data)
        runBlocking { repository.load() }
        composeRule.setContent { App(repository, platform = platform) }
        composeRule.waitForIdle()
    }

    @Test
    fun barShowsWhenPermissionsAreMissingAndOpensDefinicoes() {
        show(ReferenceFixtures.data, missing = true)
        composeRule.onNodeWithText(missingText).assertExists()
        composeRule.onNodeWithText("Resolver").performClick()
        // The app's own Definições, not the system settings.
        composeRule.onNodeWithText("Horários".uppercase()).assertExists()
    }

    @Test
    fun noBarWhenPermissionsAreGiven() {
        show(ReferenceFixtures.data, missing = false)
        composeRule.onNodeWithText(missingText).assertDoesNotExist()
    }

    @Test
    fun noBarWithoutMedications() {
        show(AppData(), missing = true)
        composeRule.onNodeWithText(missingText).assertDoesNotExist()
    }

    @Test
    fun noBarWhenRemindersAreOff() {
        show(ReferenceFixtures.data.copy(settings = Settings(remindersEnabled = false)), missing = true)
        composeRule.onNodeWithText(missingText).assertDoesNotExist()
    }

    private fun addFirstMedication() {
        composeRule.onNodeWithText("Adicionar medicamento").performClick()
        composeRule.onNodeWithText("Nome").performTextInput("Metformina")
        composeRule.onNodeWithText("Guardar").performClick()
    }

    @Test
    fun savingTheFirstMedicationExplainsWhyThenAsks() {
        var requested = 0
        show(AppData(), missing = true, PlatformUi(permissionsMissing = { true }, requestPermissions = { requested++ }))
        addFirstMedication()
        composeRule.onNodeWithText(dialogTitle).assertExists()
        composeRule.onNodeWithText("Permitir").performClick()
        assertEquals(1, requested)
        composeRule.onNodeWithText(dialogTitle).assertDoesNotExist()
    }

    @Test
    fun theExplanationCanOpenDefinicoes() {
        show(AppData(), missing = true)
        addFirstMedication()
        composeRule.onNodeWithText("Ver nas Definições ›").performClick()
        composeRule.onNodeWithText(dialogTitle).assertDoesNotExist()
        composeRule.onNodeWithText("Horários".uppercase()).assertExists()
    }

    @Test
    fun noExplanationWhenPermissionsAreGiven() {
        show(AppData(), missing = false)
        addFirstMedication()
        composeRule.onNodeWithText(dialogTitle).assertDoesNotExist()
    }
}
