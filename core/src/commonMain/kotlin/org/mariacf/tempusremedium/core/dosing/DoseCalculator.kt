package org.mariacf.tempusremedium.core.dosing

import kotlin.math.floor

/*
 * Dose suggestions for antibiotic courses, ported from the author's paediatric dose calculator
 * (https://mariacf.pt/calculadora-doses/, assets/js/doses-core.js: MEDICAMENTOS and
 * calcularPorDia). The numbers are hers; keep them in step with that file. Results are only
 * suggestions: the user confirms or changes them before a course is saved.
 *
 * Only the antibiotics (grupo "antibiotico") are ported: a daily dose in mg/kg/day, split by the
 * doses of the day, measured in mL of oral suspension.
 */

/** Why a daily dose was chosen; the UI shows the author's description of each. */
enum class DoseIndication {
    AMOXICILLIN_TONSILLITIS,
    AMOXICILLIN_HIGH,
    CO_AMOXICLAV_USUAL,
    CO_AMOXICLAV_HIGH,
    AZITHROMYCIN_USUAL,
    AZITHROMYCIN_STREP_5_DAYS,
    AZITHROMYCIN_STREP_3_DAYS,
    CEFUROXIME_USUAL,
    CEFUROXIME_OTITIS,
}

/** A predefined daily dose. [maxMgPerDay] overrides the medication's own maximum. */
data class DosePreset(
    val mgPerKgDay: Double,
    val indication: DoseIndication,
    val maxMgPerDay: Double? = null,
    /** Course length the author gives with this dose, if any. */
    val days: Int? = null,
)

/**
 * An oral suspension: [mgPerMl] of the active substance, plus clavulanic acid for
 * amoxicillin + clavulanic acid. [intervalHours] is the formulation's usual interval.
 */
data class Formulation(
    val mgPerMl: Double,
    val clavPerMl: Double? = null,
    /** "7:1", "14:1" or "4:1". */
    val ratio: String? = null,
    val intervalHours: Int? = null,
    val maxClavMgPerKgDay: Double? = null,
) {
    /** "250 mg/5 mL", "400 + 57 mg/5 mL": the amounts per 5 mL, as on the label. */
    val mgPer5Ml: Double get() = mgPerMl * 5
    val clavPer5Ml: Double? get() = clavPerMl?.let { it * 5 }
}

enum class CalculatorMedication(
    /** Lower-case, accent-free stems matched against what the user types. */
    val keywords: List<String>,
    val presets: List<DosePreset>,
    val minMgPerKgDay: Double,
    val maxMgPerKgDay: Double,
    /** Allowed intervals in hours, the default first. */
    val intervals: List<Int>,
    val maxMgPerDay: Double,
    val minWeightKg: Double,
    val formulations: List<Formulation>,
) {
    AMOXICILLIN(
        keywords = listOf("amoxicilina", "amoxicillin"),
        presets =
            listOf(
                DosePreset(50.0, DoseIndication.AMOXICILLIN_TONSILLITIS, maxMgPerDay = 1000.0, days = 10),
                DosePreset(90.0, DoseIndication.AMOXICILLIN_HIGH),
            ),
        minMgPerKgDay = 20.0,
        maxMgPerKgDay = 100.0,
        intervals = listOf(12, 8),
        maxMgPerDay = 3000.0,
        minWeightKg = 3.0,
        formulations = listOf(Formulation(50.0), Formulation(100.0)),
    ),
    CO_AMOXICLAV(
        keywords = listOf("amoxicilina", "amoxicillin", "acido", "clavulanico", "clavulanic", "clavulanato", "co-amoxiclav"),
        presets =
            listOf(
                DosePreset(45.0, DoseIndication.CO_AMOXICLAV_USUAL),
                DosePreset(90.0, DoseIndication.CO_AMOXICLAV_HIGH),
            ),
        minMgPerKgDay = 20.0,
        maxMgPerKgDay = 100.0,
        intervals = listOf(12, 8),
        maxMgPerDay = 3000.0,
        minWeightKg = 3.0,
        formulations =
            listOf(
                Formulation(80.0, clavPerMl = 11.4, ratio = "7:1", intervalHours = 12, maxClavMgPerKgDay = 10.0),
                Formulation(120.0, clavPerMl = 8.58, ratio = "14:1", intervalHours = 12, maxClavMgPerKgDay = 10.0),
                Formulation(25.0, clavPerMl = 6.25, ratio = "4:1", intervalHours = 8, maxClavMgPerKgDay = 15.0),
                Formulation(50.0, clavPerMl = 12.5, ratio = "4:1", intervalHours = 8, maxClavMgPerKgDay = 15.0),
            ),
    ),
    AZITHROMYCIN(
        keywords = listOf("azitromicina", "azithromycin"),
        presets =
            listOf(
                DosePreset(10.0, DoseIndication.AZITHROMYCIN_USUAL, days = 3),
                DosePreset(12.0, DoseIndication.AZITHROMYCIN_STREP_5_DAYS, days = 5),
                DosePreset(20.0, DoseIndication.AZITHROMYCIN_STREP_3_DAYS, days = 3),
            ),
        minMgPerKgDay = 5.0,
        maxMgPerKgDay = 20.0,
        intervals = listOf(24),
        maxMgPerDay = 500.0,
        minWeightKg = 5.0,
        formulations = listOf(Formulation(40.0, intervalHours = 24)),
    ),
    CEFUROXIME(
        keywords = listOf("cefuroxima", "cefuroxime"),
        presets =
            listOf(
                DosePreset(20.0, DoseIndication.CEFUROXIME_USUAL, maxMgPerDay = 250.0),
                DosePreset(30.0, DoseIndication.CEFUROXIME_OTITIS),
            ),
        minMgPerKgDay = 10.0,
        maxMgPerKgDay = 30.0,
        intervals = listOf(12),
        maxMgPerDay = 500.0,
        minWeightKg = 5.0,
        formulations = listOf(Formulation(25.0, intervalHours = 12), Formulation(50.0, intervalHours = 12)),
    ),
    ;

    /** The interval to start with for [formulation] (intervaloDaFormulacao). */
    fun defaultInterval(formulation: Formulation?): Int = formulation?.intervalHours ?: intervals.first()
}

