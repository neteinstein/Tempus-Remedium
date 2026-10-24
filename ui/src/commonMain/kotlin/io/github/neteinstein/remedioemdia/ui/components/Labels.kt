package io.github.neteinstein.remedioemdia.ui.components

import androidx.compose.runtime.Composable
import io.github.neteinstein.remedioemdia.core.model.PillColor
import io.github.neteinstein.remedioemdia.core.model.PillShape
import io.github.neteinstein.remedioemdia.core.model.TimeSlot
import io.github.neteinstein.remedioemdia.ui.resources.Res
import io.github.neteinstein.remedioemdia.ui.resources.color_blue
import io.github.neteinstein.remedioemdia.ui.resources.color_green
import io.github.neteinstein.remedioemdia.ui.resources.color_lavender
import io.github.neteinstein.remedioemdia.ui.resources.color_peach
import io.github.neteinstein.remedioemdia.ui.resources.color_pink
import io.github.neteinstein.remedioemdia.ui.resources.color_teal
import io.github.neteinstein.remedioemdia.ui.resources.color_white
import io.github.neteinstein.remedioemdia.ui.resources.color_yellow
import io.github.neteinstein.remedioemdia.ui.resources.day_label
import io.github.neteinstein.remedioemdia.ui.resources.shape_capsule
import io.github.neteinstein.remedioemdia.ui.resources.shape_round_scored
import io.github.neteinstein.remedioemdia.ui.resources.shape_softgel
import io.github.neteinstein.remedioemdia.ui.resources.shape_square
import io.github.neteinstein.remedioemdia.ui.resources.slot_bedtime
import io.github.neteinstein.remedioemdia.ui.resources.slot_evening
import io.github.neteinstein.remedioemdia.ui.resources.slot_midday
import io.github.neteinstein.remedioemdia.ui.resources.slot_morning
import io.github.neteinstein.remedioemdia.ui.resources.weekday_friday
import io.github.neteinstein.remedioemdia.ui.resources.weekday_monday
import io.github.neteinstein.remedioemdia.ui.resources.weekday_saturday
import io.github.neteinstein.remedioemdia.ui.resources.weekday_sunday
import io.github.neteinstein.remedioemdia.ui.resources.weekday_thursday
import io.github.neteinstein.remedioemdia.ui.resources.weekday_tuesday
import io.github.neteinstein.remedioemdia.ui.resources.weekday_wednesday
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

val TimeSlot.label: StringResource
    get() =
        when (this) {
            TimeSlot.MORNING -> Res.string.slot_morning
            TimeSlot.MIDDAY -> Res.string.slot_midday
            TimeSlot.EVENING -> Res.string.slot_evening
            TimeSlot.BEDTIME -> Res.string.slot_bedtime
        }

val DayOfWeek.shortLabel: StringResource
    get() =
        when (this) {
            DayOfWeek.MONDAY -> Res.string.weekday_monday
            DayOfWeek.TUESDAY -> Res.string.weekday_tuesday
            DayOfWeek.WEDNESDAY -> Res.string.weekday_wednesday
            DayOfWeek.THURSDAY -> Res.string.weekday_thursday
            DayOfWeek.FRIDAY -> Res.string.weekday_friday
            DayOfWeek.SATURDAY -> Res.string.weekday_saturday
            DayOfWeek.SUNDAY -> Res.string.weekday_sunday
        }

val PillShape.label: StringResource
    get() =
        when (this) {
            PillShape.ROUND_SCORED -> Res.string.shape_round_scored
            PillShape.SQUARE -> Res.string.shape_square
            PillShape.CAPSULE -> Res.string.shape_capsule
            PillShape.SOFTGEL -> Res.string.shape_softgel
        }

val PillColor.label: StringResource
    get() =
        when (this) {
            PillColor.TEAL -> Res.string.color_teal
            PillColor.PINK -> Res.string.color_pink
            PillColor.PEACH -> Res.string.color_peach
            PillColor.BLUE -> Res.string.color_blue
            PillColor.YELLOW -> Res.string.color_yellow
            PillColor.LAVENDER -> Res.string.color_lavender
            PillColor.GREEN -> Res.string.color_green
            PillColor.WHITE -> Res.string.color_white
        }

/** "qui., 22" */
@Composable
fun dayLabel(date: LocalDate): String = stringResource(Res.string.day_label, stringResource(date.dayOfWeek.shortLabel), date.day)

/** "08:00": 24-hour time, as in the reference. */
fun LocalTime.hhmm(): String = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
