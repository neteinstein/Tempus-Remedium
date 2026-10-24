package io.github.neteinstein.remedioemdia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.neteinstein.remedioemdia.ui.theme.RemedioColors
import io.github.neteinstein.remedioemdia.ui.theme.RemedioType

/** Translucent "frosted glass" surface of the reference panels. */
fun Modifier.glass(shape: Shape): Modifier =
    this
        .background(RemedioColors.GlassFill, shape)
        .border(1.dp, RemedioColors.GlassStroke, shape)

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
            style = RemedioType.SectionTitle,
            color = RemedioColors.TextPrimary,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = meta,
            style = RemedioType.SectionMeta,
            color = RemedioColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}
