package org.mariacf.tempusremedium.android.screenshots

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mariacf.tempusremedium.core.ReferenceFixtures
import org.mariacf.tempusremedium.core.model.StockDisplay
import org.mariacf.tempusremedium.core.schedule.lowStock
import org.mariacf.tempusremedium.core.schedule.planFor
import org.mariacf.tempusremedium.ui.components.HojePanel
import org.mariacf.tempusremedium.ui.components.ReporEmBrevePanel
import org.mariacf.tempusremedium.ui.components.brandBackground
import org.mariacf.tempusremedium.ui.theme.TempusTheme
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the reference panels with [ReferenceFixtures] on a 360dp/xxhdpi screen, the same
 * 1080px width as docs/design/reference/\*.png, so the output can be compared with the
 * reference side by side (see .claude/skills/match-reference-design). The dark variants are
 * the closest to the reference's dark photo background.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "pt-rPT-w360dp-h808dp-xxhdpi")
class ReferenceScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun hojePanel() = capture("hoje_panel", dark = false) { HojePanel(ReferenceFixtures.data.planFor(ReferenceFixtures.date)) }

    @Test
    fun hojePanelDark() = capture("hoje_panel_dark", dark = true) { HojePanel(ReferenceFixtures.data.planFor(ReferenceFixtures.date)) }

    @Test
    fun reporEmBrevePanel() = capture("repor_em_breve_panel", dark = false) { ReporEmBrevePanel(ReferenceFixtures.data.lowStock()) }

    @Test
    fun reporEmBrevePanelDark() = capture("repor_em_breve_panel_dark", dark = true) { ReporEmBrevePanel(ReferenceFixtures.data.lowStock()) }

    @Test
    fun reporEmBreveBlisters() =
        capture("repor_em_breve_blisters", dark = false) {
            ReporEmBrevePanel(ReferenceFixtures.data.lowStock(), display = StockDisplay.BLISTERS)
        }

    @Test
    fun reporEmBreveBlistersDark() =
        capture("repor_em_breve_blisters_dark", dark = true) {
            ReporEmBrevePanel(ReferenceFixtures.data.lowStock(), display = StockDisplay.BLISTERS)
        }

    private fun capture(
        name: String,
        dark: Boolean,
        content: @Composable () -> Unit,
    ) {
        composeRule.setContent {
            TempusTheme(darkTheme = dark) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .brandBackground(TempusTheme.colors)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                ) { content() }
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png", screenshotOptions)
    }
}
