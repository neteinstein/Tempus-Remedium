package org.mariacf.tempusremedium.core.model

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.serializers.DayOfWeekSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The four moments of the day shown in the HOJE panel, in display order. */
@Serializable
enum class TimeSlot(
    val defaultTime: LocalTime,
) {
    MORNING(LocalTime(8, 0)),
    MIDDAY(LocalTime(13, 0)),
    EVENING(LocalTime(19, 0)),
    BEDTIME(LocalTime(22, 0)),
}

@Serializable
data class Schedule(
    /** Units to take at each slot; slots that are absent (or zero) have no dose. */
    val doses: Map<TimeSlot, Int>,
    val frequency: Frequency = Frequency.Daily,
    val startDate: LocalDate,
    /** Last day it may be due, inclusive. */
    val endDate: LocalDate? = null,
    /** Stops after this many due days, counted from [startDate] ("Após 10 vezes"). */
    val occurrences: Int? = null,
)

@Serializable
sealed interface Frequency {
    @Serializable
    @SerialName("daily")
    data object Daily : Frequency

    /**
     * On [days] of every [everyWeeks]th week, counting weeks (Monday to Sunday) from the one
     * that holds [Schedule.startDate].
     */
    @Serializable
    @SerialName("weekdays")
    data class Weekdays(
        val days: Set<
            @Serializable(with = DayOfWeekSerializer::class)
            DayOfWeek,
        >,
        val everyWeeks: Int = 1,
    ) : Frequency

    /** Every [interval] days, counted from [Schedule.startDate]. */
    @Serializable
    @SerialName("every_n_days")
    data class EveryNDays(
        val interval: Int,
    ) : Frequency

    /**
     * Every [everyMonths] months on the day of the month of [Schedule.startDate]; in shorter
     * months, on their last day (a start on the 31st falls on 30 April and 28 February).
     */
    @Serializable
    @SerialName("monthly")
    data class Monthly(
        val everyMonths: Int = 1,
    ) : Frequency

    /** [daysOn] days taken then [daysOff] days of pause, repeating from [Schedule.startDate] (e.g. 21 + 7). */
    @Serializable
    @SerialName("cycle")
    data class Cycle(
        val daysOn: Int,
        val daysOff: Int,
    ) : Frequency
}
