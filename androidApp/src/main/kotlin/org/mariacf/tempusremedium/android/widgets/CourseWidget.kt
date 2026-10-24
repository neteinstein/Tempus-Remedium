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
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.Text
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.mariacf.tempusremedium.android.MainActivity
import org.mariacf.tempusremedium.android.R
import org.mariacf.tempusremedium.android.container
import org.mariacf.tempusremedium.core.model.AppData
import org.mariacf.tempusremedium.core.model.Course
import org.mariacf.tempusremedium.core.model.DoseUnit
import org.mariacf.tempusremedium.core.schedule.activeCourses
import org.mariacf.tempusremedium.ui.components.formatDecimal
import org.mariacf.tempusremedium.ui.components.hhmm
import org.mariacf.tempusremedium.ui.components.shortLabel
import org.mariacf.tempusremedium.ui.resources.Res
import org.mariacf.tempusremedium.ui.resources.course_next
import org.mariacf.tempusremedium.ui.resources.course_remaining
import org.mariacf.tempusremedium.ui.resources.day_label
import org.mariacf.tempusremedium.ui.resources.days_count
import org.mariacf.tempusremedium.ui.resources.decimal_separator
import org.mariacf.tempusremedium.ui.resources.dose_ml
import org.mariacf.tempusremedium.ui.resources.dose_units
import org.mariacf.tempusremedium.ui.resources.takes_count
import org.mariacf.tempusremedium.ui.resources.when_today
import org.mariacf.tempusremedium.ui.resources.when_tomorrow
import org.mariacf.tempusremedium.ui.resources.widget_course_title
import org.mariacf.tempusremedium.ui.resources.widget_no_course
import kotlin.math.ceil
import kotlin.time.Clock

/** Everything the course widget shows, resolved outside composition: the next dose and what is left. */
data class CourseWidgetUi(
    val title: String,
    val empty: String,
    val courses: List<Item>,
) {
    data class Item(
        val pill: Bitmap,
        val name: String,
        val next: String,
        val remaining: String,
    )

    companion object {
        const val PILL_DP = 30f
        private const val MAX_COURSES = 2

        suspend fun build(
            context: Context,
            data: AppData,
            now: LocalDateTime,
        ): CourseWidgetUi {
            val density = context.resources.displayMetrics.density
            return CourseWidgetUi(
                title = WidgetStyle.spaced(getString(Res.string.widget_course_title)),
                empty = getString(Res.string.widget_no_course),
                courses =
                    data.activeCourses(now).take(MAX_COURSES).map { progress ->
                        val medication = progress.medication
                        val next = progress.next!!
                        val days = progress.remainingDays(now.date)
                        Item(
                            pill = PillBitmaps.pill(medication.shape, medication.color, PILL_DP, density),
                            name = medication.displayName,
                            next =
                                getString(
                                    Res.string.course_next,
                                    relativeDay(next.at.date, now.date),
                                    next.at.time.hhmm(),
                                    doseLabel(medication.course!!),
                                ),
                            remaining =
                                getString(
                                    Res.string.course_remaining,
                                    getPluralString(Res.plurals.takes_count, progress.remaining, progress.remaining),
                                    getPluralString(Res.plurals.days_count, days, days),
                                ),
                        )
                    },
            )
        }

        suspend fun load(context: Context): CourseWidgetUi {
            val repository = context.container.repository
            repository.load()
            return build(context, repository.data.value, Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()))
        }

        private suspend fun relativeDay(
            date: LocalDate,
            today: LocalDate,
        ): String =
            when (date) {
                today -> getString(Res.string.when_today)
                today.plus(DatePeriod(days = 1)) -> getString(Res.string.when_tomorrow)
                else -> getString(Res.string.day_label, getString(date.dayOfWeek.shortLabel), date.day)
            }

        private suspend fun doseLabel(course: Course): String {
            val amount = formatDecimal(course.amount, getString(Res.string.decimal_separator))
            return when (course.unit) {
                DoseUnit.ML -> getString(Res.string.dose_ml, amount)
                DoseUnit.UNITS -> getPluralString(Res.plurals.dose_units, ceil(course.amount).toInt().coerceAtLeast(1), amount)
            }
        }
    }
}

class CourseWidget : GlanceAppWidget() {
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val initial = CourseWidgetUi.load(context)
        provideContent {
            val refresh by Widgets.refreshCount.collectAsStateCompat()
            val ui by produceState(initial, refresh) { value = CourseWidgetUi.load(context) }
            CourseWidgetContent(ui)
        }
    }
}

class CourseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CourseWidget()
}

// Glance composables take a GlanceModifier, which the Compose lint rules do not know.
@Suppress("ktlint:compose:modifier-missing-check")
@Composable
fun CourseWidgetContent(ui: CourseWidgetUi) {
    val context = LocalContext.current
    Column(
        GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_background))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .clickable(
                actionStartActivity(
                    Intent(context, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                ),
            ),
    ) {
        Text(ui.title, style = WidgetStyle.title, modifier = GlanceModifier.padding(horizontal = 4.dp))
        Spacer(GlanceModifier.height(6.dp))
        Box(GlanceModifier.fillMaxWidth().height(1.dp).background(WidgetStyle.divider)) {}
        Spacer(GlanceModifier.height(8.dp))
        if (ui.courses.isEmpty()) {
            Box(GlanceModifier.fillMaxWidth().defaultWeight(), contentAlignment = Alignment.Center) {
                Text(ui.empty, style = WidgetStyle.meta)
            }
            return@Column
        }
        ui.courses.forEach { item ->
            Row(
                GlanceModifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
                    .semantics { contentDescription = "${item.name}, ${item.next}, ${item.remaining}" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(provider = ImageProvider(item.pill), contentDescription = null)
                Spacer(GlanceModifier.width(10.dp))
                Column(GlanceModifier.defaultWeight()) {
                    Text(item.name, style = WidgetStyle.jarName, maxLines = 1)
                    Text(item.next, style = WidgetStyle.courseNext, maxLines = 1)
                    Text(item.remaining, style = WidgetStyle.meta, maxLines = 1)
                }
            }
        }
    }
}
