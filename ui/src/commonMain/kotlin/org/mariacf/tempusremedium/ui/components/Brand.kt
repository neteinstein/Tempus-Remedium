package org.mariacf.tempusremedium.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.mariacf.tempusremedium.ui.theme.TempusTheme

/** The app mark: a white scored tablet on the brand gradient (same as the launcher icon). */
@Composable
fun AppMark(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    val palette = TempusTheme.colors
    val shape = RoundedCornerShape(size * 0.32f)
    Box(
        modifier
            .size(size)
            .shadow(12.dp, shape, spotColor = palette.brand[1].copy(alpha = 0.5f))
            .clip(shape)
            .background(palette.brandBrush)
            .padding(size * 0.2f),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = this.size.minDimension / 2
            drawCircle(Color.White, radius)
            val half = radius * 0.5f
            drawLine(
                palette.brand[1],
                start = center + Offset(-half, half),
                end = center + Offset(half, -half),
                strokeWidth = radius * 0.17f,
            )
        }
    }
}
