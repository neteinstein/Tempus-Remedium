package org.mariacf.tempusremedium.ui.pill

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import org.mariacf.tempusremedium.core.model.PillColor
import org.mariacf.tempusremedium.core.model.PillShape
import kotlin.math.min

/** Width / height of the box a pill is drawn in. */
val PillShape.aspectRatio: Float
    get() = if (this == PillShape.SOFTGEL) 1.45f else 1f

/** Resting angle: capsules lie diagonally, as in the reference. */
val PillShape.defaultRotation: Float
    get() = if (this == PillShape.CAPSULE) -45f else 0f

/**
 * Draws one pill centred in the box at [topLeft]/[size]. Shared by the app, the
 * web build and the Android widgets (which render it into a bitmap), so every
 * surface shows the same pill.
 */
fun DrawScope.drawPill(
    shape: PillShape,
    color: PillColor,
    topLeft: Offset = Offset.Zero,
    size: Size = this.size,
    rotationDegrees: Float = shape.defaultRotation,
    alpha: Float = 1f,
) {
    val center = Offset(topLeft.x + size.width / 2f, topLeft.y + size.height / 2f)
    // u is the height of the pill's box; every measurement below is a fraction of it.
    val u = min(size.width / shape.aspectRatio, size.height)
    val fill = color.fill.copy(alpha = alpha)
    val edge = Stroke(width = u * 0.035f)
    val edgeColor = color.edge.copy(alpha = 0.55f * alpha)
    val ink = PillInk.copy(alpha = 0.92f * alpha)
    val shadow = Color.Black.copy(alpha = 0.22f * alpha)
    val shadowOffset = Offset(0f, u * 0.05f)

    rotate(rotationDegrees, pivot = center) {
        when (shape) {
            PillShape.ROUND_SCORED -> {
                val radius = u * 0.43f
                drawCircle(shadow, radius, center + shadowOffset)
                drawCircle(fill, radius, center)
                drawCircle(edgeColor, radius, center, style = edge)
                val half = radius * 0.5f
                drawLine(
                    ink,
                    start = center + Offset(-half, half),
                    end = center + Offset(half, -half),
                    strokeWidth = u * 0.08f,
                )
            }

            PillShape.SQUARE -> {
                val side = u * 0.84f
                val outer = Offset(center.x - side / 2, center.y - side / 2)
                drawRoundRect(shadow, outer + shadowOffset, Size(side, side), CornerRadius(side * 0.17f))
                drawRoundRect(fill, outer, Size(side, side), CornerRadius(side * 0.17f))
                drawRoundRect(edgeColor, outer, Size(side, side), CornerRadius(side * 0.17f), style = edge)
                val inset = side * 0.2f
                val innerSide = side - inset * 2
                drawRoundRect(
                    ink,
                    outer + Offset(inset, inset),
                    Size(innerSide, innerSide),
                    CornerRadius(side * 0.05f),
                    style = Stroke(width = side * 0.09f),
                )
            }

            PillShape.CAPSULE -> {
                val length = u * 1.12f
                val thickness = u * 0.5f
                val origin = Offset(center.x - length / 2, center.y - thickness / 2)
                // The shadow falls downwards on screen, so undo the rotation for its offset.
                drawRoundRect(
                    shadow,
                    origin + shadowOffset.rotated(-rotationDegrees),
                    Size(length, thickness),
                    CornerRadius(thickness / 2),
                )
                drawRoundRect(fill, origin, Size(length, thickness), CornerRadius(thickness / 2))
                drawRoundRect(edgeColor, origin, Size(length, thickness), CornerRadius(thickness / 2), style = edge)
                drawLine(
                    ink,
                    start = Offset(center.x, center.y - thickness / 2),
                    end = Offset(center.x, center.y + thickness / 2),
                    strokeWidth = u * 0.075f,
                    cap = StrokeCap.Butt,
                )
            }

            PillShape.SOFTGEL -> {
                val width = u * shape.aspectRatio * 0.9f
                val height = u * 0.84f
                val origin = Offset(center.x - width / 2, center.y - height / 2)
                drawRoundRect(shadow, origin + shadowOffset, Size(width, height), CornerRadius(height / 2))
                drawRoundRect(fill, origin, Size(width, height), CornerRadius(height / 2))
                drawRoundRect(edgeColor, origin, Size(width, height), CornerRadius(height / 2), style = edge)
                val inset = u * 0.13f
                val innerHeight = height - inset * 2
                drawRoundRect(
                    ink,
                    origin + Offset(inset, inset),
                    Size(width - inset * 2, innerHeight),
                    CornerRadius(innerHeight / 2),
                    style = Stroke(width = u * 0.075f),
                )
            }
        }
    }
}

private fun Offset.rotated(degrees: Float): Offset {
    val radians = degrees * kotlin.math.PI.toFloat() / 180f
    val cos = kotlin.math.cos(radians)
    val sin = kotlin.math.sin(radians)
    return Offset(x * cos - y * sin, x * sin + y * cos)
}
