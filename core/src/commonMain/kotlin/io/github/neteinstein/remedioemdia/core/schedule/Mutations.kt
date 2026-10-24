package io.github.neteinstein.remedioemdia.core.schedule

import io.github.neteinstein.remedioemdia.core.model.AppData
import io.github.neteinstein.remedioemdia.core.model.DoseLog
import io.github.neteinstein.remedioemdia.core.model.DoseStatus
import io.github.neteinstein.remedioemdia.core.model.Medication
import io.github.neteinstein.remedioemdia.core.model.TimeSlot
import kotlinx.datetime.LocalDate

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
    val wasTaken = previous?.status == DoseStatus.TAKEN
    val isTaken = status == DoseStatus.TAKEN
    val stockDelta =
        when {
            isTaken && !wasTaken -> -quantity
            !isTaken && wasTaken -> quantity
            else -> 0
        }
    val entry = DoseLog(medicationId, date, slot, status, quantity, nowEpochMillis)
    return copy(logs = logs.filterNot { it.isFor(medicationId, date, slot) } + entry)
        .adjustStock(medicationId, stockDelta)
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

fun AppData.refill(
    medicationId: String,
    units: Int,
): AppData = adjustStock(medicationId, units)

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
