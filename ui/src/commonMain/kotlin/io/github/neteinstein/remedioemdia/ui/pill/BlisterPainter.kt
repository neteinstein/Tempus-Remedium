package io.github.neteinstein.remedioemdia.ui.pill

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import io.github.neteinstein.remedioemdia.core.model.PillColor
import io.github.neteinstein.remedioemdia.core.model.PillShape
import io.github.neteinstein.remedioemdia.ui.theme.BlisterColors

/** Cells in one blister pack: 2 columns of 5. */
const val BLISTER_CAPACITY = 10

private const val BLISTER_COLUMNS = 2
private const val BLISTER_ROWS = 5

/**
 * A foil blister pack in the same footprint as a jar ([JAR_ASPECT_RATIO]). The first [count]
 * cells hold a pill under a clear dome; the rest are popped, showing the torn foil.
 */
fun DrawScope.drawBlister(
    shape: PillShape,
    color: PillColor,
    count: Int,
    colors: BlisterColors,
    size: Size = this.size,
) {
    val w = size.width
    val h = size.height
    val corner = CornerRadius(w * 0.12f)

    // Foil card with a soft drop shadow.
    drawRoundRect(colors.foilStroke, topLeft = Offset(0f, h * 0.012f), size = Size(w, h), cornerRadius = corner)
    drawRoundRect(Brush.verticalGradient(listOf(colors.foilTop, colors.foilBottom)), size = Size(w, h), cornerRadius = corner)
    drawRoundRect(colors.foilStroke, size = Size(w, h), cornerRadius = corner, style = Stroke(width = (w * 0.012f).coerceAtLeast(1f)))

    // Perforation between the two columns.
    val dash = h * 0.02f
    var y = h * 0.06f
    while (y < h * 0.94f) {
        drawLine(colors.holeEdge, Offset(w / 2, y), Offset(w / 2, y + dash), strokeWidth = (w * 0.008f).coerceAtLeast(1f))
        y += dash * 2
    }

    val padX = w * 0.08f
    val padY = h * 0.06f
    val gapX = w * 0.1f
    val gapY = h * 0.03f
    val cellW = (w - padX * 2 - gapX) / BLISTER_COLUMNS
    val cellH = (h - padY * 2 - gapY * (BLISTER_ROWS - 1)) / BLISTER_ROWS
    val cellCorner = CornerRadius(minOf(cellW, cellH) / 2)
    val full = count.coerceIn(0, BLISTER_CAPACITY)

    for (index in 0 until BLISTER_CAPACITY) {
        val col = index % BLISTER_COLUMNS
        val row = index / BLISTER_COLUMNS
        val topLeft = Offset(padX + col * (cellW + gapX), padY + row * (cellH + gapY))
        val cell = Size(cellW, cellH)
        if (index < full) {
            // Clear plastic dome: glossy at the top, the pill inside.
            drawRoundRect(
                Brush.verticalGradient(listOf(colors.domeShine, colors.dome), startY = topLeft.y, endY = topLeft.y + cellH),
                topLeft,
                cell,
                cellCorner,
            )
            if (shape == PillShape.CAPSULE) {
                // Capsules lie flat along the cell.
                val unit = cellW * 0.6f
                drawPill(
                    shape = shape,
                    color = color,
                    topLeft = Offset(topLeft.x + (cellW - unit) / 2, topLeft.y + (cellH - unit) / 2),
                    size = Size(unit, unit),
                    rotationDegrees = 0f,
                )
            } else {
                val pillHeight = cellH * 0.8f
                val pillWidth = minOf(pillHeight * shape.aspectRatio, cellW * 0.82f)
                drawPill(
                    shape = shape,
                    color = color,
                    topLeft = Offset(topLeft.x + (cellW - pillWidth) / 2, topLeft.y + (cellH - pillHeight) / 2),
                    size = Size(pillWidth, pillHeight),
                )
            }
            drawRoundRect(colors.domeStroke, topLeft, cell, cellCorner, style = Stroke(width = (w * 0.01f).coerceAtLeast(1f)))
        } else {
            // Popped: an empty pocket with a flap of torn foil hanging from its top edge.
            drawRoundRect(colors.hole, topLeft, cell, cellCorner)
            val flap =
                Path().apply {
                    val left = topLeft.x + cellW * 0.16f
                    val right = topLeft.x + cellW * 0.7f
                    val top = topLeft.y + cellH * 0.08f
                    moveTo(left, top)
                    lineTo(right, top)
                    lineTo(right - cellW * 0.06f, top + cellH * 0.3f)
                    lineTo(right - cellW * 0.16f, top + cellH * 0.18f)
                    lineTo(right - cellW * 0.27f, top + cellH * 0.38f)
                    lineTo(left + cellW * 0.12f, top + cellH * 0.22f)
                    lineTo(left + cellW * 0.03f, top + cellH * 0.32f)
                    close()
                }
            drawPath(flap, colors.foilTop.copy(alpha = 0.9f))
            drawPath(flap, colors.holeEdge, style = Stroke(width = (w * 0.008f).coerceAtLeast(1f)))
            drawRoundRect(colors.holeEdge, topLeft, cell, cellCorner, style = Stroke(width = (w * 0.01f).coerceAtLeast(1f)))
        }
    }
}
