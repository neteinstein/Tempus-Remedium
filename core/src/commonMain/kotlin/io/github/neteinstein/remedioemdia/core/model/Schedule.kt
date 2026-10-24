package io.github.neteinstein.remedioemdia.core.model

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
    val endDate: LocalDate? = null,
)

@Serializable
sealed interface Frequency {
    @Serializable
    @SerialName("daily")
    data object Daily : Frequency

    @Serializable
    @SerialName("weekdays")
    data class Weekdays(
        val days: Set<
            @Serializable(with = DayOfWeekSerializer::class)
            DayOfWeek,
        >,
    ) : Frequency

    /** Every [interval] days, counted from [Schedule.startDate]. */
    @Serializable
    @SerialName("every_n_days")
    data class EveryNDays(
        val interval: Int,
    ) : Frequency
}
