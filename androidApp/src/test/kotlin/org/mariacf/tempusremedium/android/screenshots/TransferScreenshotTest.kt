package org.mariacf.tempusremedium.android.screenshots

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.datetime.DayOfWeek
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mariacf.tempusremedium.core.CourseFixtures
import org.mariacf.tempusremedium.core.ReferenceFixtures
import org.mariacf.tempusremedium.core.model.AppData
import org.mariacf.tempusremedium.core.model.Frequency
import org.mariacf.tempusremedium.core.schedule.upsertMedication
import org.mariacf.tempusremedium.ui.FileTransfer
import org.mariacf.tempusremedium.ui.components.brandBackground
import org.mariacf.tempusremedium.ui.screens.ExportMedicationsDialog
import org.mariacf.tempusremedium.ui.screens.ImportMedicationsDialog
import org.mariacf.tempusremedium.ui.screens.MedicationTransferSection
import org.mariacf.tempusremedium.ui.screens.SettingsScreen
import org.mariacf.tempusremedium.ui.sheet.SHEET_HEIGHT
import org.mariacf.tempusremedium.ui.sheet.SHEET_WIDTH
import org.mariacf.tempusremedium.ui.sheet.SheetPage
import org.mariacf.tempusremedium.ui.sheet.sheetPages
import org.mariacf.tempusremedium.ui.sheet.sheetStyle
import org.mariacf.tempusremedium.ui.sheet.sheetTexts
import org.mariacf.tempusremedium.ui.theme.TempusTheme
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Exportar e importar in Definições, its dialogs, and the pages of the medications sheet (PDF). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "pt-rPT-w360dp-h800dp-xxhdpi")
class TransferScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    /** The reference medications, a course, a weekly one and notes: every kind of line on the sheet. */
    private val sheetData: AppData =
        CourseFixtures.data
            .upsertMedication(
                ReferenceFixtures.metformina.copy(notes = "Tomar durante a refeição, com um copo de água."),
            ).upsertMedication(
                ReferenceFixtures.vitaminaD.copy(
                    schedule =
                        ReferenceFixtures.vitaminaD.schedule.copy(
                            frequency = Frequency.Weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)),
                        ),
                ),
            )

    @Test
    @Config(qualifiers = "pt-rPT-w360dp-h1300dp-xxhdpi")
    fun settingsSection() =
        capture("screen_settings_transfer") {
            SettingsScreen(
                ReferenceFixtures.data.settings,
                onChange = {},
                transfer = {
                    MedicationTransferSection(
                        medications = ReferenceFixtures.data.medications,
                        settings = ReferenceFixtures.data.settings,
                        today = ReferenceFixtures.date,
                        files = NoFiles,
                        onImport = {},
                    )
                },
            )
        }

    @Test
    fun exportDialog() =
        capture("dialog_export") {
            ExportMedicationsDialog(ReferenceFixtures.data.medications, onDismiss = {}, onExport = { _, _ -> })
        }

    @Test
    fun importDialogDark() =
        capture("dialog_import_dark", dark = true) {
            ImportMedicationsDialog(ReferenceFixtures.data.medications, onDismiss = {}, onConfirm = {})
        }

    @Test
    fun importDialogSingle() =
        capture("dialog_import_single") {
            ImportMedicationsDialog(listOf(ReferenceFixtures.omega3), onDismiss = {}, onConfirm = {})
        }

    @Test
    @Config(qualifiers = "pt-rPT-w595dp-h842dp-xhdpi")
    fun sheetFirstPage() = captureSheet("pdf_page_1", page = 0)

    @Test
    @Config(qualifiers = "pt-rPT-w595dp-h842dp-xhdpi")
    fun sheetSecondPage() = captureSheet("pdf_page_2", page = 1)

    @Test
    @Config(qualifiers = "pt-rPT-w595dp-h842dp-xhdpi")
    fun sheetThirdPage() = captureSheet("pdf_page_3", page = 2)

    @Test
    @Config(qualifiers = "en-rUS-w595dp-h842dp-xhdpi")
    fun sheetInEnglish() = captureSheet("pdf_page_1_en", page = 0)

    /** A page as the PDF has it, drawn at the screen's density instead of in points. */
    private fun captureSheet(
        name: String,
        page: Int,
    ) {
        composeRule.setContent {
            TempusTheme {
                val pages =
                    sheetPages(
                        sheetTexts(sheetData.activeMedications, sheetData.settings, ReferenceFixtures.date),
                        sheetStyle(),
                        rememberTextMeasurer(),
                    )
                SheetCanvas(pages[page])
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png", screenshotOptions)
    }

    @Composable
    private fun SheetCanvas(page: SheetPage) {
        Canvas(Modifier.size(SHEET_WIDTH.dp, SHEET_HEIGHT.dp)) {
            scale(density, pivot = Offset.Zero) { page() }
        }
    }

    private fun capture(
        name: String,
        dark: Boolean = false,
        content: @Composable () -> Unit,
    ) {
        composeRule.setContent {
            TempusTheme(darkTheme = dark) {
                Box(Modifier.fillMaxSize().brandBackground(TempusTheme.colors)) { content() }
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png", screenshotOptions)
    }

    private object NoFiles : FileTransfer {
        override suspend fun save(
            name: String,
            type: String,
            bytes: ByteArray,
        ) = Unit

        override suspend fun open(): String? = null

        override suspend fun pdf(pages: List<SheetPage>) = ByteArray(0)
    }
}
