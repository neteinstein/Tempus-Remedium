package org.mariacf.tempusremedium.android.widgets

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.Text
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.mariacf.tempusremedium.android.MainActivity
import org.mariacf.tempusremedium.android.R
import org.mariacf.tempusremedium.android.container
import org.mariacf.tempusremedium.core.model.AppData
import org.mariacf.tempusremedium.core.schedule.lowStock
import org.mariacf.tempusremedium.ui.pill.JAR_ASPECT_RATIO
import org.mariacf.tempusremedium.ui.resources.Res
import org.mariacf.tempusremedium.ui.resources.medications_count
import org.mariacf.tempusremedium.ui.resources.refill_soon
import org.mariacf.tempusremedium.ui.resources.units_left
import org.mariacf.tempusremedium.ui.resources.widget_nothing_low

/** Everything the REPOR EM BREVE widget shows, resolved outside composition. */
data class ReporWidgetUi(
    val title: String,
    val meta: String,
    val empty: String,
    val jars: List<Jar>,
) {
    data class Jar(
        val medicationId: String,
        val bitmap: Bitmap,
        val name: String,
        val count: String,
    )

    companion object {
        const val JAR_DP = 96f

        /** The widget shows one row of jars, like the reference. */
        private const val MAX_JARS = 2

        suspend fun build(
            context: Context,
            data: AppData,
        ): ReporWidgetUi {
            val density = context.resources.displayMetrics.density
            val palette = WidgetStyle.palette(context)
            val items = data.lowStock()
            return ReporWidgetUi(
                title = WidgetStyle.spaced(getString(Res.string.refill_soon)),
                meta = getPluralString(Res.plurals.medications_count, items.size, items.size),
                empty = getString(Res.string.widget_nothing_low),
                jars =
                    items.take(MAX_JARS).map { item ->
                        Jar(
                            medicationId = item.medication.id,
                            bitmap =
                                PillBitmaps.stock(
                                    item.medication.shape,
                                    item.medication.color,
                                    item.remaining,
                                    data.settings.stockDisplay,
                                    palette,
                                    JAR_DP,
                                    density,
                                ),
                            name = item.medication.name,
                            count = getPluralString(Res.plurals.units_left, item.remaining, item.remaining),
                        )
                    },
            )
        }

        suspend fun load(context: Context): ReporWidgetUi {
            val repository = context.container.repository
            repository.load()
            return build(context, repository.data.value)
        }
    }
}

class ReporEmBreveWidget : GlanceAppWidget() {
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val initial = ReporWidgetUi.load(context)
        provideContent {
            val refresh by Widgets.refreshCount.collectAsStateCompat()
            val ui by produceState(initial, refresh) { value = ReporWidgetUi.load(context) }
            ReporWidgetContent(ui)
        }
    }
}

class ReporEmBreveWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ReporEmBreveWidget()
}

// Glance composables take a GlanceModifier, which the Compose lint rules do not know.
@Suppress("ktlint:compose:modifier-missing-check")
@Composable
fun ReporWidgetContent(ui: ReporWidgetUi) {
    val context = LocalContext.current
    Column(
        GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_background))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(GlanceModifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(ui.title, style = WidgetStyle.title)
            Spacer(GlanceModifier.defaultWeight())
            Text(ui.meta, style = WidgetStyle.meta, maxLines = 1)
        }
        Spacer(GlanceModifier.height(6.dp))
        Box(GlanceModifier.fillMaxWidth().height(1.dp).background(WidgetStyle.divider)) {}
        Spacer(GlanceModifier.height(8.dp))
        if (ui.jars.isEmpty()) {
            Box(GlanceModifier.fillMaxWidth().defaultWeight(), contentAlignment = Alignment.Center) {
                Text(ui.empty, style = WidgetStyle.meta)
            }
            return@Column
        }
        Row(GlanceModifier.fillMaxWidth().defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
            ui.jars.forEach { jar ->
                Column(
                    GlanceModifier
                        .defaultWeight()
                        .semantics { contentDescription = "${jar.name}, ${jar.count}" }
                        .clickable(
                            actionStartActivity(
                                Intent(context, MainActivity::class.java)
                                    .putExtra(MainActivity.EXTRA_REFILL, jar.medicationId)
                                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                            ),
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        provider = ImageProvider(jar.bitmap),
                        contentDescription = null,
                        modifier = GlanceModifier.size((ReporWidgetUi.JAR_DP * JAR_ASPECT_RATIO).dp, ReporWidgetUi.JAR_DP.dp),
                    )
                    Spacer(GlanceModifier.height(4.dp))
                    Text(jar.name, style = WidgetStyle.jarName, maxLines = 1)
                    Text(jar.count, style = WidgetStyle.jarCount, maxLines = 1)
                }
            }
            if (ui.jars.size == 1) Box(GlanceModifier.defaultWeight()) {}
        }
    }
}
