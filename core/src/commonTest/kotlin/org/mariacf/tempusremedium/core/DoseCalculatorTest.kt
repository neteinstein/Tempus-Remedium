package org.mariacf.tempusremedium.core

import org.mariacf.tempusremedium.core.dosing.CalculatorMedication
import org.mariacf.tempusremedium.core.dosing.CalculatorMedication.AMOXICILLIN
import org.mariacf.tempusremedium.core.dosing.CalculatorMedication.AZITHROMYCIN
import org.mariacf.tempusremedium.core.dosing.CalculatorMedication.CEFUROXIME
import org.mariacf.tempusremedium.core.dosing.CalculatorMedication.CO_AMOXICLAV
import org.mariacf.tempusremedium.core.dosing.DoseCalculation
import org.mariacf.tempusremedium.core.dosing.DoseCalculator
import org.mariacf.tempusremedium.core.dosing.DoseSuggestion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Expected values come from the author's calculator (mariacf.pt/calculadora-doses). */
class DoseCalculatorTest {
    private fun suggest(
        weightKg: Double?,
        medication: CalculatorMedication,
        mgPerMl: Double,
        mgPerKgDay: Double? = null,
        intervalHours: Int? = null,
        customDose: Boolean = false,
        customConcentration: Boolean = false,
    ): DoseSuggestion {
        val result = DoseCalculator.calculate(weightKg, medication, mgPerMl, mgPerKgDay, intervalHours, customDose, customConcentration)
        return assertIs<DoseCalculation.Suggested>(result).dose
    }

    @Test
    fun amoxicillinForTonsillitis() {
        // ?peso=12&med=amoxicilina&c=50: 50 mg/kg/dia, de 12/12 h.
        val dose = suggest(12.0, AMOXICILLIN, 50.0)
        assertEquals(12, dose.intervalHours)
        assertEquals(2, dose.dosesPerDay)
        assertEquals(300.0, dose.mgPerDose)
        assertEquals(6.0, dose.ml)
        assertEquals(12.0, dose.mlPerDay)
        assertEquals(600.0, dose.mgPerDay)
        assertEquals(10, dose.suggestedDays)
        assertFalse(dose.limited)
        assertNull(dose.clavMgPerDose)
    }

    @Test
    fun theTonsillitisDoseIsCappedAtOneGramADay() {
        val dose = suggest(25.0, AMOXICILLIN, 50.0)
        assertTrue(dose.limited)
        assertEquals(1000.0, dose.maxMgPerDay)
        assertEquals(500.0, dose.mgPerDose)
        assertEquals(10.0, dose.ml)
        // Typed by hand, only the 3 g/day maximum applies.
        val custom = suggest(25.0, AMOXICILLIN, 50.0, mgPerKgDay = 50.0, customDose = true)
        assertFalse(custom.limited)
        assertEquals(625.0, custom.mgPerDose)
        assertNull(custom.suggestedDays)
    }

    @Test
    fun highDoseAmoxicillinEveryEightHours() {
        val dose = suggest(12.0, AMOXICILLIN, 100.0, mgPerKgDay = 90.0, intervalHours = 8)
        assertEquals(3, dose.dosesPerDay)
        assertEquals(360.0, dose.mgPerDose)
        assertEquals(3.6, dose.ml)
        assertNull(dose.suggestedDays)
    }

    @Test
    fun coAmoxiclavShowsTheClavulanicAcid() {
        val dose = suggest(12.0, CO_AMOXICLAV, 80.0)
        assertEquals("7:1", dose.formulation!!.ratio)
        assertEquals(12, dose.intervalHours)
        assertEquals(270.0, dose.mgPerDose)
        assertEquals(3.4, dose.ml)
        assertEquals(38.8, dose.clavMgPerDose)
        assertEquals(6.5, dose.clavMgPerKgDay)
        assertFalse(dose.clavExcessive)
        assertFalse(dose.prefer14To1)
        assertEquals(400.0, dose.formulation.mgPer5Ml)
        assertEquals(57.0, dose.formulation.clavPer5Ml!!, 1e-9)
    }

    @Test
    fun tooMuchClavulanicAcidIsFlagged() {
        // 4:1 at a high dose: 22,5 mg/kg/dia of clavulanic acid, above 15.
        assertEquals(8, CO_AMOXICLAV.defaultInterval(CO_AMOXICLAV.formulations[2]))
        val dose = suggest(10.0, CO_AMOXICLAV, 25.0, mgPerKgDay = 90.0)
        assertEquals(8, dose.intervalHours)
        assertEquals(12.0, dose.ml)
        assertEquals(22.5, dose.clavMgPerKgDay)
        assertTrue(dose.clavExcessive)
    }

