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

    private val blockedText = "As notificações estão desligadas: não vamos conseguir avisar das tomas."

    private fun repository(data: AppData) =
        MedicationRepository(InMemoryDocumentStorage(MedicationRepository.json.encodeToString(AppData.serializer(), data)))

    private fun show(
        data: AppData,
        blocked: Boolean,
        platform: PlatformUi = PlatformUi(notificationsBlocked = { blocked }),
    ) {
        val repository = repository(data)
        runBlocking { repository.load() }
        composeRule.setContent { App(repository, platform = platform) }
        composeRule.waitForIdle()
    }

    @Test
    fun barShowsWhenNotificationsAreBlockedAndOpensSettings() {
        var opened = 0
        show(ReferenceFixtures.data, blocked = true, PlatformUi(notificationsBlocked = { true }, openNotificationSettings = { opened++ }))
        composeRule.onNodeWithText(blockedText).assertExists()
        composeRule.onNodeWithText("Ativar").performClick()
        assertEquals(1, opened)
    }

    @Test
    fun noBarWhenNotificationsWork() {
        show(ReferenceFixtures.data, blocked = false)
        composeRule.onNodeWithText(blockedText).assertDoesNotExist()
    }

    @Test
    fun noBarWithoutMedicationsOrWithRemindersOff() {
        show(AppData(), blocked = true)
        composeRule.onNodeWithText(blockedText).assertDoesNotExist()
    }

    @Test
    fun noBarWhenRemindersAreOff() {
        show(ReferenceFixtures.data.copy(settings = Settings(remindersEnabled = false)), blocked = true)
        composeRule.onNodeWithText(blockedText).assertDoesNotExist()
    }

    @Test
    fun savingTheFirstMedicationExplainsWhyThenAsks() {
        var requested = 0
        show(AppData(), blocked = true, PlatformUi(notificationsBlocked = { true }, requestNotifications = { requested++ }))
        composeRule.onNodeWithText("Adicionar medicamento").performClick()
        composeRule.onNodeWithText("Nome").performTextInput("Metformina")
        composeRule.onNodeWithText("Guardar").performClick()
        composeRule.onNodeWithText("Avisos das tomas").assertExists()
        composeRule.onNodeWithText("Permitir").performClick()
        assertEquals(1, requested)
        composeRule.onNodeWithText("Avisos das tomas").assertDoesNotExist()
    }
}
