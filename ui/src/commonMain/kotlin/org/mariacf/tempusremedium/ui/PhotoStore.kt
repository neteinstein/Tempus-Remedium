package org.mariacf.tempusremedium.ui

/**
 * Photos attached to symptoms, kept by the platform next to the data (Android: the app's files;
 * web: the browser's IndexedDB). A photo is stored, already reduced to JPEG, as soon as it is
 * taken or picked, and referred to by the id these functions return.
 */
interface PhotoStore {
    /** Whether [take] can open a camera (Android with one). The web's picker offers it itself. */
    val canTakePhoto: Boolean

    /** Takes a photo with the camera and stores it. */
    suspend fun take(): PhotoResult

    /** Picks a photo from the gallery or files and stores it. */
    suspend fun pick(): PhotoResult

    /** The JPEG bytes of a stored photo, or null when it is gone. */
    suspend fun load(id: String): ByteArray?

    suspend fun delete(id: String)
}

sealed interface PhotoResult {
    data class Saved(
        val id: String,
    ) : PhotoResult

    /** The user backed out: nothing to say. */
    data object Cancelled : PhotoResult

    /** The image could not be read or stored. */
    data object Failed : PhotoResult
}
