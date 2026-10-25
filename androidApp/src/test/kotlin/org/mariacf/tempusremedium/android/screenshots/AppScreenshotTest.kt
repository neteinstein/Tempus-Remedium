package org.mariacf.tempusremedium.android.screenshots

import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mariacf.tempusremedium.core.ReferenceFixtures
import org.mariacf.tempusremedium.core.data.InMemoryDocumentStorage
import org.mariacf.tempusremedium.core.data.MedicationRepository
import org.mariacf.tempusremedium.core.model.AppData
import org.mariacf.tempusremedium.core.model.AppLanguage
import org.mariacf.tempusremedium.ui.App
import org.mariacf.tempusremedium.ui.AppViewModel
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The whole app on a 360 dp phone: every bottom bar label stays on one line, uncut, in every language.
 * The language comes from the setting, not the qualifiers: the app keeps the first device locale it sees.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "pt-rPT-w360dp-h720dp-xxhdpi")
class AppScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val device = Locale.getDefault()

    @After
    fun restore() = Locale.setDefault(device)

    @Test
    fun app() {
        showApp(AppLanguage.PT)
        assertLabelsFit("Hoje", "Medicamentos", "Histórico", "Definições")
        composeRule.onRoot().captureRoboImage("src/test/screenshots/app_360dp.png", screenshotOptions)
    }

    @Test
    fun appInEnglish() {
        showApp(AppLanguage.EN)
        assertLabelsFit("Today", "Medications", "History", "Settings")
        composeRule.onRoot().captureRoboImage("src/test/screenshots/app_360dp_en.png", screenshotOptions)
    }

    @Test
    fun labelsFitInFrench() {
        showApp(AppLanguage.FR)
        assertLabelsFit("Aujourd’hui", "Médicaments", "Historique", "Réglages")
    }

    @Test
    fun labelsFitInGerman() {
        showApp(AppLanguage.DE)
        assertLabelsFit("Heute", "Medikamente", "Verlauf", "Einstellungen")
    }

    @Test
    fun labelsFitInSpanish() {
        showApp(AppLanguage.ES)
        assertLabelsFit("Hoy", "Medicamentos", "Historial", "Ajustes")
    }

    /** Android's "large" text size: the labels shrink further rather than wrap. */
    @Test
    fun labelsFitWithLargerText() {
        RuntimeEnvironment.setFontScale(1.3f)
        showApp(AppLanguage.PT)
        assertLabelsFit("Hoje", "Medicamentos", "Histórico", "Definições")
    }

    private fun showApp(language: AppLanguage) {
        val data = ReferenceFixtures.data.run { copy(settings = settings.copy(language = language)) }
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
    }

    /** A wrapped label has two lines; a clipped or ellipsised one is narrower than its text. */
    private fun assertLabelsFit(vararg labels: String) {
        val tab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
        labels.forEach { label ->
            val node = composeRule.onNode(hasText(label) and hasAnyAncestor(tab), useUnmergedTree = true).fetchSemanticsNode()
            val layouts = mutableListOf<TextLayoutResult>()
            node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
            val layout = layouts.single()
            assertEquals("$label lines", 1, layout.lineCount)
            val textWidth = layout.multiParagraph.intrinsics.maxIntrinsicWidth
            assertTrue("$label is $textWidth px wide in ${node.size.width} px", textWidth <= node.size.width)
        }
    }
}
