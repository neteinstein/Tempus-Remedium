package io.github.neteinstein.remedioemdia.android.screenshots

import androidx.compose.foundation.background
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
import io.github.neteinstein.remedioemdia.core.ReferenceFixtures
import io.github.neteinstein.remedioemdia.core.schedule.lowStock
import io.github.neteinstein.remedioemdia.core.schedule.planFor
import io.github.neteinstein.remedioemdia.ui.components.HojePanel
import io.github.neteinstein.remedioemdia.ui.components.ReporEmBrevePanel
import io.github.neteinstein.remedioemdia.ui.theme.RemedioColors
import io.github.neteinstein.remedioemdia.ui.theme.RemedioTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the reference panels with [ReferenceFixtures] on a 360dp/xxhdpi screen,
 * the same 1080px width as docs/design/reference/\*.png, so the output can be
 * compared with the reference side by side (see .claude/skills/match-reference-design).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "pt-rPT-w360dp-h808dp-xxhdpi")
class ReferenceScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun hojePanel() =
        capture("hoje_panel") {
            HojePanel(ReferenceFixtures.data.planFor(ReferenceFixtures.date))
        }

    @Test
    fun reporEmBrevePanel() =
        capture("repor_em_breve_panel") {
            ReporEmBrevePanel(ReferenceFixtures.data.lowStock())
        }

    private fun capture(
        name: String,
        content: @Composable () -> Unit,
    ) {
        composeRule.setContent {
            RemedioTheme {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(RemedioColors.Background)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                ) { content() }
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png", screenshotOptions)
    }
}
