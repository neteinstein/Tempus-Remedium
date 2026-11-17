package org.mariacf.tempusremedium.core.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/** Something the user felt (vomiting, dizziness, a headache...) and when it happened. */
@Serializable
data class Symptom(
    val id: String,
    /** Free text; the Sintomas screen offers common ones. */
    val name: String,
    val at: LocalDateTime,
    /** Id of an attached photo, kept by the platform outside the JSON document; null without one. */
    val photo: String? = null,
)
