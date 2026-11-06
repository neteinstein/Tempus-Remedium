package org.mariacf.tempusremedium.android

import android.app.Application
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mariacf.tempusremedium.core.ReferenceFixtures
import org.mariacf.tempusremedium.core.dosing.CalculatorMedication
import org.mariacf.tempusremedium.ui.screens.CourseEditor
import org.mariacf.tempusremedium.ui.screens.MedicationEditor
import org.mariacf.tempusremedium.ui.theme.TempusTheme
import org.robolectric.annotation.Config

/** The keyboard closes (the text field loses focus) when a control that takes no text is tapped. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "pt-rPT", application = Application::class)
class EditorKeyboardTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val nameField get() = composeRule.onNode(hasSetTextAction() and hasText("Nome"))

    private fun showMedicationEditor() {
        composeRule.setContent {
            TempusTheme {
                MedicationEditor(ReferenceFixtures.omega3, today = ReferenceFixtures.now.date, onSave = {}, onDelete = {}, onClose = {})
            }
        }
    }

    private fun showCourseEditor() {
        composeRule.setContent {
            TempusTheme {
                CourseEditor(
                    medication = null,
                    now = ReferenceFixtures.now,
                    onSave = {},
                    onDelete = {},
                    onClose = {},
                    initialName = "Amoxicilina",
                    initialCalculator = CalculatorMedication.AMOXICILLIN,
                    initialWeight = "12",
                )
            }
        }
    }

    /** Types in the name field, taps [control] and expects the field to have let go of the focus. */
    private fun assertTapClosesKeyboard(control: () -> SemanticsNodeInteraction) {
        nameField.performScrollTo().performTextInput("a")
        nameField.assertIsFocused()
        control().performScrollTo().performClick()
        composeRule.waitForIdle()
        nameField.assertIsNotFocused()
    }

    @Test
    fun choosingAMedicineTypeClosesTheKeyboard() {
        showMedicationEditor()
        assertTapClosesKeyboard { composeRule.onNodeWithContentDescription("Cápsula") }
    }

    @Test
    fun choosingAColourClosesTheKeyboard() {
        showMedicationEditor()
        assertTapClosesKeyboard { composeRule.onNode(colourSwatch) }
    }

    @Test
    fun aChipClosesTheKeyboard() {
        showMedicationEditor()
        assertTapClosesKeyboard { composeRule.onNodeWithText("Dias da semana") }
    }

    @Test
    fun aStepperClosesTheKeyboard() {
        showMedicationEditor()
        assertTapClosesKeyboard { composeRule.onAllNodes(hasText("+")).onFirst() }
    }

    @Test
    fun theStockSwitchClosesTheKeyboard() {
        showMedicationEditor()
        assertTapClosesKeyboard { composeRule.onNode(isToggleable()) }
    }

    @Test
    fun deleteClosesTheKeyboard() {
        showMedicationEditor()
        assertTapClosesKeyboard { composeRule.onNodeWithText("Apagar") }
    }

    @Test
    fun aDosePresetClosesTheKeyboardInACourse() {
        showCourseEditor()
        assertTapClosesKeyboard { composeRule.onAllNodes(hasText("mg/kg/dia", substring = true)).onFirst() }
    }

    @Test
    fun usingTheSuggestionClosesTheKeyboard() {
        showCourseEditor()
        assertTapClosesKeyboard { composeRule.onNodeWithText("Usar sugestão") }
    }

    private companion object {
        /** A colour swatch that is not the selected one: tiles carry text, swatches only a description. */
        val colourSwatch =
            SemanticsMatcher("an unselected colour") { node ->
                node.config.getOrElseNullable(SemanticsProperties.ContentDescription) { null }?.firstOrNull() == "Azul" &&
                    node.config.getOrElseNullable(SemanticsProperties.Selected) { null } == false
            }
    }
}
