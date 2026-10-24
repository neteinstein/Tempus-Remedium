package io.github.neteinstein.remedioemdia.ui.pill

import androidx.compose.ui.graphics.Color
import io.github.neteinstein.remedioemdia.core.model.PillColor

/** Score lines and inner outlines drawn on pills, as in the reference. */
val PillInk = Color(0xFF3C4043)

/** Fill colour of each [PillColor]. The first four are sampled from the reference. */
val PillColor.fill: Color
    get() =
        when (this) {
            PillColor.TEAL -> Color(0xFF8EE8E2)
            PillColor.PINK -> Color(0xFFFE9EB0)
            PillColor.PEACH -> Color(0xFFFCD3A2)
            PillColor.BLUE -> Color(0xFFAAD2FC)
            PillColor.YELLOW -> Color(0xFFF6E38C)
            PillColor.LAVENDER -> Color(0xFFCDBBF5)
            PillColor.GREEN -> Color(0xFFB7E3A3)
            PillColor.WHITE -> Color(0xFFF2F0EC)
        }

/** A darker shade of the fill, outlining the pill so light pills hold their shape on white. */
val PillColor.edge: Color
    get() = fill.let { Color(red = it.red * 0.78f, green = it.green * 0.78f, blue = it.blue * 0.78f, alpha = 1f) }
