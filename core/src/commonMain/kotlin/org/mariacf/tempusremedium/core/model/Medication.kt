package org.mariacf.tempusremedium.core.model

import kotlinx.serialization.Serializable

/** Visual form of a medication. The four shapes are the ones in docs/design/reference. */
@Serializable
enum class PillShape {
    ROUND_SCORED,
    SQUARE,
    CAPSULE,
    SOFTGEL,
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
) {
    /** "Metformina 500mg": the name plus strength, as shown in the HOJE legend. */
    val displayName: String
        get() = if (strength.isBlank()) name else "$name $strength"
}

/**
 * Units left of a medication. When [lowThreshold] is null the threshold is
 * derived from the schedule (see [org.mariacf.tempusremedium.core.schedule.Inventory]).
 */
@Serializable
data class Stock(
    val remaining: Int,
    val lowThreshold: Int? = null,
)
