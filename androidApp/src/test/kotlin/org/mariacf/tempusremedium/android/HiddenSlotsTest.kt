package org.mariacf.tempusremedium.android

import android.app.Application
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mariacf.tempusremedium.core.ReferenceFixtures
import org.mariacf.tempusremedium.core.model.Settings
import org.mariacf.tempusremedium.core.model.TimeSlot
import org.mariacf.tempusremedium.core.schedule.planFor
import org.mariacf.tempusremedium.ui.components.HojePanel
import org.mariacf.tempusremedium.ui.screens.MedicationEditor
import org.mariacf.tempusremedium.ui.screens.SettingsScreen
import org.mariacf.tempusremedium.ui.theme.TempusTheme
import org.robolectric.annotation.Config

/** "Horários a mostrar": what Definições changes, and that no dose goes out of sight. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "pt-rPT", application = Application::class)
class HiddenSlotsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val metformina = ReferenceFixtures.metformina
    private val morningOnly = metformina.copy(schedule = metformina.schedule.copy(doses = mapOf(TimeSlot.MORNING to 1)))

    @Test
    fun thePanelDropsAHiddenSlotUnlessItHasDoses() {
        val plan = ReferenceFixtures.data.copy(medications = listOf(morningOnly)).planFor(ReferenceFixtures.date)
        composeRule.setContent {
            TempusTheme { HojePanel(plan, hiddenSlots = setOf(TimeSlot.MORNING, TimeSlot.BEDTIME)) }
        }
        composeRule.onNodeWithText("MANHÃ").assertExists()
        composeRule.onNodeWithText("MEIO-DIA").assertExists()
        composeRule.onNodeWithText("DEITAR").assertDoesNotExist()
    }

    @Test
    fun theEditorDropsAHiddenSlotUnlessTheMedicationUsesIt() {
        composeRule.setContent {
            TempusTheme {
                MedicationEditor(
                    morningOnly,
                    today = ReferenceFixtures.now.date,
                    onSave = {},
                    onDelete = {},
                    onClose = {},
                    hiddenSlots = setOf(TimeSlot.MORNING, TimeSlot.BEDTIME),
                )
            }
        }
        composeRule.onNodeWithText("Manhã").assertExists()
        composeRule.onNodeWithText("Deitar").assertDoesNotExist()
    }

    @Test
    fun definicoesHidesASlot() {
        var settings = Settings()
        composeRule.setContent {
            TempusTheme { SettingsScreen(settings, onChange = { settings = it }) }
        }
        bedtimeSwitch().performScrollTo().performClick()
        assertEquals(setOf(TimeSlot.BEDTIME), settings.hiddenSlots)
    }

    @Test
    fun theLastSlotShowingCannotBeSwitchedOff() {
        val settings = Settings(hiddenSlots = setOf(TimeSlot.MORNING, TimeSlot.MIDDAY, TimeSlot.EVENING))
        composeRule.setContent { TempusTheme { SettingsScreen(settings, onChange = {}) } }
        bedtimeSwitch().assertIsNotEnabled()
    }

    /** Horários a mostrar comes first in Definições, one switch per slot in order. */
    private fun bedtimeSwitch() = composeRule.onAllNodes(isToggleable())[TimeSlot.BEDTIME.ordinal]
}
