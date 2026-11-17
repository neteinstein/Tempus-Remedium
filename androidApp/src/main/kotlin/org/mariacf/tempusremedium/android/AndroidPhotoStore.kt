package org.mariacf.tempusremedium.android

import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mariacf.tempusremedium.ui.PhotoResult
import org.mariacf.tempusremedium.ui.PhotoStore
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.math.max

/**
 * Symptom photos in the app's private files (`files/symptom-photos`, part of Android's backup like
 * the data). The gallery goes through the system photo picker and the camera through the camera
 * app, so neither needs a permission. Create it while [activity] is being created: it registers
 * for results.
 */
class AndroidPhotoStore(
    private val activity: ComponentActivity,
) : PhotoStore {
    private val directory get() = File(activity.filesDir, PHOTO_DIRECTORY)

    private var picked: CompletableDeferred<Uri?>? = null
    private var taken: CompletableDeferred<Boolean>? = null

    private val pickLauncher =
        activity.registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            picked?.complete(uri)
            picked = null
        }
    private val takeLauncher =
        activity.registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            taken?.complete(success)
            taken = null
        }

    override val canTakePhoto: Boolean
        get() = activity.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)

    override suspend fun take(): PhotoResult {
        // The camera app writes into a file of ours that it reaches through the FileProvider.
        val shot =
            withContext(Dispatchers.IO) {
                File(activity.cacheDir, CAMERA_DIRECTORY).apply { mkdirs() }.let { File(it, "shot.jpg") }.apply { delete() }
            }
        val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.photos", shot)
        val next = CompletableDeferred<Boolean>()
        taken?.complete(false)
        taken = next
        try {
            takeLauncher.launch(uri)
        } catch (e: ActivityNotFoundException) {
            taken = null
            return PhotoResult.Failed
        }
        if (!next.await()) return PhotoResult.Cancelled
        return try {
            store(withContext(Dispatchers.IO) { shot.readBytes() })
        } finally {
            withContext(Dispatchers.IO) { shot.delete() }
        }
    }

    override suspend fun pick(): PhotoResult {
        val next = CompletableDeferred<Uri?>()
        picked?.complete(null)
        picked = next
        pickLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        val uri = next.await() ?: return PhotoResult.Cancelled
        val bytes =
            withContext(Dispatchers.IO) {
                try {
                    activity.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } catch (e: IOException) {
                    null
                } catch (e: SecurityException) {
                    null
                }
            } ?: return PhotoResult.Failed
        return store(bytes)
    }

    override suspend fun load(id: String): ByteArray? =
        withContext(Dispatchers.IO) {
            photoFile(id)?.takeIf { it.isFile }?.readBytes()
        }

    override suspend fun delete(id: String) {
        withContext(Dispatchers.IO) { photoFile(id)?.delete() }
    }

    private suspend fun store(original: ByteArray): PhotoResult =
        withContext(Dispatchers.Default) {
            val jpeg = shrinkPhoto(original) ?: return@withContext PhotoResult.Failed
            val id = "${UUID.randomUUID()}.jpg"
            try {
                withContext(Dispatchers.IO) {
                    directory.mkdirs()
                    File(directory, id).writeBytes(jpeg)
                }
                PhotoResult.Saved(id)
            } catch (e: IOException) {
                PhotoResult.Failed
            }
        }

    /** Only names this store gave out: no path separators, so nothing outside the folder. */
    private fun photoFile(id: String): File? = id.takeIf { PHOTO_ID.matches(it) }?.let { File(directory, it) }

    companion object {
        const val PHOTO_DIRECTORY = "symptom-photos"
        private const val CAMERA_DIRECTORY = "camera"
        private val PHOTO_ID = Regex("[0-9a-f-]{36}\\.jpg")
    }
}

/** Longest side of a stored photo, in pixels: sharp on a phone, a few hundred KB as JPEG. */
const val PHOTO_MAX_SIDE = 1600
private const val PHOTO_QUALITY = 85

/**
 * [bytes] of any image Android decodes, as an upright JPEG no larger than [maxSide] on either
 * side, or null when it is not an image. Upright: the camera's EXIF rotation is applied, since
 * the copy keeps no EXIF (and so no location either).
 */
fun shrinkPhoto(
    bytes: ByteArray,
    maxSide: Int = PHOTO_MAX_SIDE,
): ByteArray? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    // Decode at a power-of-two fraction close to the target, then scale exactly.
    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
    val decoded =
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
    val scale = minOf(1f, maxSide.toFloat() / max(decoded.width, decoded.height))
    val matrix =
        Matrix().apply {
            postScale(scale, scale)
            postRotate(rotationOf(bytes).toFloat())
        }
    val upright =
        if (matrix.isIdentity) decoded else Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
    return ByteArrayOutputStream()
        .also { upright.compress(Bitmap.CompressFormat.JPEG, PHOTO_QUALITY, it) }
        .toByteArray()
        .also {
            if (upright !== decoded) upright.recycle()
            decoded.recycle()
        }
}

/** Degrees the image must turn to stand upright, from its EXIF orientation. */
private fun rotationOf(bytes: ByteArray): Int =
    try {
        when (ExifInterface(bytes.inputStream()).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    } catch (e: IOException) {
        0
    }
