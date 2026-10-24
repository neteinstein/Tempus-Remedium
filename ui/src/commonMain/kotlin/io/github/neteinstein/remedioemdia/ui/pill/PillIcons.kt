package io.github.neteinstein.remedioemdia.ui.pill

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import io.github.neteinstein.remedioemdia.core.model.PillColor
import io.github.neteinstein.remedioemdia.core.model.PillShape
import io.github.neteinstein.remedioemdia.core.model.StockDisplay
import io.github.neteinstein.remedioemdia.ui.theme.RemedioTheme

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

/** The units left of a medication, as a jar or a blister pack depending on [display]. */
@Composable
fun StockIcon(
    shape: PillShape,
    color: PillColor,
    count: Int,
    height: Dp,
    display: StockDisplay,
    modifier: Modifier = Modifier,
) {
    val colors = RemedioTheme.colors
    Canvas(modifier.size(width = height * JAR_ASPECT_RATIO, height = height)) {
        when (display) {
            StockDisplay.JARS -> drawJar(shape, color, count, colors.jar)
            StockDisplay.BLISTERS -> drawBlister(shape, color, count, colors.blister)
        }
    }
}
