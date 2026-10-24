package org.mariacf.tempusremedium.ui.components

import androidx.compose.runtime.Composable
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.mariacf.tempusremedium.core.dosing.CalculatorMedication
import org.mariacf.tempusremedium.core.dosing.DoseIndication
import org.mariacf.tempusremedium.core.model.Course
import org.mariacf.tempusremedium.core.model.DoseUnit
import org.mariacf.tempusremedium.ui.resources.Res
import org.mariacf.tempusremedium.ui.resources.calc_ind_amoxicillin_high
import org.mariacf.tempusremedium.ui.resources.calc_ind_amoxicillin_tonsillitis
import org.mariacf.tempusremedium.ui.resources.calc_ind_azithromycin_strep_3
import org.mariacf.tempusremedium.ui.resources.calc_ind_azithromycin_strep_5
import org.mariacf.tempusremedium.ui.resources.calc_ind_azithromycin_usual
import org.mariacf.tempusremedium.ui.resources.calc_ind_cefuroxime_otitis
import org.mariacf.tempusremedium.ui.resources.calc_ind_cefuroxime_usual
import org.mariacf.tempusremedium.ui.resources.calc_ind_co_amoxiclav_high
import org.mariacf.tempusremedium.ui.resources.calc_ind_co_amoxiclav_usual
import org.mariacf.tempusremedium.ui.resources.calc_med_amoxicillin
import org.mariacf.tempusremedium.ui.resources.calc_med_azithromycin
import org.mariacf.tempusremedium.ui.resources.calc_med_cefuroxime
import org.mariacf.tempusremedium.ui.resources.calc_med_co_amoxiclav
import org.mariacf.tempusremedium.ui.resources.calc_substance_amoxicillin
import org.mariacf.tempusremedium.ui.resources.calc_substance_azithromycin
import org.mariacf.tempusremedium.ui.resources.calc_substance_cefuroxime
import org.mariacf.tempusremedium.ui.resources.decimal_separator
import org.mariacf.tempusremedium.ui.resources.dose_ml
import org.mariacf.tempusremedium.ui.resources.dose_units
import org.mariacf.tempusremedium.ui.resources.interval_every
import org.mariacf.tempusremedium.ui.resources.when_today
import org.mariacf.tempusremedium.ui.resources.when_tomorrow
import kotlin.math.ceil
import kotlin.math.floor

/** "6", "6,5", "1,25": up to two decimals, rounded half up, with [separator]. */
fun formatDecimal(
    value: Double,
    separator: String,
): String {
    val hundredths = floor(value * 100 + 0.5).toLong()
    val whole = hundredths / 100
    val fraction = hundredths % 100
    return when {
        fraction == 0L -> "$whole"
        fraction % 10 == 0L -> "$whole$separator${fraction / 10}"
        else -> "$whole$separator${fraction.toString().padStart(2, '0')}"
    }
}

/** [formatDecimal] with the decimal separator of the current language. */
@Composable
fun Double.decimal(): String = formatDecimal(this, stringResource(Res.string.decimal_separator))

/** "6,5 mL", "1 unidade". */
@Composable
fun doseLabel(
    amount: Double,
    unit: DoseUnit,
): String =
    when (unit) {
        DoseUnit.ML -> stringResource(Res.string.dose_ml, amount.decimal())
        DoseUnit.UNITS -> pluralStringResource(Res.plurals.dose_units, ceil(amount).toInt().coerceAtLeast(1), amount.decimal())
    }

@Composable
fun Course.doseLabel(): String = doseLabel(amount, unit)

/** "De 8 em 8 horas". */
@Composable
fun intervalLabel(hours: Int): String = pluralStringResource(Res.plurals.interval_every, hours, hours)

/** "hoje", "amanhã" or "qui., 22", for "Próxima toma hoje às 16:00". */
@Composable
fun relativeDay(
    date: LocalDate,
    today: LocalDate,
): String =
    when (date) {
        today -> stringResource(Res.string.when_today)
        today.plus(DatePeriod(days = 1)) -> stringResource(Res.string.when_tomorrow)
        else -> dayLabel(date)
    }

val CalculatorMedication.label: StringResource
    get() =
        when (this) {
            CalculatorMedication.AMOXICILLIN -> Res.string.calc_med_amoxicillin
            CalculatorMedication.CO_AMOXICLAV -> Res.string.calc_med_co_amoxiclav
            CalculatorMedication.AZITHROMYCIN -> Res.string.calc_med_azithromycin
            CalculatorMedication.CEFUROXIME -> Res.string.calc_med_cefuroxime
        }

/** The substance the dose is measured in ("mg de amoxicilina"). */
val CalculatorMedication.substance: StringResource
    get() =
        when (this) {
            CalculatorMedication.AMOXICILLIN, CalculatorMedication.CO_AMOXICLAV -> Res.string.calc_substance_amoxicillin
            CalculatorMedication.AZITHROMYCIN -> Res.string.calc_substance_azithromycin
            CalculatorMedication.CEFUROXIME -> Res.string.calc_substance_cefuroxime
        }

val DoseIndication.label: StringResource
    get() =
        when (this) {
            DoseIndication.AMOXICILLIN_TONSILLITIS -> Res.string.calc_ind_amoxicillin_tonsillitis
            DoseIndication.AMOXICILLIN_HIGH -> Res.string.calc_ind_amoxicillin_high
            DoseIndication.CO_AMOXICLAV_USUAL -> Res.string.calc_ind_co_amoxiclav_usual
            DoseIndication.CO_AMOXICLAV_HIGH -> Res.string.calc_ind_co_amoxiclav_high
            DoseIndication.AZITHROMYCIN_USUAL -> Res.string.calc_ind_azithromycin_usual
            DoseIndication.AZITHROMYCIN_STREP_5_DAYS -> Res.string.calc_ind_azithromycin_strep_5
            DoseIndication.AZITHROMYCIN_STREP_3_DAYS -> Res.string.calc_ind_azithromycin_strep_3
            DoseIndication.CEFUROXIME_USUAL -> Res.string.calc_ind_cefuroxime_usual
            DoseIndication.CEFUROXIME_OTITIS -> Res.string.calc_ind_cefuroxime_otitis
        }
