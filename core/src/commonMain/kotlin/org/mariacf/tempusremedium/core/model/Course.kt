package org.mariacf.tempusremedium.core.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.ceil
import kotlin.math.floor

/** What one dose of a course is measured in. */
@Serializable
enum class DoseUnit {
    /** Oral suspensions and syrups. */
    ML,

    /** Tablets, capsules, sachets: these use the medication's stock. */
    UNITS,
}

/** When a course stops. */
@Serializable
sealed interface CourseEnd {
    /** The doses that fall within [days] days of the first one. */
    @Serializable
    @SerialName("days")
    data class AfterDays(
        val days: Int,
    ) : CourseEnd

    /** Exactly [doses] doses, taken or not. */
    @Serializable
    @SerialName("doses")
    data class AfterDoses(
        val doses: Int,
    ) : CourseEnd

    /** Every dose up to and including [date]. */
    @Serializable
    @SerialName("until")
    data class OnDate(
        val date: LocalDate,
    ) : CourseEnd

    /**
     * "Até acabar": until a pack of [packAmount] (in the course's unit) is used up.
     * A skipped dose does not use the pack, so it adds one more dose at the end.
     */
    @Serializable
    @SerialName("finish_pack")
    data class FinishPack(
        val packAmount: Double,
    ) : CourseEnd
}

/**
 * A course of treatment taken every [intervalHours] hours from [firstDose], e.g. an antibiotic
 * every 8 hours for 7 days. Dose `k` is due at `firstDose + k × intervalHours` (wall-clock time).
 */
@Serializable
data class Course(
    val firstDose: LocalDateTime,
    val intervalHours: Int,
    /** Amount of each dose, in [unit] (6.5 mL, ½ tablet...). */
    val amount: Double,
    val unit: DoseUnit,
    val end: CourseEnd,
) {
    /** Stock units used by one dose: whole tablets started, nothing for liquids. */
    val stockUnitsPerDose: Int
        get() = if (unit == DoseUnit.UNITS) ceil(amount - EPSILON).toInt().coerceAtLeast(1) else 0

    /** Doses a pack holds, for [CourseEnd.FinishPack]. */
    fun dosesIn(packAmount: Double): Int = if (amount <= 0) 0 else floor(packAmount / amount + EPSILON).toInt()

    private companion object {
        const val EPSILON = 1e-9
    }
}

/** What happened to dose number [index] of a medication's course. */
@Serializable
data class CourseDoseLog(
    val medicationId: String,
    val index: Int,
    val status: DoseStatus,
    val quantity: Int,
    val loggedAtEpochMillis: Long,
)
