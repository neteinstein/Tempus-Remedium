// The theme is the one place that defines CompositionLocals.
@file:Suppress("ktlint:compose:compositionlocal-allowlist")

package io.github.neteinstein.remedioemdia.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.neteinstein.remedioemdia.ui.resources.Res
import io.github.neteinstein.remedioemdia.ui.resources.fraunces_medium
import io.github.neteinstein.remedioemdia.ui.resources.fraunces_medium_italic
import io.github.neteinstein.remedioemdia.ui.resources.fraunces_semibold
import io.github.neteinstein.remedioemdia.ui.resources.fraunces_semibold_italic
import io.github.neteinstein.remedioemdia.ui.resources.jakarta_bold
import io.github.neteinstein.remedioemdia.ui.resources.jakarta_extrabold
import io.github.neteinstein.remedioemdia.ui.resources.jakarta_medium
import io.github.neteinstein.remedioemdia.ui.resources.jakarta_regular
import io.github.neteinstein.remedioemdia.ui.resources.jakarta_semibold
import org.jetbrains.compose.resources.Font

/** Colours of the glass jar in REPOR EM BREVE. */
@Immutable
data class JarColors(
    val body: Color,
    val stroke: Color,
    val highlight: Color,
    val lidBase: Color,
    val lidStripe: Color,
    val lidStroke: Color,
)

/** Colours of the blister pack in REPOR EM BREVE. */
@Immutable
data class BlisterColors(
    val foilTop: Color,
    val foilBottom: Color,
    val foilStroke: Color,
    val dome: Color,
    val domeStroke: Color,
    val domeShine: Color,
    val hole: Color,
    val holeEdge: Color,
)

/**
 * The palette of the author's site, mariacf.pt ("Saúde em Família"): calm blues, frosted white
 * surfaces and soft background blobs, with a dark variant. Pill colours stay those of the
 * design reference (see PillPalette.kt).
 */
@Immutable
data class RemedioPalette(
    val isDark: Boolean,
    val background: Color,
    val background2: Color,
    val blobs: List<Color>,
    val ink: Color,
    val inkSoft: Color,
    val inkMute: Color,
    val line: Color,
    val surface: Color,
    val surfaceStrong: Color,
    val surfaceBorder: Color,
    /** Links, highlighted words, selected states. */
    val accent: Color,
    val accentSoft: Color,
    /** The small dot before eyebrow labels. */
    val dot: Color,
    /** Gradient of the logo and primary buttons. */
    val brand: List<Color>,
    /** "Restam 3": the amber of the reference, darkened on light backgrounds for contrast. */
    val stock: Color,
    val stockSoft: Color,
    val danger: Color,
    val shadow: Color,
    val jar: JarColors,
    val blister: BlisterColors,
) {
    val brandBrush: Brush get() = Brush.linearGradient(brand)

    companion object {
        private val Brand = listOf(Color(0xFF026A9F), Color(0xFF125A98), Color(0xFF00668B))

        val Light =
            RemedioPalette(
                isDark = false,
                background = Color(0xFFF9FAFC),
                background2 = Color(0xFFECF1F6),
                blobs = listOf(Color(0xFFC6E1FF), Color(0xFFDCE6F2), Color(0xFFB8E6FE), Color(0xFFCFEEFA)),
                ink = Color(0xFF1A232C),
                inkSoft = Color(0xFF47515C),
                inkMute = Color(0xFF5B646F),
                line = Color(0x1F1E2F41),
                surface = Color(0xB8FFFFFF),
                surfaceStrong = Color(0xEBFFFFFF),
                surfaceBorder = Color(0xE6FFFFFF),
                accent = Color(0xFF1A609E),
                accentSoft = Color(0xFFDEEDFE),
                dot = Color(0xFF049FD6),
                brand = Brand,
                stock = Color(0xFF9A6200),
                stockSoft = Color(0xFFFBEDD0),
                danger = Color(0xFFC2413A),
                shadow = Color(0xFF014F8A),
                jar =
                    JarColors(
                        body = Color(0x0D1A232C),
                        stroke = Color(0x4D1E2F41),
                        highlight = Color(0xD9FFFFFF),
                        lidBase = Color(0xFFF4F7FA),
                        lidStripe = Color(0xFFC9DDF2),
                        lidStroke = Color(0x2E1E2F41),
                    ),
                blister =
                    BlisterColors(
                        foilTop = Color(0xFFF1F4F8),
                        foilBottom = Color(0xFFD9E0E8),
                        foilStroke = Color(0x2E1E2F41),
                        dome = Color(0xB3F4F8FC),
                        domeStroke = Color(0x331E2F41),
                        domeShine = Color(0xFFFFFFFF),
                        hole = Color(0xFFC9D2DC),
                        holeEdge = Color(0xFFA9B5C2),
                    ),
            )

        val Dark =
            RemedioPalette(
                isDark = true,
                background = Color(0xFF0B1015),
                background2 = Color(0xFF10171E),
                blobs = listOf(Color(0xFF0E3F6A), Color(0xFF263151), Color(0xFF023D55), Color(0xFF232D47)),
                ink = Color(0xFFEBEFF4),
                inkSoft = Color(0xFFC0C8D1),
                inkMute = Color(0xFF989FA8),
                line = Color(0x1AEBEFF4),
                surface = Color(0x8C1D252D),
                surfaceStrong = Color(0xD9212A33),
                surfaceBorder = Color(0x14FFFFFF),
                accent = Color(0xFF89C3FE),
                accentSoft = Color(0x2489C3FE),
                dot = Color(0xFF049FD6),
                brand = Brand,
                stock = Color(0xFFFFC861),
                stockSoft = Color(0x24FFC861),
                danger = Color(0xFFFF8A80),
                shadow = Color(0xFF000000),
                jar =
                    JarColors(
                        body = Color(0x0FFFFFFF),
                        stroke = Color(0x80FFFFFF),
                        highlight = Color(0x4DFFFFFF),
                        lidBase = Color(0xFFF3F3EF),
                        lidStripe = Color(0xFFD6E2F0),
                        lidStroke = Color(0x00000000),
                    ),
                blister =
                    BlisterColors(
                        foilTop = Color(0xFF4A5663),
                        foilBottom = Color(0xFF2C3540),
                        foilStroke = Color(0x29FFFFFF),
                        dome = Color(0x14FFFFFF),
                        domeStroke = Color(0x40FFFFFF),
                        domeShine = Color(0x40FFFFFF),
                        hole = Color(0xFF161C22),
                        holeEdge = Color(0xFF55616E),
                    ),
            )
    }
}

