package io.github.neteinstein.remedioemdia.android.widgets

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import io.github.neteinstein.remedioemdia.core.model.PillColor
import io.github.neteinstein.remedioemdia.core.model.PillShape
import io.github.neteinstein.remedioemdia.ui.pill.JAR_ASPECT_RATIO
import io.github.neteinstein.remedioemdia.ui.pill.aspectRatio
import io.github.neteinstein.remedioemdia.ui.pill.drawJar
import io.github.neteinstein.remedioemdia.ui.pill.drawPill
import kotlin.math.roundToInt

/**
 * Widgets cannot run Compose drawing code, so pills and jars are drawn with the
 * shared painters into bitmaps. Scale is capped to stay within the RemoteViews
 * bitmap budget.
 */
object PillBitmaps {
    private const val MAX_SCALE = 2f

    /** Room around a pill for the diagonal capsule and the shadow. */
    const val PILL_MARGIN = 1.2f

    fun scale(density: Float) = density.coerceAtMost(MAX_SCALE)

    fun pill(
        shape: PillShape,
        color: PillColor,
        heightDp: Float,
        density: Float,
        alpha: Float = 1f,
    ): Bitmap {
        val height = heightDp * scale(density)
        val width = height * shape.aspectRatio
        return render(width * PILL_MARGIN, height * PILL_MARGIN) {
            drawPill(
                shape,
                color,
                topLeft = Offset(width * (PILL_MARGIN - 1) / 2, height * (PILL_MARGIN - 1) / 2),
                size = Size(width, height),
                alpha = alpha,
            )
        }
    }

    fun jar(
        shape: PillShape,
        color: PillColor,
        count: Int,
        heightDp: Float,
        density: Float,
    ): Bitmap {
        val height = heightDp * scale(density)
        return render(height * JAR_ASPECT_RATIO, height) { drawJar(shape, color, count) }
    }

    private fun render(
        width: Float,
        height: Float,
        draw: DrawScope.() -> Unit,
    ): Bitmap {
        val image = ImageBitmap(width.roundToInt().coerceAtLeast(1), height.roundToInt().coerceAtLeast(1))
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), Size(image.width.toFloat(), image.height.toFloat()), draw)
        return image.asAndroidBitmap()
    }
}
