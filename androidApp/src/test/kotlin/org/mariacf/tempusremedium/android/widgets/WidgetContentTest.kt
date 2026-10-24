package org.mariacf.tempusremedium.android.widgets

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.mariacf.tempusremedium.core.ReferenceFixtures
import org.mariacf.tempusremedium.core.model.AppData
import org.mariacf.tempusremedium.core.model.Settings
import org.mariacf.tempusremedium.core.model.StockDisplay
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "pt-rPT")
class WidgetContentTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun hojeWidgetShowsTheReferenceDay() {
        val ui = runBlocking { HojeWidgetUi.build(context, ReferenceFixtures.data, ReferenceFixtures.date) }
        assertEquals("qui., 22 · 5 doses", ui.meta)
        assertEquals(listOf("MANHÃ", "MEIO-DIA", "NOITE", "DEITAR"), ui.slots.map { it.label })
        assertEquals(listOf(2, 1, 1, 1), ui.slots.map { it.pills.size })

        runGlanceAppWidgetUnitTest {
            setAppWidgetSize(DpSize(320.dp, 220.dp))
            setContext(context)
            provideComposable { HojeWidgetContent(ui) }
            onNode(hasText("MANHÃ")).assertExists()
            onNode(hasText("22:00")).assertExists()
            onNode(hasText("Vitamina D 1000IU")).assertExists()
        }
    }

    @Test
    fun reporWidgetShowsTheReferenceJars() {
        val ui = runBlocking { ReporWidgetUi.build(context, ReferenceFixtures.data) }
        assertEquals("2 medicamentos", ui.meta)
        assertEquals(listOf("Ómega 3" to "Restam 3", "Metformina" to "Restam 6"), ui.jars.map { it.name to it.count })

        runGlanceAppWidgetUnitTest {
            setAppWidgetSize(DpSize(320.dp, 220.dp))
            setContext(context)
            provideComposable { ReporWidgetContent(ui) }
            onNode(hasText("Restam 3")).assertExists()
            onNode(hasText("Metformina")).assertExists()
        }
    }

    @Test
    fun reporWidgetFollowsTheBlisterSetting() {
        val jars = runBlocking { ReporWidgetUi.build(context, ReferenceFixtures.data) }
        val blisters =
            runBlocking {
                ReporWidgetUi.build(
                    context,
                    ReferenceFixtures.data.copy(settings = Settings(stockDisplay = StockDisplay.BLISTERS)),
                )
            }
        // Same footprint, different drawing.
        assertEquals(jars.jars[0].bitmap.width, blisters.jars[0].bitmap.width)
        assertFalse(jars.jars[0].bitmap.sameAs(blisters.jars[0].bitmap))
    }

    @Test
    fun reporWidgetSaysWhenNothingIsLow() {
        val ui = runBlocking { ReporWidgetUi.build(context, AppData()) }
        assertEquals("0 medicamentos", ui.meta)
        runGlanceAppWidgetUnitTest {
            setContext(context)
            provideComposable { ReporWidgetContent(ui) }
            onNode(hasText("Nada a repor por agora.")).assertExists()
        }
    }
}
