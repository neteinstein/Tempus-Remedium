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
import io.github.neteinstein.remedioemdia.ui.resources.long_date
import io.github.neteinstein.remedioemdia.ui.resources.month_april
import io.github.neteinstein.remedioemdia.ui.resources.month_august
import io.github.neteinstein.remedioemdia.ui.resources.month_december
import io.github.neteinstein.remedioemdia.ui.resources.month_february
import io.github.neteinstein.remedioemdia.ui.resources.month_january
import io.github.neteinstein.remedioemdia.ui.resources.month_july
import io.github.neteinstein.remedioemdia.ui.resources.month_june
import io.github.neteinstein.remedioemdia.ui.resources.month_march
import io.github.neteinstein.remedioemdia.ui.resources.month_may
import io.github.neteinstein.remedioemdia.ui.resources.month_november
import io.github.neteinstein.remedioemdia.ui.resources.month_october
import io.github.neteinstein.remedioemdia.ui.resources.month_september
import io.github.neteinstein.remedioemdia.ui.resources.shape_capsule
import io.github.neteinstein.remedioemdia.ui.resources.shape_round_scored
import io.github.neteinstein.remedioemdia.ui.resources.shape_softgel
import io.github.neteinstein.remedioemdia.ui.resources.shape_square
import io.github.neteinstein.remedioemdia.ui.resources.slot_bedtime
import io.github.neteinstein.remedioemdia.ui.resources.slot_evening
import io.github.neteinstein.remedioemdia.ui.resources.slot_midday
import io.github.neteinstein.remedioemdia.ui.resources.slot_morning
import io.github.neteinstein.remedioemdia.ui.resources.weekday_friday
import io.github.neteinstein.remedioemdia.ui.resources.weekday_full_friday
import io.github.neteinstein.remedioemdia.ui.resources.weekday_full_monday
import io.github.neteinstein.remedioemdia.ui.resources.weekday_full_saturday
import io.github.neteinstein.remedioemdia.ui.resources.weekday_full_sunday
import io.github.neteinstein.remedioemdia.ui.resources.weekday_full_thursday
import io.github.neteinstein.remedioemdia.ui.resources.weekday_full_tuesday
import io.github.neteinstein.remedioemdia.ui.resources.weekday_full_wednesday
import io.github.neteinstein.remedioemdia.ui.resources.weekday_monday
import io.github.neteinstein.remedioemdia.ui.resources.weekday_saturday
import io.github.neteinstein.remedioemdia.ui.resources.weekday_sunday
import io.github.neteinstein.remedioemdia.ui.resources.weekday_thursday
import io.github.neteinstein.remedioemdia.ui.resources.weekday_tuesday
import io.github.neteinstein.remedioemdia.ui.resources.weekday_wednesday
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
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

val DayOfWeek.fullLabel: StringResource
    get() =
        when (this) {
            DayOfWeek.MONDAY -> Res.string.weekday_full_monday
            DayOfWeek.TUESDAY -> Res.string.weekday_full_tuesday
            DayOfWeek.WEDNESDAY -> Res.string.weekday_full_wednesday
            DayOfWeek.THURSDAY -> Res.string.weekday_full_thursday
            DayOfWeek.FRIDAY -> Res.string.weekday_full_friday
            DayOfWeek.SATURDAY -> Res.string.weekday_full_saturday
            DayOfWeek.SUNDAY -> Res.string.weekday_full_sunday
        }

private val Month.label: StringResource
    get() =
        when (this) {
            Month.JANUARY -> Res.string.month_january
            Month.FEBRUARY -> Res.string.month_february
            Month.MARCH -> Res.string.month_march
            Month.APRIL -> Res.string.month_april
            Month.MAY -> Res.string.month_may
            Month.JUNE -> Res.string.month_june
            Month.JULY -> Res.string.month_july
            Month.AUGUST -> Res.string.month_august
            Month.SEPTEMBER -> Res.string.month_september
            Month.OCTOBER -> Res.string.month_october
            Month.NOVEMBER -> Res.string.month_november
            Month.DECEMBER -> Res.string.month_december
        }

/** "quinta-feira, 22 de outubro" */
@Composable
fun longDateLabel(date: LocalDate): String =
    stringResource(Res.string.long_date, stringResource(date.dayOfWeek.fullLabel), date.day, stringResource(date.month.label))

/** "qui., 22" */
@Composable
fun dayLabel(date: LocalDate): String = stringResource(Res.string.day_label, stringResource(date.dayOfWeek.shortLabel), date.day)

/** "08:00": 24-hour time, as in the reference. */
fun LocalTime.hhmm(): String = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
