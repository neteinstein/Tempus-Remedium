package org.mariacf.tempusremedium.core.model

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun newMedicationId(): String = Uuid.random().toString()

@OptIn(ExperimentalUuidApi::class)
fun newSymptomId(): String = Uuid.random().toString()
