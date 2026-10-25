package org.mariacf.tempusremedium.android.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.view.View
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.compose
import androidx.glance.appwidget.provideContent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mariacf.tempusremedium.core.CourseFixtures
import org.mariacf.tempusremedium.core.ReferenceFixtures
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders the widgets with the reference data into the images the launcher's widget picker shows
 * (`android:previewImage`, under androidApp/src/main/res/drawable-*-nodpi). The images are only
 * written by `./gradlew :androidApp:recordRoborazziDebug`; other runs just check they render.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetPreviewImagesTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    @Config(sdk = [36], qualifiers = "pt-rPT-xhdpi")
    fun portugueseLight() = renderAll("nodpi")

    @Test
    @Config(sdk = [36], qualifiers = "pt-rPT-night-xhdpi")
    fun portugueseDark() = renderAll("night-nodpi")

    @Test
    @Config(sdk = [36], qualifiers = "en-rUS-xhdpi")
    fun englishLight() = renderAll("en-nodpi")

    @Test
    @Config(sdk = [36], qualifiers = "en-rUS-night-xhdpi")
    fun englishDark() = renderAll("en-night-nodpi")

    @Test
    @Config(sdk = [36], qualifiers = "fr-rFR-xhdpi")
    fun frenchLight() = renderAll("fr-nodpi")

    @Test
    @Config(sdk = [36], qualifiers = "fr-rFR-night-xhdpi")
    fun frenchDark() = renderAll("fr-night-nodpi")

    @Test
    @Config(sdk = [36], qualifiers = "de-rDE-xhdpi")
    fun germanLight() = renderAll("de-nodpi")

    @Test
    @Config(sdk = [36], qualifiers = "de-rDE-night-xhdpi")
    fun germanDark() = renderAll("de-night-nodpi")

    @Test
    @Config(sdk = [36], qualifiers = "es-rES-xhdpi")
    fun spanishLight() = renderAll("es-nodpi")

    @Test
    @Config(sdk = [36], qualifiers = "es-rES-night-xhdpi")
    fun spanishDark() = renderAll("es-night-nodpi")

    /**
     * Illustrations for the README (docs/images): the widgets on a home screen and HOJE on a lock
     * screen, dark, on a wallpaper in the brand blues.
     */
    @Test
    @Config(sdk = [36], qualifiers = "pt-rPT-night-xhdpi")
    fun phoneScreens() {
        val hoje = runBlocking { HojeWidgetUi.build(context, ReferenceFixtures.data, ReferenceFixtures.date) }
        val repor = runBlocking { ReporWidgetUi.build(context, ReferenceFixtures.data) }
        val course = runBlocking { CourseWidgetUi.build(context, CourseFixtures.data, ReferenceFixtures.now) }
        val hojeBitmap = draw("hoje", DpSize(320.dp, 220.dp)) { HojeWidgetContent(hoje) }
        val courseBitmap = draw("course", DpSize(320.dp, 150.dp)) { CourseWidgetContent(course) }
        val reporBitmap = draw("repor", DpSize(320.dp, 220.dp)) { ReporWidgetContent(repor) }

        val home =
            phone { canvas, dp ->
                canvas.drawText("16:37", 24 * dp, 34 * dp, text(14 * dp))
                var y = 60 * dp
                for (widget in listOf(hojeBitmap, courseBitmap, reporBitmap)) {
                    canvas.drawBitmap(widget, 20 * dp, y, null)
                    y += widget.height + 16 * dp
                }
            }
        val lock =
            phone { canvas, dp ->
                val center = PHONE_WIDTH_DP * dp / 2
                canvas.drawText("16:37", center, 170 * dp, text(84 * dp, Paint.Align.CENTER, light = true))
                canvas.drawText("quinta-feira, 22 de outubro", center, 206 * dp, text(16 * dp, Paint.Align.CENTER))
                canvas.drawBitmap(hojeBitmap, 20 * dp, 250 * dp, null)
            }
        if (System.getProperty("roborazzi.test.record") == "true") {
            save(home, File("../docs/images/phone_home_widgets.png"))
            save(lock, File("../docs/images/phone_lock_widget.png"))
        }
    }

    private fun phone(drawContent: (Canvas, Float) -> Unit): Bitmap {
        val dp = context.resources.displayMetrics.density
        val width = (PHONE_WIDTH_DP * dp).toInt()
        val height = (PHONE_HEIGHT_DP * dp).toInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val screen = RectF(0f, 0f, width.toFloat(), height.toFloat())
        val radius = 36 * dp
        canvas.drawRoundRect(
            screen,
            radius,
            radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader =
                    LinearGradient(0f, 0f, width * 0.4f, height.toFloat(), 0xFF0E2E52.toInt(), 0xFF050D18.toInt(), Shader.TileMode.CLAMP)
            },
        )
        // Soft blobs, as on mariacf.pt.
        for ((x, y, r) in listOf(Triple(0.15f, 0.2f, 0.6f), Triple(0.9f, 0.75f, 0.55f))) {
            canvas.drawCircle(
                x * width,
                y * height,
                r * width,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(x * width, y * height, r * width, 0x55125A98, 0x00125A98, Shader.TileMode.CLAMP)
                },
            )
        }
        drawContent(canvas, dp)
        // The gesture bar.
        val bar = RectF(width / 2 - 54 * dp, height - 14 * dp, width / 2 + 54 * dp, height - 10 * dp)
        canvas.drawRoundRect(bar, 2 * dp, 2 * dp, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xCCFFFFFF.toInt() })
        return bitmap
    }

    private fun text(
        size: Float,
        align: Paint.Align = Paint.Align.LEFT,
        light: Boolean = false,
    ) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textSize = size
        textAlign = align
        typeface = Typeface.create(Typeface.SANS_SERIF, if (light) Typeface.NORMAL else Typeface.BOLD)
    }

    private fun save(
        bitmap: Bitmap,
        file: File,
    ) {
        file.parentFile?.mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun renderAll(qualifiers: String) {
        val hoje = runBlocking { HojeWidgetUi.build(context, ReferenceFixtures.data, ReferenceFixtures.date) }
        val repor = runBlocking { ReporWidgetUi.build(context, ReferenceFixtures.data) }
        val course = runBlocking { CourseWidgetUi.build(context, CourseFixtures.data, ReferenceFixtures.now) }
        render("widget_hoje_preview", qualifiers, DpSize(320.dp, 220.dp)) { HojeWidgetContent(hoje) }
        render("widget_repor_preview", qualifiers, DpSize(320.dp, 220.dp)) { ReporWidgetContent(repor) }
        render("widget_course_preview", qualifiers, DpSize(320.dp, 150.dp)) { CourseWidgetContent(course) }
    }

    private fun render(
        name: String,
        qualifiers: String,
        size: DpSize,
        content: @Composable () -> Unit,
    ) {
        val bitmap = draw(name, size, content)
        if (System.getProperty("roborazzi.test.record") == "true") {
            val file = File("src/main/res/drawable-$qualifiers/$name.webp")
            file.parentFile?.mkdirs()
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, it) }
        }
    }

    private fun draw(
        name: String,
        size: DpSize,
        content: @Composable () -> Unit,
    ): Bitmap {
        val views = runBlocking { ContentWidget(content).compose(context, size = size) }
        val density = context.resources.displayMetrics.density
        val width = (size.width.value * density).toInt()
        val height = (size.height.value * density).toInt()
        val view = views.apply(context, FrameLayout(context))
        view.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        assertTrue("$name drew nothing", (0 until width step 8).any { x -> bitmap.getPixel(x, height / 2) != 0 })
        return bitmap
    }

    private companion object {
        const val PHONE_WIDTH_DP = 360
        const val PHONE_HEIGHT_DP = 760
    }

    private class ContentWidget(
        private val content: @Composable () -> Unit,
    ) : GlanceAppWidget() {
        override suspend fun provideGlance(
            context: Context,
            id: GlanceId,
        ) = provideContent { content() }
    }
}
