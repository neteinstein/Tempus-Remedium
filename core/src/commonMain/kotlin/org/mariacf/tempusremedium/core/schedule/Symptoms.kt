package org.mariacf.tempusremedium.core.schedule

import kotlinx.datetime.LocalDate
import org.mariacf.tempusremedium.core.model.Symptom

/** Symptoms by day, the most recent day first and, within a day, the most recent first (the Sintomas list). */
fun List<Symptom>.newestFirstByDay(): List<Pair<LocalDate, List<Symptom>>> = sortedByDescending { it.at }.groupBy { it.at.date }.toList()

/** The symptoms of each day, in the order they happened (Histórico). */
fun List<Symptom>.byDayInOrder(): Map<LocalDate, List<Symptom>> = sortedBy { it.at }.groupBy { it.at.date }
