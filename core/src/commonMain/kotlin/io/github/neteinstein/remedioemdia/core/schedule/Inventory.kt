package io.github.neteinstein.remedioemdia.core.schedule

import io.github.neteinstein.remedioemdia.core.model.AppData
import io.github.neteinstein.remedioemdia.core.model.Frequency
import io.github.neteinstein.remedioemdia.core.model.Medication
import kotlin.math.ceil

/** A medication shown in REPOR EM BREVE. */
data class LowStockItem(
    val medication: Medication,
    val remaining: Int,
)

object Inventory {
    /** Days of supply used as the low-stock threshold when the user did not set one. */
    const val DEFAULT_THRESHOLD_DAYS = 7

    /** Average units used per day according to the schedule. */
    fun averageDailyUnits(medication: Medication): Double {
        val perDueDay =
            medication.schedule.doses.values
                .sum()
                .toDouble()
        return when (val frequency = medication.schedule.frequency) {
            Frequency.Daily -> perDueDay
            is Frequency.Weekdays -> perDueDay * frequency.days.size / 7.0
            is Frequency.EveryNDays -> perDueDay / frequency.interval.coerceAtLeast(1)
        }
    }

    fun threshold(medication: Medication): Int {
        val stock = medication.stock ?: return 0
        return stock.lowThreshold ?: ceil(averageDailyUnits(medication) * DEFAULT_THRESHOLD_DAYS).toInt()
    }

    fun isLow(medication: Medication): Boolean {
        val stock = medication.stock ?: return false
        return !medication.archived && stock.remaining <= threshold(medication)
    }
}

/** Medications that are running low, fewest units left first. */
fun AppData.lowStock(): List<LowStockItem> =
    activeMedications
        .filter(Inventory::isLow)
        .map { LowStockItem(it, it.stock!!.remaining) }
        .sortedWith(compareBy<LowStockItem> { it.remaining }.thenBy { it.medication.name })