/** A dose worked out from weight, medication, suspension and daily dose. */
data class DoseSuggestion(
    val medication: CalculatorMedication,
    /** Null for a concentration typed by hand. */
    val formulation: Formulation?,
    val weightKg: Double,
    val mgPerMl: Double,
    val mgPerKgDay: Double,
    val intervalHours: Int,
    val dosesPerDay: Int,
    /** Substance per dose (rounded like the author's calculator). */
    val mgPerDose: Double,
    /** Volume per dose, to one decimal. */
    val ml: Double,
    val mlPerDay: Double,
    val mgPerDay: Double,
    /** The daily maximum capped the dose. */
    val limited: Boolean,
    val maxMgPerDay: Double,
    val clavMgPerDose: Double?,
    val clavMgPerKgDay: Double?,
    val maxClavMgPerKgDay: Double,
    val clavExcessive: Boolean,
    /** From 40 kg the adult dose is used; the UI says so. */
    val adultWeight: Boolean,
    /** Course length the author gives with the chosen daily dose, if any. */
    val suggestedDays: Int?,
) {
    /** Above 45 mg/kg/day the 14:1 suspension is preferred over the 7:1. */
    val prefer14To1: Boolean
        get() = formulation?.ratio == "7:1" && mgPerKgDay > 45 && !clavExcessive

    /** Under 2–3 months (about 5 kg) only the 4:1 suspension is recommended. */
    val ratioNotForInfants: Boolean
        get() = formulation?.ratio != null && formulation.ratio != "4:1" && weightKg < 5
}

sealed interface DoseCalculation {
    data class Suggested(
        val dose: DoseSuggestion,
    ) : DoseCalculation

    data object WeightMissing : DoseCalculation

    data class WeightOutOfRange(
        val minKg: Double,
        val maxKg: Double,
    ) : DoseCalculation

    data class BelowMinimumWeight(
        val minKg: Double,
    ) : DoseCalculation

    data object InvalidConcentration : DoseCalculation

    data class DoseOutOfRange(
        val minMgPerKgDay: Double,
        val maxMgPerKgDay: Double,
    ) : DoseCalculation

    data object InvalidInterval : DoseCalculation

    /** The dose comes to less than 0.1 mL: the concentration is probably wrong. */
    data object VolumeTooSmall : DoseCalculation
}

object DoseCalculator {
    const val MIN_WEIGHT_KG = 3.0
    const val MAX_WEIGHT_KG = 60.0
    const val ADULT_WEIGHT_KG = 40.0
    private const val DEFAULT_MAX_CLAV_MG_PER_KG_DAY = 10.0

