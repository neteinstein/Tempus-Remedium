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
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.Text
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.mariacf.tempusremedium.android.MainActivity
import org.mariacf.tempusremedium.android.R
import org.mariacf.tempusremedium.android.container
import org.mariacf.tempusremedium.core.model.AppData
import org.mariacf.tempusremedium.core.model.TimeSlot
import org.mariacf.tempusremedium.core.schedule.planFor
import org.mariacf.tempusremedium.ui.components.hhmm
import org.mariacf.tempusremedium.ui.components.label
import org.mariacf.tempusremedium.ui.components.shortLabel
import org.mariacf.tempusremedium.ui.pill.aspectRatio
import org.mariacf.tempusremedium.ui.resources.Res
import org.mariacf.tempusremedium.ui.resources.day_label
import org.mariacf.tempusremedium.ui.resources.doses
import org.mariacf.tempusremedium.ui.resources.today
import org.mariacf.tempusremedium.ui.resources.today_meta
import kotlin.time.Clock

/** Everything the HOJE widget shows, resolved outside composition (strings and bitmaps). */
data class HojeWidgetUi(
    val title: String,
    val meta: String,
    val slots: List<Slot>,
    val legend: List<LegendEntry>,
) {
    data class Slot(
        val slot: TimeSlot,
        val label: String,
        val time: String,
        val pills: List<Pill>,
        val description: String,
    )

    data class Pill(
        val bitmap: Bitmap,
        val widthDp: Float,
    )

    data class LegendEntry(
        val pill: Pill,
        val text: String,
    )

    companion object {
        private const val PILL_DP = 20f
        private const val LEGEND_PILL_DP = 11f

        suspend fun build(
            context: Context,
            data: AppData,
            date: LocalDate,
        ): HojeWidgetUi {
            val density = context.resources.displayMetrics.density
            val plan = data.planFor(date)
            val dayLabel = getString(Res.string.day_label, getString(date.dayOfWeek.shortLabel), date.day)
            val doses = getPluralString(Res.plurals.doses, plan.doseCount, plan.doseCount)
            return HojeWidgetUi(
                title = WidgetStyle.spaced(getString(Res.string.today)),
                meta = getString(Res.string.today_meta, dayLabel, doses),
                slots =
                    plan.slots.map { slot ->
                        val label = getString(slot.slot.label)
                        Slot(
                            slot = slot.slot,
                            label = label.uppercase(),
                            time = slot.time.hhmm(),
                            pills =
                                slot.doses.take(4).map { dose ->
                                    val alpha = if (dose.status == null) 1f else 0.35f
                                    Pill(
                                        PillBitmaps.pill(dose.medication.shape, dose.medication.color, PILL_DP, density, alpha),
                                        PILL_DP * dose.medication.shape.aspectRatio * PillBitmaps.PILL_MARGIN,
                                    )
                                },
                            description =
                                (listOf(label, slot.time.hhmm()) + slot.doses.map { it.medication.displayName }).joinToString(", "),
                        )
                    },
                legend =
                    plan.legend.map { medication ->
                        LegendEntry(
                            Pill(
                                PillBitmaps.pill(medication.shape, medication.color, LEGEND_PILL_DP, density),
                                LEGEND_PILL_DP * medication.shape.aspectRatio * PillBitmaps.PILL_MARGIN,
                            ),
                            medication.displayName,
                        )
                    },
            )
        }

        suspend fun load(context: Context): HojeWidgetUi {
            val repository = context.container.repository
            repository.load()
            return build(context, repository.data.value, Clock.System.todayIn(TimeZone.currentSystemDefault()))
        }
    }
}

class HojeWidget : GlanceAppWidget() {
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val initial = HojeWidgetUi.load(context)
        provideContent {
            val refresh by Widgets.refreshCount.collectAsStateCompat()
            val ui by produceState(initial, refresh) { value = HojeWidgetUi.load(context) }
            HojeWidgetContent(ui)
        }
    }
}

class HojeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HojeWidget()
}

// Glance composables take a GlanceModifier, which the Compose lint rules do not know.
@Suppress("ktlint:compose:modifier-missing-check")
@Composable
fun HojeWidgetContent(ui: HojeWidgetUi) {
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
        Spacer(GlanceModifier.height(8.dp))
        Row(GlanceModifier.fillMaxWidth().defaultWeight()) {
            ui.slots.forEachIndexed { index, slot ->
                if (index > 0) Spacer(GlanceModifier.width(6.dp))
                Column(
                    GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                        .background(ImageProvider(R.drawable.widget_glass))
                        .padding(vertical = 8.dp)
                        .semantics { contentDescription = slot.description }
                        .clickable(
                            actionStartActivity(
                                Intent(context, MainActivity::class.java)
                                    .putExtra(MainActivity.EXTRA_SLOT, slot.slot.name)
                                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                            ),
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(slot.label, style = WidgetStyle.slotLabel, maxLines = 1)
                    Text(slot.time, style = WidgetStyle.slotTime, maxLines = 1)
                    Spacer(GlanceModifier.defaultWeight())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        slot.pills.forEach { pill -> PillImage(pill) }
                    }
                }
            }
        }
        if (ui.legend.isNotEmpty()) {
            Spacer(GlanceModifier.height(8.dp))
            ui.legend.chunked(2).forEach { row ->
                Row(GlanceModifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 1.dp)) {
                    row.forEach { entry ->
                        Row(GlanceModifier.defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                            PillImage(entry.pill)
                            Spacer(GlanceModifier.width(4.dp))
                            Text(entry.text, style = WidgetStyle.legend, maxLines = 1)
                        }
                    }
                    if (row.size == 1) Box(GlanceModifier.defaultWeight()) {}
                }
            }
        }
    }
}

@Composable
private fun PillImage(pill: HojeWidgetUi.Pill) {
    val height = pill.bitmap.height.toFloat() / pill.bitmap.width * pill.widthDp
    Image(
        provider = ImageProvider(pill.bitmap),
        contentDescription = null,
        modifier = GlanceModifier.size(pill.widthDp.dp, height.dp),
    )
}
