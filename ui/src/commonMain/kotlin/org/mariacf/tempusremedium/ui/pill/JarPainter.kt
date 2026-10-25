package org.mariacf.tempusremedium.ui.pill

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import org.mariacf.tempusremedium.core.model.PillColor
import org.mariacf.tempusremedium.core.model.PillShape
import org.mariacf.tempusremedium.ui.theme.JarColors

/** Width / height of a jar (and of a blister pack, so both fill the same space). */
const val JAR_ASPECT_RATIO = 0.68f

/** Pills drawn in a jar: 3 per row, at most 4 rows. */
const val JAR_CAPACITY = 12

private const val COLUMNS = 3
private val columnCenters = floatArrayOf(0.23f, 0.5f, 0.77f)

// Fixed so the jar looks hand-filled but renders identically every time.
private val roundRotations = floatArrayOf(0f, 80f, 5f, 70f, -5f, 75f, 10f, 85f, -10f, 65f, 0f, 80f)
private val capsuleRotations = floatArrayOf(-55f, -40f, -45f, -35f, -50f, -42f, -58f, -38f, -48f, -44f, -52f, -36f)
private val smallRotations = floatArrayOf(0f, 8f, -6f, 4f, -8f, 6f, -3f, 9f, -7f, 3f, -5f, 7f)

/**
 * Glass jar with a striped lid and up to [JAR_CAPACITY] pills piled at the bottom,
 * as in docs/design/reference/repor-em-breve.png.
 */
fun DrawScope.drawJar(
    shape: PillShape,
    color: PillColor,
    count: Int,
    colors: JarColors,
    size: Size = this.size,
) {
    val w = size.width
    val h = size.height

    val bodyTop = h * 0.115f
    val body =
        RoundRect(
            rect = Rect(0f, bodyTop, w, h),
            topLeft = CornerRadius(w * 0.14f),
            topRight = CornerRadius(w * 0.14f),
            bottomRight = CornerRadius(w * 0.2f),
            bottomLeft = CornerRadius(w * 0.2f),
        )
    val bodyPath = Path().apply { addRoundRect(body) }
    drawPath(bodyPath, colors.body)
    drawPath(bodyPath, colors.stroke, style = Stroke(width = (w * 0.013f).coerceAtLeast(1f)))

    // Reflection on the left side of the glass.
    val highlightWidth = w * 0.042f
    drawRoundRect(
        colors.highlight,
        topLeft = Offset(w * 0.065f, bodyTop + h * 0.085f),
        size = Size(highlightWidth, h * 0.63f),
        cornerRadius = CornerRadius(highlightWidth / 2),
    )

    val unit = w * 0.243f
    val pitch = w * 0.255f
    val bottomRowCenter = h - w * 0.21f
    repeat(count.coerceIn(0, JAR_CAPACITY)) { index ->
        val row = index / COLUMNS
        val center = Offset(w * columnCenters[index % COLUMNS], bottomRowCenter - row * pitch)
        val boxWidth = unit * shape.aspectRatio
        val rotation =
            when (shape) {
                PillShape.ROUND_SCORED -> roundRotations[index]
                PillShape.CAPSULE, PillShape.INJECTION -> capsuleRotations[index]
                else -> smallRotations[index]
            }
        drawPill(
            shape = shape,
            color = color,
            topLeft = Offset(center.x - boxWidth / 2, center.y - unit / 2),
            size = Size(boxWidth, unit),
            rotationDegrees = rotation,
        )
    }

    // Lid, drawn last so it sits over the rim.
    val lidWidth = w * 0.76f
    val lidHeight = h * 0.13f
    val lid =
        RoundRect(
            rect = Rect(Offset((w - lidWidth) / 2, 0f), Size(lidWidth, lidHeight)),
            cornerRadius = CornerRadius(w * 0.05f),
        )
    val lidPath = Path().apply { addRoundRect(lid) }
    drawPath(lidPath, colors.lidBase)
    clipPath(lidPath) {
        val stripes = 13
        val stripePitch = lidWidth / stripes
        repeat(stripes) { i ->
            drawRect(
                colors.lidStripe,
                topLeft = Offset(lid.left + stripePitch * i + stripePitch * 0.3f, 0f),
                size = Size(stripePitch * 0.45f, lidHeight),
            )
        }
    }
    drawPath(lidPath, colors.lidStroke, style = Stroke(width = (w * 0.01f).coerceAtLeast(1f)))
}