    /**
     * Medications matching what the user typed: each word of 3 letters or more that starts a
     * keyword (or a word that a keyword starts) counts; best matches first.
     */
    fun match(query: String): List<CalculatorMedication> {
        val words = normalize(query).split(' ', '+', '/').filter { it.length >= 3 }
        return CalculatorMedication.entries
            .map { medication ->
                medication to words.count { word -> medication.keywords.any { it.startsWith(word) || word.startsWith(it) } }
            }.filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    /**
     * The author's calcularDose for a daily-dose antibiotic. [mgPerKgDay] and [intervalHours]
     * default to the first preset and the formulation's interval. [customDose] means the daily
     * dose was typed by hand, so no preset maximum applies; [customConcentration] means
     * [mgPerMl] is not one of the listed suspensions.
     */
    fun calculate(
        weightKg: Double?,
        medication: CalculatorMedication,
        mgPerMl: Double,
        mgPerKgDay: Double? = null,
        intervalHours: Int? = null,
        customDose: Boolean = false,
        customConcentration: Boolean = false,
    ): DoseCalculation {
        if (weightKg == null || !weightKg.isFinite() || weightKg <= 0) return DoseCalculation.WeightMissing
        if (weightKg < MIN_WEIGHT_KG || weightKg > MAX_WEIGHT_KG) return DoseCalculation.WeightOutOfRange(MIN_WEIGHT_KG, MAX_WEIGHT_KG)
        if (weightKg < medication.minWeightKg) return DoseCalculation.BelowMinimumWeight(medication.minWeightKg)
        if (!mgPerMl.isFinite() || mgPerMl <= 0) return DoseCalculation.InvalidConcentration

        val formulation = if (customConcentration) null else medication.formulations.firstOrNull { it.mgPerMl == mgPerMl }
        val daily = mgPerKgDay ?: medication.presets.first().mgPerKgDay
        if (!daily.isFinite() || daily < medication.minMgPerKgDay || daily > medication.maxMgPerKgDay) {
            return DoseCalculation.DoseOutOfRange(medication.minMgPerKgDay, medication.maxMgPerKgDay)
        }
        val interval = intervalHours ?: medication.defaultInterval(formulation)
        if (interval !in medication.intervals) return DoseCalculation.InvalidInterval
        val dosesPerDay = 24 / interval

        // A preset can carry its own daily maximum (e.g. amoxicillin for tonsillitis, 1 g/day).
        val preset = if (customDose) null else medication.presets.firstOrNull { it.mgPerKgDay == daily }
        val maxMgPerDay = preset?.maxMgPerDay ?: medication.maxMgPerDay
        val computedMgPerDay = weightKg * daily
        val limited = computedMgPerDay > maxMgPerDay
        val mgPerDose = minOf(computedMgPerDay, maxMgPerDay) / dosesPerDay
        val ml = round1(mgPerDose / mgPerMl)
        if (ml <= 0) return DoseCalculation.VolumeTooSmall

        // Clavulanic acid is only known for the listed suspensions.
        val clavPerMl = formulation?.clavPerMl
        val clavMgPerDose = clavPerMl?.let { ml * it }
        val clavMgPerKgDay = clavMgPerDose?.let { it * dosesPerDay / weightKg }
        val maxClav = formulation?.maxClavMgPerKgDay ?: DEFAULT_MAX_CLAV_MG_PER_KG_DAY

        return DoseCalculation.Suggested(
            DoseSuggestion(
                medication = medication,
                formulation = formulation,
                weightKg = weightKg,
                mgPerMl = mgPerMl,
                mgPerKgDay = daily,
                intervalHours = interval,
                dosesPerDay = dosesPerDay,
                mgPerDose = if (round1(mgPerDose) >= 100) jsRound(mgPerDose) else round1(mgPerDose),
                ml = ml,
                mlPerDay = round1(ml * dosesPerDay),
                mgPerDay = round1(mgPerDose * dosesPerDay),
                limited = limited,
                maxMgPerDay = maxMgPerDay,
                clavMgPerDose = clavMgPerDose?.let(::round1),
                clavMgPerKgDay = clavMgPerKgDay?.let(::round1),
                maxClavMgPerKgDay = maxClav,
                clavExcessive = clavMgPerKgDay != null && clavMgPerKgDay > maxClav + 0.05,
                adultWeight = weightKg >= ADULT_WEIGHT_KG,
                suggestedDays = preset?.days,
            ),
        )
    }

    /** JavaScript's Math.round: halves go up, unlike Kotlin's round (half to even). */
    private fun jsRound(value: Double): Double = floor(value + 0.5)

    private fun round1(value: Double): Double = jsRound(value * 10) / 10

    private fun normalize(text: String): String =
        text
            .lowercase()
            .map { ACCENTS[it] ?: it }
            .joinToString("")
            .trim()

    private val ACCENTS =
        mapOf(
            'á' to 'a',
            'à' to 'a',
            'â' to 'a',
            'ã' to 'a',
            'é' to 'e',
            'ê' to 'e',
            'í' to 'i',
            'ó' to 'o',
            'ô' to 'o',
            'õ' to 'o',
            'ú' to 'u',
            'ç' to 'c',
        )
}
