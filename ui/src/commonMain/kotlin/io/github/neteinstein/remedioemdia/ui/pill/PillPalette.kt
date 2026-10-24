package io.github.neteinstein.remedioemdia.ui.pill

import androidx.compose.ui.graphics.Color
import io.github.neteinstein.remedioemdia.core.model.PillColor

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
