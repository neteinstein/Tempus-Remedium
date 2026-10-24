package io.github.neteinstein.remedioemdia.android.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.neteinstein.remedioemdia.core.ReferenceFixtures
import io.github.neteinstein.remedioemdia.core.ReferenceFixtures.date
import io.github.neteinstein.remedioemdia.core.model.AppData
import io.github.neteinstein.remedioemdia.core.model.DoseStatus
import io.github.neteinstein.remedioemdia.core.model.TimeSlot
import io.github.neteinstein.remedioemdia.core.schedule.adherence
import io.github.neteinstein.remedioemdia.core.schedule.logDose
import io.github.neteinstein.remedioemdia.core.schedule.logSlot
import io.github.neteinstein.remedioemdia.core.schedule.lowStock
import io.github.neteinstein.remedioemdia.core.schedule.planFor
import io.github.neteinstein.remedioemdia.ui.screens.HistoryScreen
import io.github.neteinstein.remedioemdia.ui.screens.MedicationEditor
import io.github.neteinstein.remedioemdia.ui.screens.MedicationsScreen
import io.github.neteinstein.remedioemdia.ui.screens.SettingsScreen
import io.github.neteinstein.remedioemdia.ui.screens.TodayScreen
import io.github.neteinstein.remedioemdia.ui.theme.RemedioColors
import io.github.neteinstein.remedioemdia.ui.theme.RemedioTheme
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Whole screens, for review; the reference only covers the two panels. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "pt-rPT-w360dp-h800dp-xxhdpi")
class ScreensScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    /** MANHÃ taken: those pills fade, and Metformina's jar loses one. */
    private val morningTaken = ReferenceFixtures.data.logSlot(date, TimeSlot.MORNING, DoseStatus.TAKEN, 0L)

    @Test
    fun today() = capture("screen_today") { Today(ReferenceFixtures.data) }

    @Test
    fun todayAfterMorning() = capture("screen_today_morning_taken") { Today(morningTaken) }

    @Test
    fun todayEmpty() = capture("screen_today_empty") { Today(AppData()) }

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h800dp-xxhdpi")
    fun todayInEnglish() = capture("screen_today_en") { Today(ReferenceFixtures.data) }

    @Test
    fun medications() =
        capture("screen_medications") {
            MedicationsScreen(ReferenceFixtures.data.medications, onEdit = {}, onAdd = {})
        }

    @Test
    fun editor() =
        capture("screen_editor") {
            MedicationEditor(ReferenceFixtures.omega3, today = date, onSave = {}, onDelete = {}, onClose = {})
        }

    @Test
    fun history() {
        val data =
            morningTaken
                .logDose(ReferenceFixtures.omega3.id, date, TimeSlot.MIDDAY, DoseStatus.TAKEN, 0L)
                .logSlot(LocalDate(2026, 10, 21), TimeSlot.MORNING, DoseStatus.TAKEN, 0L)
                .logSlot(LocalDate(2026, 10, 21), TimeSlot.EVENING, DoseStatus.SKIPPED, 0L)
        capture("screen_history") { HistoryScreen(data.adherence(ReferenceFixtures.now, days = 3)) }
    }

    @Test
    fun settings() = capture("screen_settings") { SettingsScreen(ReferenceFixtures.data.settings, onChange = {}) }

    @Composable
    private fun Today(data: AppData) {
        TodayScreen(
            plan = data.planFor(date),
            lowStock = data.lowStock(),
            medications = data.activeMedications,
            onLogDose = { _, _, _ -> },
            onUndoDose = { _, _ -> },
            onLogSlot = {},
            onRefill = { _, _ -> },
            onAddMedication = {},
        )
    }

    private fun capture(
        name: String,
        content: @Composable () -> Unit,
    ) {
        composeRule.setContent {
            RemedioTheme {
                Box(Modifier.fillMaxSize().background(RemedioColors.Background)) { content() }
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png", screenshotOptions)
    }
}
