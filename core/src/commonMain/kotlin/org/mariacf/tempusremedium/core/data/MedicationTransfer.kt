package org.mariacf.tempusremedium.core.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import org.mariacf.tempusremedium.core.model.AppData
import org.mariacf.tempusremedium.core.model.Medication
import org.mariacf.tempusremedium.core.schedule.upsertMedication

/** Name of an exported medications file. */
const val MEDICATIONS_FILE_NAME = "tempus-remedium-medicamentos.json"

/** Name of the medications sheet, the PDF to read and print. */
const val MEDICATIONS_PDF_NAME = "tempus-remedium-medicamentos.pdf"

const val MEDICATIONS_FILE_TYPE = "application/json"

/**
 * A file of medications to move to another phone or keep as a copy: the medications only,
 * without the history of doses or the settings.
 */
@Serializable
data class MedicationsFile(
    // No default: it tells an export from any other JSON file.
    val format: String,
    val version: Int = VERSION,
    val medications: List<Medication>,
) {
    companion object {
        const val FORMAT = "tempus-remedium/medications"
        const val VERSION = 1
    }
}

/** [medications] as an export file. Today's silenced stock alert stays behind. */
fun exportMedications(medications: List<Medication>): String {
    val file =
        MedicationsFile(
            format = MedicationsFile.FORMAT,
            medications = medications.map { it.copy(stock = it.stock?.copy(alertSeenOn = null)) },
        )
    return MedicationRepository.json.encodeToString(MedicationsFile.serializer(), file)
}

/** The medications of an export file, or null when [text] is not one. */
fun parseMedicationsFile(text: String): List<Medication>? {
    val file =
        try {
            MedicationRepository.json.decodeFromString(MedicationsFile.serializer(), text)
        } catch (e: SerializationException) {
            return null
        } catch (e: IllegalArgumentException) {
            return null
        }
    return file.medications.takeIf { file.format == MedicationsFile.FORMAT }
}

/**
 * Adds [imported] to the list. One with the id of a medication already here (a file exported
 * from this phone) replaces it and keeps its history.
 */
fun AppData.importMedications(imported: List<Medication>): AppData =
    imported.fold(this) { data, medication -> data.upsertMedication(medication) }
