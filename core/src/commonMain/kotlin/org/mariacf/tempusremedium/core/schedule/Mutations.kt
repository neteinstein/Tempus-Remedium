package org.mariacf.tempusremedium.core.schedule

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.mariacf.tempusremedium.core.model.AppData
import org.mariacf.tempusremedium.core.model.CourseDoseLog
import org.mariacf.tempusremedium.core.model.DoseLog
import org.mariacf.tempusremedium.core.model.DoseStatus
import org.mariacf.tempusremedium.core.model.Medication
import org.mariacf.tempusremedium.core.model.TimeSlot

/*
 * Pure state transitions. Stock follows the logs: marking a dose TAKEN removes its
 * units, undoing or changing it to SKIPPED gives them back.
 */

fun AppData.upsertMedication(medication: Medication): AppData =
    if (medications.any { it.id == medication.id }) {
        copy(medications = medications.map { if (it.id == medication.id) medication else it })
    } else {
        copy(medications = medications + medication)
    }

/** Removes a medication and its history. */
fun AppData.deleteMedication(id: String): AppData =
    copy(
        medications = medications.filterNot { it.id == id },
        logs = logs.filterNot { it.medicationId == id },
        courseLogs = courseLogs.filterNot { it.medicationId == id },
    )

fun AppData.logDose(
    medicationId: String,
    date: LocalDate,
    slot: TimeSlot,
    status: DoseStatus,
    nowEpochMillis: Long,
): AppData {
    val medication = medication(medicationId) ?: return this
    val previous = log(medicationId, date, slot)
    val quantity = previous?.quantity ?: medication.quantityAt(slot).coerceAtLeast(1)
    val entry = DoseLog(medicationId, date, slot, status, quantity, nowEpochMillis)
    return copy(logs = logs.filterNot { it.isFor(medicationId, date, slot) } + entry)
        .adjustStock(medicationId, stockDelta(previous?.status, status, quantity))
}

/** Units to add to the stock when a dose goes from [before] to [after]. */
private fun stockDelta(
    before: DoseStatus?,
    after: DoseStatus,
    quantity: Int,
): Int {
    val wasTaken = before == DoseStatus.TAKEN
    val isTaken = after == DoseStatus.TAKEN
    return when {
        isTaken && !wasTaken -> -quantity
        !isTaken && wasTaken -> quantity
        else -> 0
    }
}

fun AppData.undoDose(
    medicationId: String,
    date: LocalDate,
    slot: TimeSlot,
): AppData {
    val previous = log(medicationId, date, slot) ?: return this
    val stockDelta = if (previous.status == DoseStatus.TAKEN) previous.quantity else 0
    return copy(logs = logs - previous).adjustStock(medicationId, stockDelta)
}

/** Marks every pending dose of a slot as [status] (used by "Tomei" on a reminder). */
fun AppData.logSlot(
    date: LocalDate,
    slot: TimeSlot,
    status: DoseStatus,
    nowEpochMillis: Long,
): AppData =
    planFor(date)
        .slot(slot)
        .doses
        .filter { it.status == null }
        .fold(this) { data, dose -> data.logDose(dose.medication.id, date, slot, status, nowEpochMillis) }

/** Logs dose [index] of a course; stock follows as for slot doses. */
fun AppData.logCourseDose(
    medicationId: String,
    index: Int,
    status: DoseStatus,
    nowEpochMillis: Long,
): AppData {
    val course = medication(medicationId)?.course ?: return this
    val previous = courseLog(medicationId, index)
    val quantity = previous?.quantity ?: course.stockUnitsPerDose
    val entry = CourseDoseLog(medicationId, index, status, quantity, nowEpochMillis)
    return copy(courseLogs = courseLogs.filterNot { it.medicationId == medicationId && it.index == index } + entry)
        .adjustStock(medicationId, stockDelta(previous?.status, status, quantity))
}

fun AppData.undoCourseDose(
    medicationId: String,
    index: Int,
): AppData {
    val previous = courseLog(medicationId, index) ?: return this
    val stockDelta = if (previous.status == DoseStatus.TAKEN) previous.quantity else 0
    return copy(courseLogs = courseLogs - previous).adjustStock(medicationId, stockDelta)
}

/** Logs every pending course dose due at [at] (used by "Tomei" on a course reminder). */
fun AppData.logCourseDosesAt(
    at: LocalDateTime,
    status: DoseStatus,
    nowEpochMillis: Long,
): AppData =
    courseDosesOn(at.date)
        .filter { it.at == at && it.status == null }
        .fold(this) { data, dose -> data.logCourseDose(dose.medication.id, dose.index, status, nowEpochMillis) }

fun AppData.refill(
    medicationId: String,
    units: Int,
): AppData = adjustStock(medicationId, units)

/** Silences the "running out" alert of a medication until the end of [today]. */
fun AppData.acknowledgeStockAlert(
    medicationId: String,
    today: LocalDate,
): AppData =
    copy(
        medications =
            medications.map { medication ->
                val stock = medication.stock
                if (medication.id != medicationId || stock == null) medication else medication.copy(stock = stock.copy(alertSeenOn = today))
            },
    )

private fun AppData.adjustStock(
    medicationId: String,
    delta: Int,
): AppData {
    if (delta == 0) return this
    return copy(
        medications =
            medications.map { medication ->
                val stock = medication.stock
                if (medication.id != medicationId || stock == null) {
                    medication
                } else {
                    medication.copy(stock = stock.copy(remaining = (stock.remaining + delta).coerceAtLeast(0)))
                }
            },
    )
}

private fun DoseLog.isFor(
    medicationId: String,
    date: LocalDate,
    slot: TimeSlot,
) = this.medicationId == medicationId && this.date == date && this.slot == slot
