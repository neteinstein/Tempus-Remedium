package org.mariacf.tempusremedium.core.schedule

import kotlinx.datetime.LocalDate
import org.mariacf.tempusremedium.core.model.Symptom

/** Symptoms by day, the most recent day first and, within a day, the most recent first (the Sintomas list). */
fun List<Symptom>.newestFirstByDay(): List<Pair<LocalDate, List<Symptom>>> = sortedByDescending { it.at }.groupBy { it.at.date }.toList()

/** The symptoms of each day, in the order they happened (Histórico). */
fun List<Symptom>.byDayInOrder(): Map<LocalDate, List<Symptom>> = sortedBy { it.at }.groupBy { it.at.date }

/**
 * Photos to delete once the editor of a symptom closes. [original] is the photo the symptom had,
 * [current] the one in the editor. Saved, the replaced original goes; closed without saving, a
 * newly picked photo goes. A deleted symptom takes both with it.
 */
fun discardedPhotos(
    original: String?,
    current: String?,
    outcome: EditorOutcome,
): Set<String> =
    when (outcome) {
        EditorOutcome.SAVED -> setOfNotNull(original.takeIf { it != current })
        EditorOutcome.CLOSED -> setOfNotNull(current.takeIf { it != original })
        EditorOutcome.DELETED -> setOfNotNull(original, current)
    }

enum class EditorOutcome {
    SAVED,
    CLOSED,
    DELETED,
}
