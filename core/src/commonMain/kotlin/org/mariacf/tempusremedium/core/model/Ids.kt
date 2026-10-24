package org.mariacf.tempusremedium.core.model

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun newMedicationId(): String = Uuid.random().toString()
