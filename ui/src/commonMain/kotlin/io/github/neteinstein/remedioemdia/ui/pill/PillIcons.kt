package io.github.neteinstein.remedioemdia.ui.pill

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import io.github.neteinstein.remedioemdia.core.model.PillColor
import io.github.neteinstein.remedioemdia.core.model.PillShape

/** A pill [height] tall; soft gels are wider than they are tall. */
@Composable
fun PillIcon(
    shape: PillShape,
    color: PillColor,
    height: Dp,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
) {
    Canvas(modifier.size(width = height * shape.aspectRatio, height = height)) {
        drawPill(shape, color, alpha = alpha)
    }
}

@Composable
fun Jar(
    shape: PillShape,
    color: PillColor,
    count: Int,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(width = height * JAR_ASPECT_RATIO, height = height)) {
        drawJar(shape, color, count)
    }
}
