package io.github.neteinstein.remedioemdia.android.widgets

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.glance.text.FontWeight
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.github.neteinstein.remedioemdia.ui.theme.RemedioColors

/** Glance versions of the reference text styles (Glance has no letter spacing, see [spaced]). */
internal object WidgetStyle {
    val primary = ColorProvider(RemedioColors.TextPrimary)
    val secondary = ColorProvider(RemedioColors.TextSecondary)
    val amber = ColorProvider(RemedioColors.Amber)
    val divider = ColorProvider(RemedioColors.Divider)
    val transparent = ColorProvider(Color.Transparent)

    val title = TextStyle(color = primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    val meta = TextStyle(color = secondary, fontSize = 13.sp)
    val slotLabel = TextStyle(color = primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    val slotTime = TextStyle(color = primary, fontSize = 13.sp)
    val legend = TextStyle(color = primary, fontSize = 13.sp)
    val jarName = TextStyle(color = primary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    val jarCount = TextStyle(color = amber, fontSize = 13.sp, fontWeight = FontWeight.Medium)

    /** "HOJE" -> "H O J E" with hair spaces, standing in for the reference's letter spacing. */
    fun spaced(text: String): String = text.uppercase().toList().joinToString(" ")
}
