package io.github.neteinstein.remedioemdia.android.widgets

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.unit.sp
import androidx.glance.color.ColorProvider
import androidx.glance.text.FontWeight
import androidx.glance.text.TextStyle
import io.github.neteinstein.remedioemdia.ui.theme.RemedioPalette

/** Glance versions of the app's text styles, following the system's day/night setting. */
internal object WidgetStyle {
    private val light = RemedioPalette.Light
    private val dark = RemedioPalette.Dark

    val ink = ColorProvider(day = light.ink, night = dark.ink)
    val inkSoft = ColorProvider(day = light.inkSoft, night = dark.inkSoft)
    val inkMute = ColorProvider(day = light.inkMute, night = dark.inkMute)
    val accent = ColorProvider(day = light.accent, night = dark.accent)
    val stock = ColorProvider(day = light.stock, night = dark.stock)
    val divider = ColorProvider(day = light.line, night = dark.line)

    val title = TextStyle(color = ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    val meta = TextStyle(color = inkMute, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    val slotLabel = TextStyle(color = inkSoft, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    val slotTime = TextStyle(color = ink, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    val legend = TextStyle(color = ink, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    val jarName = TextStyle(color = ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    val jarCount = TextStyle(color = stock, fontSize = 13.sp, fontWeight = FontWeight.Medium)

    /** "HOJE" -> "H O J E" with hair spaces, standing in for the reference's letter spacing. */
    fun spaced(text: String): String = text.uppercase().toList().joinToString(" ")

    /** Bitmaps cannot follow day/night by themselves, so pick the palette when drawing them. */
    fun palette(context: Context): RemedioPalette {
        val night = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        return if (night) dark else light
    }
}
