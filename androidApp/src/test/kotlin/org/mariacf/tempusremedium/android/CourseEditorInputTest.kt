package org.mariacf.tempusremedium.android

import android.app.Application
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mariacf.tempusremedium.core.ReferenceFixtures
import org.mariacf.tempusremedium.ui.screens.CourseEditor
import org.mariacf.tempusremedium.ui.theme.TempusTheme
import org.robolectric.annotation.Config

/** The first-dose time is typed on a number keypad, which has no ":", so the field adds it. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "pt-rPT", application = Application::class)
class CourseEditorInputTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val timeField get() = composeRule.onNode(hasSetTextAction() and hasText("Hora (HH:MM)"))

    private fun show() {
        composeRule.setContent {
            TempusTheme {
                CourseEditor(medication = null, now = ReferenceFixtures.now, onSave = {}, onDelete = {}, onClose = {}, initialName = "X")
            }
        }
    }

    private fun SemanticsNodeInteraction.shownText(): String = fetchSemanticsNode().config[SemanticsProperties.EditableText].text

    private fun typeAmount() {
        val amount = composeRule.onNode(hasSetTextAction() and hasText(AMOUNT))
        amount.performScrollTo().performTextInput("5")
    }

    private fun type(text: String) {
        timeField.performScrollTo().performTextClearance()
        timeField.performTextInput(text)
        composeRule.waitForIdle()
    }

    @Test
    fun digitsAreShownWithTheColon() {
        show()
        typeAmount()
        type("2000")
        assertEquals("20:00", timeField.shownText())
        composeRule.onNodeWithText("Nenhuma toma com estas datas.").assertDoesNotExist()
    }

    @Test
    fun aSingleDigitHourGetsALeadingZero() {
        show()
        type("805")
        assertEquals("08:05", timeField.shownText())
    }

    @Test
    fun aTypedColonIsNotDoubled() {
        show()
        type("20:30")
        assertEquals("20:30", timeField.shownText())
    }

    @Test
    fun anIncompleteTimeHasNoDoses() {
        show()
        typeAmount()
        type("20")
        assertEquals("20", timeField.shownText())
        composeRule.onNodeWithText("Nenhuma toma com estas datas.").assertExists()
    }

    private companion object {
        const val AMOUNT = "Quantidade"
    }
}
