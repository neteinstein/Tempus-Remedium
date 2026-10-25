package org.mariacf.tempusremedium.ui.pill

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
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

/** Resting angle: capsules lie diagonally, as in the reference, and so do syringes. */
val PillShape.defaultRotation: Float
    get() = if (this == PillShape.CAPSULE || this == PillShape.INJECTION) -45f else 0f

/** Caps, canisters and nozzles: the white plastic parts of devices, whatever the medication's colour. */
private val DeviceWhite = PillColor.WHITE.fill

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

            PillShape.INHALER -> {
                // Pressurised inhaler: white canister in a coloured actuator with the mouthpiece to the left.
                val canisterTopLeft = center + Offset(-u * 0.04f, -u * 0.45f)
                val canister = Size(u * 0.24f, u * 0.3f)
                val bodyTopLeft = center + Offset(-u * 0.1f, -u * 0.22f)
                val body = Size(u * 0.36f, u * 0.64f)
                val mouthTopLeft = center + Offset(-u * 0.42f, u * 0.12f)
                val mouth = Size(u * 0.4f, u * 0.3f)
                drawRoundRect(shadow, bodyTopLeft + shadowOffset, body, CornerRadius(u * 0.1f))
                drawRoundRect(shadow, mouthTopLeft + shadowOffset, mouth, CornerRadius(u * 0.08f))
                drawRoundRect(DeviceWhite.copy(alpha = alpha), canisterTopLeft, canister, CornerRadius(u * 0.07f))
                drawRoundRect(edgeColor, canisterTopLeft, canister, CornerRadius(u * 0.07f), style = edge)
                drawRoundRect(fill, mouthTopLeft, mouth, CornerRadius(u * 0.08f))
                drawRoundRect(edgeColor, mouthTopLeft, mouth, CornerRadius(u * 0.08f), style = edge)
                drawRoundRect(fill, bodyTopLeft, body, CornerRadius(u * 0.1f))
                drawRoundRect(edgeColor, bodyTopLeft, body, CornerRadius(u * 0.1f), style = edge)
                // Mouthpiece opening.
                drawLine(
                    ink,
                    start = mouthTopLeft + Offset(u * 0.08f, u * 0.08f),
                    end = mouthTopLeft + Offset(u * 0.08f, mouth.height - u * 0.08f),
                    strokeWidth = u * 0.07f,
                    cap = StrokeCap.Round,
                )
            }

            PillShape.PATCH -> {
                // Transdermal patch: a soft square with the drug reservoir outlined in the middle.
                val side = u * 0.84f
                val outer = Offset(center.x - side / 2, center.y - side / 2)
                val corner = CornerRadius(side * 0.3f)
                drawRoundRect(shadow, outer + shadowOffset, Size(side, side), corner)
                drawRoundRect(fill, outer, Size(side, side), corner)
                drawRoundRect(edgeColor, outer, Size(side, side), corner, style = edge)
                drawCircle(
                    ink,
                    radius = side * 0.24f,
                    center = center,
                    style =
                        Stroke(
                            width = side * 0.07f,
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(side * 0.06f, side * 0.1f)),
                        ),
                )
            }

            PillShape.NASAL_SPRAY -> {
                // Spray bottle: coloured body, white collar and a tapering nozzle.
                val bodyTopLeft = center + Offset(-u * 0.22f, -u * 0.06f)
                val body = Size(u * 0.44f, u * 0.5f)
                val collarTopLeft = center + Offset(-u * 0.25f, -u * 0.16f)
                val collar = Size(u * 0.5f, u * 0.12f)
                val nozzle =
                    Path().apply {
                        moveTo(center.x - u * 0.1f, center.y - u * 0.16f)
                        lineTo(center.x - u * 0.045f, center.y - u * 0.46f)
                        lineTo(center.x + u * 0.045f, center.y - u * 0.46f)
                        lineTo(center.x + u * 0.1f, center.y - u * 0.16f)
                        close()
                    }
                drawRoundRect(shadow, bodyTopLeft + shadowOffset, body, CornerRadius(u * 0.12f))
                drawRoundRect(fill, bodyTopLeft, body, CornerRadius(u * 0.12f))
                drawRoundRect(edgeColor, bodyTopLeft, body, CornerRadius(u * 0.12f), style = edge)
                drawPath(nozzle, DeviceWhite.copy(alpha = alpha))
                drawPath(nozzle, edgeColor, style = edge)
                drawRoundRect(DeviceWhite.copy(alpha = alpha), collarTopLeft, collar, CornerRadius(u * 0.04f))
                drawRoundRect(edgeColor, collarTopLeft, collar, CornerRadius(u * 0.04f), style = edge)
                // Label band.
                drawLine(
                    ink,
                    start = center + Offset(-u * 0.12f, u * 0.2f),
                    end = center + Offset(u * 0.12f, u * 0.2f),
                    strokeWidth = u * 0.07f,
                    cap = StrokeCap.Round,
                )
            }

            PillShape.DROPS -> {
                // A single drop.
                val top = center + Offset(0f, -u * 0.44f)
                val radius = u * 0.27f
                val bulb = center + Offset(0f, u * 0.14f)
                val drop =
                    Path().apply {
                        moveTo(top.x, top.y)
                        cubicTo(top.x + radius * 0.35f, top.y + u * 0.2f, bulb.x + radius, bulb.y - radius * 0.9f, bulb.x + radius, bulb.y)
                        cubicTo(bulb.x + radius, bulb.y + radius * 1.35f, bulb.x - radius, bulb.y + radius * 1.35f, bulb.x - radius, bulb.y)
                        cubicTo(bulb.x - radius, bulb.y - radius * 0.9f, top.x - radius * 0.35f, top.y + u * 0.2f, top.x, top.y)
                        close()
                    }
                drawPath(Path().apply { addPath(drop, shadowOffset) }, shadow)
                drawPath(drop, fill)
                drawPath(drop, edgeColor, style = edge)
                // Highlight on the bulb.
                drawArc(
                    ink,
                    startAngle = 100f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = bulb - Offset(radius * 0.6f, radius * 0.6f),
                    size = Size(radius * 1.2f, radius * 1.2f),
                    style = Stroke(width = u * 0.07f, cap = StrokeCap.Round),
                )
            }

            PillShape.INJECTION -> {
                // Syringe lying along the box: needle to the left, plunger to the right.
                val barrelLength = u * 0.6f
                val thickness = u * 0.26f
                val barrelTopLeft = Offset(center.x - barrelLength / 2 - u * 0.05f, center.y - thickness / 2)
                val barrel = Size(barrelLength, thickness)
                drawRoundRect(
                    shadow,
                    barrelTopLeft + shadowOffset.rotated(-rotationDegrees),
                    barrel,
                    CornerRadius(u * 0.05f),
                )
                // Needle and plunger rod behind the barrel.
                drawLine(
                    edgeColor,
                    start = Offset(barrelTopLeft.x - u * 0.24f, center.y),
                    end = Offset(barrelTopLeft.x, center.y),
                    strokeWidth = u * 0.035f,
                )
                val rodStart = barrelTopLeft.x + barrelLength
                drawRect(DeviceWhite.copy(alpha = alpha), Offset(rodStart, center.y - u * 0.04f), Size(u * 0.2f, u * 0.08f))
                drawRect(edgeColor, Offset(rodStart, center.y - u * 0.04f), Size(u * 0.2f, u * 0.08f), style = edge)
                val flangeTopLeft = Offset(rodStart + u * 0.18f, center.y - u * 0.16f)
                drawRoundRect(DeviceWhite.copy(alpha = alpha), flangeTopLeft, Size(u * 0.07f, u * 0.32f), CornerRadius(u * 0.03f))
                drawRoundRect(edgeColor, flangeTopLeft, Size(u * 0.07f, u * 0.32f), CornerRadius(u * 0.03f), style = edge)
                drawRoundRect(fill, barrelTopLeft, barrel, CornerRadius(u * 0.05f))
                drawRoundRect(edgeColor, barrelTopLeft, barrel, CornerRadius(u * 0.05f), style = edge)
                // Graduation marks.
                repeat(3) { i ->
                    val x = barrelTopLeft.x + barrelLength * (0.28f + i * 0.2f)
                    drawLine(
                        ink,
                        start = Offset(x, barrelTopLeft.y),
                        end = Offset(x, barrelTopLeft.y + thickness * 0.45f),
                        strokeWidth = u * 0.05f,
                        cap = StrokeCap.Butt,
                    )
                }
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
