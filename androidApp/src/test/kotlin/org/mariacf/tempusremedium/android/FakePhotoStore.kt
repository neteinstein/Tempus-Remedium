package org.mariacf.tempusremedium.android

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import org.mariacf.tempusremedium.ui.PhotoResult
import org.mariacf.tempusremedium.ui.PhotoStore
import java.io.ByteArrayOutputStream

/** Photos in memory: [take] and [pick] hand out [results] in turn (Cancelled once they run out). */
class FakePhotoStore(
    override val canTakePhoto: Boolean = true,
    results: List<PhotoResult> = emptyList(),
    val stored: MutableMap<String, ByteArray> = mutableMapOf(),
) : PhotoStore {
    private val pending = ArrayDeque(results)
    val deleted = mutableListOf<String>()

    override suspend fun take(): PhotoResult = next()

    override suspend fun pick(): PhotoResult = next()

    private fun next(): PhotoResult =
        (pending.removeFirstOrNull() ?: PhotoResult.Cancelled).also {
            if (it is PhotoResult.Saved) stored.getOrPut(it.id) { samplePhoto() }
        }

    override suspend fun load(id: String): ByteArray? = stored[id]

    override suspend fun delete(id: String) {
        deleted += id
        stored.remove(id)
    }
}

/** A stand-in photo: skin tones with a few red spots, like a rash someone would photograph. */
fun samplePhoto(
    width: Int = 800,
    height: Int = 600,
): ByteArray {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), 0xFFF1C9A8.toInt(), 0xFFD9A47F.toInt(), Shader.TileMode.CLAMP)
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    paint.shader = null
    paint.color = 0xB0C8484A.toInt()
    listOf(0.3f to 0.4f, 0.45f to 0.55f, 0.6f to 0.35f, 0.52f to 0.7f, 0.38f to 0.62f).forEachIndexed { index, (x, y) ->
        canvas.drawCircle(x * width, y * height, height * (0.03f + index % 3 * 0.012f), paint)
    }
    return ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }.toByteArray()
}