/** Text styles: Fraunces for display text, Plus Jakarta Sans for everything else, as on mariacf.pt. */
@Immutable
data class RemedioTypography(
    val display: FontFamily,
    val body: FontFamily,
) {
    /** "Boa tarde," */
    val headline =
        TextStyle(fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-0.6).sp)

    /** "faltam 3 doses.": the italic, coloured part of a headline. */
    val headlineAccent = headline.copy(fontWeight = FontWeight.Medium, fontStyle = FontStyle.Italic)
    val title =
        TextStyle(fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.3).sp)
    val stat =
        TextStyle(fontFamily = display, fontWeight = FontWeight.Medium, fontStyle = FontStyle.Italic, fontSize = 56.sp, lineHeight = 60.sp)

    /** Small uppercase label above a headline ("● QUINTA-FEIRA, 22 DE OUTUBRO"). */
    val eyebrow = TextStyle(fontFamily = body, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, letterSpacing = 1.4.sp)

    // The reference panels.
    val sectionTitle = TextStyle(fontFamily = body, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 2.6.sp)
    val sectionMeta = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 13.5.sp)
    val slotLabel = TextStyle(fontFamily = body, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, letterSpacing = 1.sp)
    val slotTime = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    val legend = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 13.5.sp)
    val jarName = TextStyle(fontFamily = body, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    val jarCount = TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

    val bodyText = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp)
    val bodySmall = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp)
    val rowTitle = TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp)
    val button = TextStyle(fontFamily = body, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
}

private val LocalRemedioPalette = staticCompositionLocalOf { RemedioPalette.Light }
private val LocalRemedioTypography = staticCompositionLocalOf { RemedioTypography(FontFamily.Serif, FontFamily.SansSerif) }

object RemedioTheme {
    val colors: RemedioPalette
        @Composable @ReadOnlyComposable
        get() = LocalRemedioPalette.current

    val type: RemedioTypography
        @Composable @ReadOnlyComposable
        get() = LocalRemedioTypography.current
}