    @Test
    fun formulationNotices() {
        assertTrue(suggest(12.0, CO_AMOXICLAV, 80.0, mgPerKgDay = 60.0).prefer14To1)
        assertTrue(suggest(4.0, CO_AMOXICLAV, 80.0).ratioNotForInfants)
        assertFalse(suggest(4.0, CO_AMOXICLAV, 25.0).ratioNotForInfants)
        assertTrue(suggest(40.0, AMOXICILLIN, 100.0, mgPerKgDay = 90.0).adultWeight)
        assertFalse(suggest(39.9, AMOXICILLIN, 100.0, mgPerKgDay = 90.0).adultWeight)
        // A concentration typed by hand has no known clavulanic acid.
        val custom = suggest(12.0, CO_AMOXICLAV, 80.0, customConcentration = true)
        assertNull(custom.formulation)
        assertNull(custom.clavMgPerDose)
        assertEquals(12, custom.intervalHours)
    }

    @Test
    fun azithromycinOnceADayWithItsCourseLength() {
        val usual = suggest(12.0, AZITHROMYCIN, 40.0)
        assertEquals(24, usual.intervalHours)
        assertEquals(120.0, usual.mgPerDose)
        assertEquals(3.0, usual.ml)
        assertEquals(3, usual.suggestedDays)
        assertEquals(5, suggest(12.0, AZITHROMYCIN, 40.0, mgPerKgDay = 12.0).suggestedDays)
        val capped = suggest(30.0, AZITHROMYCIN, 40.0, mgPerKgDay = 20.0)
        assertTrue(capped.limited)
        assertEquals(500.0, capped.mgPerDose)
        assertEquals(12.5, capped.ml)
    }

    @Test
    fun cefuroximeKeepsItsPerDoseMaximum() {
        val usual = suggest(15.0, CEFUROXIME, 25.0)
        assertTrue(usual.limited)
        assertEquals(125.0, usual.mgPerDose)
        assertEquals(5.0, usual.ml)
        val otitis = suggest(15.0, CEFUROXIME, 50.0, mgPerKgDay = 30.0)
        assertEquals(225.0, otitis.mgPerDose)
        assertEquals(4.5, otitis.ml)
    }

    @Test
    fun halvesRoundUpLikeTheCalculator() {
        // 225 mg / 100 mg/mL = 2,25 mL -> 2,3 mL (Math.round), not 2,2.
        assertEquals(2.3, suggest(9.0, AMOXICILLIN, 100.0).ml)
        // Small doses keep one decimal of mg.
        assertEquals(37.5, suggest(3.0, AMOXICILLIN, 50.0, mgPerKgDay = 25.0).mgPerDose)
    }

    @Test
    fun unsafeInputsGiveAReasonInsteadOfADose() {
        assertEquals(DoseCalculation.WeightMissing, DoseCalculator.calculate(null, AMOXICILLIN, 50.0))
        assertEquals(DoseCalculation.WeightMissing, DoseCalculator.calculate(0.0, AMOXICILLIN, 50.0))
        assertEquals(DoseCalculation.WeightMissing, DoseCalculator.calculate(Double.NaN, AMOXICILLIN, 50.0))
        assertEquals(DoseCalculation.WeightOutOfRange(3.0, 60.0), DoseCalculator.calculate(2.0, AMOXICILLIN, 50.0))
        assertEquals(DoseCalculation.WeightOutOfRange(3.0, 60.0), DoseCalculator.calculate(70.0, AMOXICILLIN, 50.0))
        assertEquals(DoseCalculation.BelowMinimumWeight(5.0), DoseCalculator.calculate(4.0, AZITHROMYCIN, 40.0))
        assertEquals(DoseCalculation.InvalidConcentration, DoseCalculator.calculate(12.0, AMOXICILLIN, 0.0))
        assertEquals(DoseCalculation.DoseOutOfRange(20.0, 100.0), DoseCalculator.calculate(12.0, AMOXICILLIN, 50.0, mgPerKgDay = 150.0))
        assertEquals(DoseCalculation.DoseOutOfRange(20.0, 100.0), DoseCalculator.calculate(12.0, AMOXICILLIN, 50.0, mgPerKgDay = 10.0))
        assertEquals(DoseCalculation.InvalidInterval, DoseCalculator.calculate(12.0, AMOXICILLIN, 50.0, intervalHours = 6))
        assertEquals(
            DoseCalculation.VolumeTooSmall,
            DoseCalculator.calculate(3.0, AMOXICILLIN, 10_000.0, mgPerKgDay = 20.0, customConcentration = true),
        )
    }

    @Test
    fun suggestionsFollowWhatTheUserTypes() {
        assertEquals(listOf(AMOXICILLIN, CO_AMOXICLAV), DoseCalculator.match("amox"))
        assertEquals(CO_AMOXICLAV, DoseCalculator.match("Amoxicilina + ácido clavulânico").first())
        assertEquals(listOf(AZITHROMYCIN), DoseCalculator.match("Azitro"))
        assertEquals(listOf(CEFUROXIME), DoseCalculator.match("Cefuroxime 250"))
        assertTrue(DoseCalculator.match("ab").isEmpty())
        assertTrue(DoseCalculator.match("paracetamol").isEmpty())
    }
}
