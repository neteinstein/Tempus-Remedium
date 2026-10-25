package org.mariacf.tempusremedium.core.schedule

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.mariacf.tempusremedium.core.model.AppData
import org.mariacf.tempusremedium.core.model.Frequency
import org.mariacf.tempusremedium.core.model.Medication
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
        medication.course?.let { course -> return course.stockUnitsPerDose * 24.0 / course.intervalHours.coerceAtLeast(1) }
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

/**
 * Medications whose stock runs out today or tomorrow: the units left do not cover the doses
 * still pending today plus tomorrow's doses.
 */
fun AppData.runningOut(today: LocalDate): List<Medication> {
    val pendingToday = planFor(today).slots.flatMap { slot -> slot.doses.filter { it.status == null } }
    val tomorrow = planFor(today.plus(DatePeriod(days = 1))).slots.flatMap { it.doses }
    return activeMedications.filter { medication ->
        val stock = medication.stock ?: return@filter false
        val needed = (pendingToday + tomorrow).filter { it.medication.id == medication.id }.sumOf { it.quantity }
        stock.remaining == 0 || stock.remaining <= needed
    }
}

/** [runningOut] minus the ones whose alert the user already tapped [today]. */
fun AppData.stockAlerts(today: LocalDate): Set<String> =
    runningOut(today)
        .filter { it.stock?.alertSeenOn != today }
        .mapTo(mutableSetOf()) { it.id }