@Composable
fun frauncesFamily(): FontFamily =
    FontFamily(
        Font(Res.font.fraunces_medium, FontWeight.Medium),
        Font(Res.font.fraunces_semibold, FontWeight.SemiBold),
        Font(Res.font.fraunces_medium_italic, FontWeight.Medium, FontStyle.Italic),
        Font(Res.font.fraunces_semibold_italic, FontWeight.SemiBold, FontStyle.Italic),
    )

@Composable
fun jakartaFamily(): FontFamily =
    FontFamily(
        Font(Res.font.jakarta_regular, FontWeight.Normal),
        Font(Res.font.jakarta_medium, FontWeight.Medium),
        Font(Res.font.jakarta_semibold, FontWeight.SemiBold),
        Font(Res.font.jakarta_bold, FontWeight.Bold),
        Font(Res.font.jakarta_extrabold, FontWeight.ExtraBold),
    )

private fun RemedioPalette.colorScheme(): ColorScheme =
    if (isDark) {
        darkColorScheme(
            primary = accent,
            onPrimary = Color(0xFF00315A),
            primaryContainer = Color(0xFF14476F),
            onPrimaryContainer = Color(0xFFD4E8FF),
            secondary = Color(0xFF7DCAEB),
            onSecondary = Color(0xFF003547),
            background = background,
            onBackground = ink,
            surface = Color(0xFF151C23),
            onSurface = ink,
            surfaceVariant = Color(0xFF212A33),
            onSurfaceVariant = inkSoft,
            surfaceContainerLowest = Color(0xFF0B1015),
            surfaceContainerLow = Color(0xFF131A21),
            surfaceContainer = Color(0xFF172029),
            surfaceContainerHigh = Color(0xFF1D2630),
            surfaceContainerHighest = Color(0xFF242E39),
            outline = Color(0xFF5E6A76),
            outlineVariant = Color(0xFF2E3843),
            error = danger,
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            primaryContainer = accentSoft,
            onPrimaryContainer = Color(0xFF0B3B63),
            secondary = Color(0xFF066786),
            onSecondary = Color.White,
            background = background,
            onBackground = ink,
            surface = Color.White,
            onSurface = ink,
            surfaceVariant = background2,
            onSurfaceVariant = inkSoft,
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = Color(0xFFF7F9FC),
            surfaceContainer = Color(0xFFF2F5F9),
            surfaceContainerHigh = Color(0xFFECF1F6),
            surfaceContainerHighest = Color(0xFFE5EBF2),
            outline = Color(0xFF8792A0),
            outlineVariant = Color(0xFFD5DDE6),
            error = danger,
        )
    }

@Composable
fun RemedioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) RemedioPalette.Dark else RemedioPalette.Light
    val display = frauncesFamily()
    val body = jakartaFamily()
    val type = remember(display, body) { RemedioTypography(display, body) }
    val base = Typography()
    val typography =
        remember(body) {
            Typography(
                displayLarge = base.displayLarge.copy(fontFamily = display),
                displayMedium = base.displayMedium.copy(fontFamily = display),
                displaySmall = base.displaySmall.copy(fontFamily = display),
                headlineLarge = base.headlineLarge.copy(fontFamily = display),
                headlineMedium = base.headlineMedium.copy(fontFamily = display),
                headlineSmall = base.headlineSmall.copy(fontFamily = display),
                titleLarge = base.titleLarge.copy(fontFamily = display, fontWeight = FontWeight.SemiBold),
                titleMedium = base.titleMedium.copy(fontFamily = body, fontWeight = FontWeight.SemiBold),
                titleSmall = base.titleSmall.copy(fontFamily = body, fontWeight = FontWeight.SemiBold),
                bodyLarge = base.bodyLarge.copy(fontFamily = body),
                bodyMedium = base.bodyMedium.copy(fontFamily = body),
                bodySmall = base.bodySmall.copy(fontFamily = body),
                labelLarge = base.labelLarge.copy(fontFamily = body, fontWeight = FontWeight.Bold),
                labelMedium = base.labelMedium.copy(fontFamily = body, fontWeight = FontWeight.SemiBold),
                labelSmall = base.labelSmall.copy(fontFamily = body, fontWeight = FontWeight.SemiBold),
            )
        }
    MaterialTheme(colorScheme = palette.colorScheme(), typography = typography) {
        CompositionLocalProvider(
            LocalRemedioPalette provides palette,
            LocalRemedioTypography provides type,
            LocalContentColor provides palette.ink,
            content = content,
        )
    }
}
