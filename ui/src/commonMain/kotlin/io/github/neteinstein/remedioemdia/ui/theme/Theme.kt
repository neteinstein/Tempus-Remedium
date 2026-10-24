package io.github.neteinstein.remedioemdia.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.neteinstein.remedioemdia.ui.resources.Res
import io.github.neteinstein.remedioemdia.ui.resources.inter_bold
import io.github.neteinstein.remedioemdia.ui.resources.inter_medium
import io.github.neteinstein.remedioemdia.ui.resources.inter_regular
import io.github.neteinstein.remedioemdia.ui.resources.inter_semibold
import org.jetbrains.compose.resources.Font

/** Colours sampled from docs/design/reference (see REFERENCE.md for the sample points). */
object RemedioColors {
    val TextPrimary = Color(0xFFFDFDFB)
    val TextSecondary = Color(0xFFD8D1CD)
    val Amber = Color(0xFFF8E29F)
    val GlassFill = Color.White.copy(alpha = 0.10f)
    val GlassStroke = Color.White.copy(alpha = 0.30f)
    val Divider = Color.White.copy(alpha = 0.35f)

    /** Score lines and outlines drawn on pills. */
    val Ink = Color(0xFF3C4043)
    val LidBase = Color(0xFFF3F3EF)
    val LidStripe = Color(0xFFD6E2F0)

    /** App background, standing in for the photo behind the reference panels. */
    val Background =
        Brush.verticalGradient(
            0f to Color(0xFF51443B),
            0.55f to Color(0xFF2E2520),
            1f to Color(0xFF15110E),
        )
    val Surface = Color(0xFF2B231E)
}

@Composable
fun interFontFamily(): FontFamily =
    FontFamily(
        Font(Res.font.inter_regular, FontWeight.Normal),
        Font(Res.font.inter_medium, FontWeight.Medium),
        Font(Res.font.inter_semibold, FontWeight.SemiBold),
        Font(Res.font.inter_bold, FontWeight.Bold),
    )

/** Text styles of the reference panels, sized for a 360dp-wide screen. */
object RemedioType {
    val SectionTitle = TextStyle(fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.6.sp)
    val SectionMeta = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal)
    val SlotLabel = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
    val SlotTime = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal)
    val Legend = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal)
    val JarName = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium)
    val JarCount = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
}

private val colorScheme =
    darkColorScheme(
        primary = RemedioColors.Amber,
        onPrimary = Color(0xFF2A2116),
        primaryContainer = Color(0xFF5A4A2E),
        onPrimaryContainer = RemedioColors.Amber,
        secondary = Color(0xFF8EE8E2),
        onSecondary = Color(0xFF00201E),
        background = Color(0xFF1D1714),
        onBackground = RemedioColors.TextPrimary,
        surface = RemedioColors.Surface,
        onSurface = RemedioColors.TextPrimary,
        surfaceVariant = Color(0xFF3A302A),
        onSurfaceVariant = RemedioColors.TextSecondary,
        surfaceContainer = Color(0xFF2F2621),
        surfaceContainerHigh = Color(0xFF382E28),
        surfaceContainerHighest = Color(0xFF41362F),
        surfaceContainerLow = Color(0xFF261F1B),
        outline = Color(0xFF8C7F77),
        outlineVariant = Color(0xFF51463F),
        error = Color(0xFFFFB4AB),
    )

@Composable
fun RemedioTheme(content: @Composable () -> Unit) {
    val family = interFontFamily()
    val base = Typography()
    MaterialTheme(
        colorScheme = colorScheme,
        typography =
            Typography(
                displayLarge = base.displayLarge.copy(fontFamily = family),
                displayMedium = base.displayMedium.copy(fontFamily = family),
                displaySmall = base.displaySmall.copy(fontFamily = family),
                headlineLarge = base.headlineLarge.copy(fontFamily = family),
                headlineMedium = base.headlineMedium.copy(fontFamily = family),
                headlineSmall = base.headlineSmall.copy(fontFamily = family),
                titleLarge = base.titleLarge.copy(fontFamily = family),
                titleMedium = base.titleMedium.copy(fontFamily = family),
                titleSmall = base.titleSmall.copy(fontFamily = family),
                bodyLarge = base.bodyLarge.copy(fontFamily = family),
                bodyMedium = base.bodyMedium.copy(fontFamily = family),
                bodySmall = base.bodySmall.copy(fontFamily = family),
                labelLarge = base.labelLarge.copy(fontFamily = family),
                labelMedium = base.labelMedium.copy(fontFamily = family),
                labelSmall = base.labelSmall.copy(fontFamily = family),
            ),
    ) {
        CompositionLocalProvider(LocalContentColor provides RemedioColors.TextPrimary, content = content)
    }
}
