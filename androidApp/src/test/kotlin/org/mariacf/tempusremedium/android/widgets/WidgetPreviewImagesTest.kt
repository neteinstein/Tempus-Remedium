package org.mariacf.tempusremedium.android.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
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

    private fun renderAll(qualifiers: String) {
        val hoje = runBlocking { HojeWidgetUi.build(context, ReferenceFixtures.data, ReferenceFixtures.date) }
        val repor = runBlocking { ReporWidgetUi.build(context, ReferenceFixtures.data) }
        render("widget_hoje_preview", qualifiers) { HojeWidgetContent(hoje) }
        render("widget_repor_preview", qualifiers) { ReporWidgetContent(repor) }
    }

    private fun render(
        name: String,
        qualifiers: String,
        content: @Composable () -> Unit,
    ) {
        val size = DpSize(320.dp, 220.dp)
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

        if (System.getProperty("roborazzi.test.record") == "true") {
            val file = File("src/main/res/drawable-$qualifiers/$name.png")
            file.parentFile?.mkdirs()
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
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
