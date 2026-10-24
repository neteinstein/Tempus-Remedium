package io.github.neteinstein.remedioemdia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.neteinstein.remedioemdia.ui.theme.RemedioPalette
import io.github.neteinstein.remedioemdia.ui.theme.RemedioTheme

/**
 * Frosted surface of the panels and cards (white glass on light, smoked glass on dark).
 * Shadow renderers assume an opaque caster and leave a light patch under a translucent one,
 * so elevated cards get an opaque base under the glass.
 */
fun Modifier.glass(
    shape: Shape,
    palette: RemedioPalette,
    strong: Boolean = false,
    elevation: Dp = 0.dp,
): Modifier =
    this
        .then(
            if (elevation > 0.dp) {
                Modifier
                    .shadow(
                        elevation,
                        shape,
                        ambientColor = palette.shadow.copy(alpha = 0.08f),
                        spotColor = palette.shadow.copy(alpha = 0.22f),
                    ).background(palette.background, shape)
            } else {
                Modifier
            },
        ).background(if (strong) palette.surfaceStrong else palette.surface, shape)
        .border(1.dp, if (strong) palette.line else palette.surfaceBorder, shape)

/** The page background: soft colour blobs on a near-white (or near-black) ground. */
fun Modifier.brandBackground(palette: RemedioPalette): Modifier =
    drawBehind {
        drawRect(palette.background)

        fun blob(
            color: Color,
            x: Float,
            y: Float,
            radius: Float,
            alpha: Float,
        ) = drawRect(
            Brush.radialGradient(
                listOf(color.copy(alpha = alpha), Color.Transparent),
                center = Offset(size.width * x, size.height * y),
                radius = size.width * radius,
            ),
        )
        val strength = if (palette.isDark) 0.55f else 0.85f
        blob(palette.blobs[0], 0.05f, 0.02f, 0.95f, strength)
        blob(palette.blobs[2], 0.95f, 0.18f, 0.75f, strength * 0.8f)
        blob(palette.blobs[3], 0.2f, 0.62f, 0.8f, strength * 0.55f)
        blob(palette.blobs[1], 0.9f, 0.95f, 0.9f, strength * 0.6f)
    }

/** "HOJE ……… qui., 22 · 5 doses": letter-spaced title on the left, detail on the right. */
@Composable
fun SectionHeader(
    title: String,
    meta: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title.uppercase(),
            style = RemedioTheme.type.sectionTitle,
            color = RemedioTheme.colors.ink,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = meta,
            style = RemedioTheme.type.sectionMeta,
            color = RemedioTheme.colors.inkMute,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

/** "● QUINTA-FEIRA, 22 DE OUTUBRO": small uppercase label with a dot. */
@Composable
fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).background(RemedioTheme.colors.dot, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(text.uppercase(), style = RemedioTheme.type.eyebrow, color = RemedioTheme.colors.inkSoft)
    }
}

/** Eyebrow plus a Fraunces title, at the top of each screen. */
@Composable
fun ScreenHeader(
    eyebrow: String,
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(horizontal = 8.dp)) {
        Eyebrow(eyebrow)
        Spacer(Modifier.height(10.dp))
        Text(title, style = RemedioTheme.type.title.copy(fontSize = RemedioTheme.type.headline.fontSize), color = RemedioTheme.colors.ink)
    }
}

/** Primary action: a pill-shaped button filled with the brand gradient. */
@Composable
fun BrandButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val palette = RemedioTheme.colors
    Box(
        modifier
            .alpha(if (enabled) 1f else 0.45f)
            .shadow(if (enabled) 10.dp else 0.dp, CircleShape, spotColor = palette.brand[1].copy(alpha = 0.45f))
            .clip(CircleShape)
            .background(palette.brandBrush)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = RemedioTheme.type.button, color = Color.White, maxLines = 1)
    }
}

/** Secondary action: text in the accent colour on a soft accent pill. */
@Composable
fun SoftButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = RemedioTheme.colors
    Box(
        modifier
            .clip(CircleShape)
            .background(palette.accentSoft)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = RemedioTheme.type.button, color = palette.accent, maxLines = 1)
    }
}

/** A frosted card holding rows separated by hairlines. */
@Composable
fun GroupCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier
            .fillMaxWidth()
            .glass(shape, RemedioTheme.colors, elevation = 6.dp)
            .clip(shape),
        verticalArrangement = Arrangement.Top,
        content = content,
    )
}

@Composable
fun GroupDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(RemedioTheme.colors.line),
    )
}
