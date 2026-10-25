package org.mariacf.tempusremedium.core.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * Visual form of a medication. The first four shapes are the ones in docs/design/reference;
 * the rest are product additions for medications that are not swallowed. The name stays
 * `PillShape` because it is stored in the JSON document.
 */
@Serializable
enum class PillShape {
    ROUND_SCORED,
    SQUARE,
    CAPSULE,
    SOFTGEL,
    INHALER,
    PATCH,
    NASAL_SPRAY,
    DROPS,
    INJECTION,
}

/**
 * Colour of a pill. The first four entries are the colours in docs/design/reference;
 * the rest are product additions so more medications can be told apart.
 */
@Serializable
enum class PillColor {
    TEAL,
    PINK,
    PEACH,
    BLUE,
    YELLOW,
    LAVENDER,
    GREEN,
    WHITE,
}

@Serializable
data class Medication(
    val id: String,
    val name: String,
    val strength: String = "",
    val shape: PillShape,
    val color: PillColor,
    val schedule: Schedule,
    val stock: Stock? = null,
    val notes: String = "",
    val archived: Boolean = false,
    /** Set for a course taken every few hours (see [Course]); its slot doses are then empty. */
    val course: Course? = null,
) {
    /** "Metformina 500mg": the name plus strength, as shown in the HOJE legend. */
    val displayName: String
        get() = if (strength.isBlank()) name else "$name $strength"
}

/**
 * Units left of a medication. When [lowThreshold] is null the threshold is
 * derived from the schedule (see [org.mariacf.tempusremedium.core.schedule.Inventory]).
 * [alertSeenOn] is the day the user last tapped the "running out" alert, which silences it
 * for the rest of that day.
 */
@Serializable
data class Stock(
    val remaining: Int,
    val lowThreshold: Int? = null,
    val alertSeenOn: LocalDate? = null,
)
