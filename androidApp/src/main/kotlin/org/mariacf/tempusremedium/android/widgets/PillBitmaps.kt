package org.mariacf.tempusremedium.android.widgets

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
import org.mariacf.tempusremedium.core.model.PillColor
import org.mariacf.tempusremedium.core.model.PillShape
import org.mariacf.tempusremedium.core.model.StockDisplay
import org.mariacf.tempusremedium.ui.pill.JAR_ASPECT_RATIO
import org.mariacf.tempusremedium.ui.pill.aspectRatio
import org.mariacf.tempusremedium.ui.pill.drawBlister
import org.mariacf.tempusremedium.ui.pill.drawJar
import org.mariacf.tempusremedium.ui.pill.drawPill
import org.mariacf.tempusremedium.ui.theme.TempusPalette
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

    /** A jar or a blister pack, following the user's [display] setting. */
    fun stock(
        shape: PillShape,
        color: PillColor,
        count: Int,
        display: StockDisplay,
        palette: TempusPalette,
        heightDp: Float,
        density: Float,
    ): Bitmap {
        val height = heightDp * scale(density)
        return render(height * JAR_ASPECT_RATIO, height) {
            when (display) {
                StockDisplay.JARS -> drawJar(shape, color, count, palette.jar)
                StockDisplay.BLISTERS -> drawBlister(shape, color, count, palette.blister)
            }
        }
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
