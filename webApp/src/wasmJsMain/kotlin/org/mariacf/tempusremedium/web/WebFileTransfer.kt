package org.mariacf.tempusremedium.web

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.yield
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.mariacf.tempusremedium.core.data.JpegPage
import org.mariacf.tempusremedium.core.data.imagePdf
import org.mariacf.tempusremedium.ui.FileTransfer
import org.mariacf.tempusremedium.ui.sheet.SHEET_HEIGHT
import org.mariacf.tempusremedium.ui.sheet.SHEET_WIDTH
import org.mariacf.tempusremedium.ui.sheet.SheetPage
import kotlin.coroutines.resume
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** Pixels per PDF point: about 180 dpi, sharp on paper and on a phone's screen. */
private const val PAGE_SCALE = 2.5f
private const val JPEG_QUALITY = 90

/** Files on the web: downloads to save, the browser's file picker to open. */
object WebFileTransfer : FileTransfer {
    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun save(
        name: String,
        type: String,
        bytes: ByteArray,
    ) = downloadBase64File(name, type, Base64.encode(bytes))

    override suspend fun open(): String? = suspendCancellableCoroutine { continuation -> pickTextFile { continuation.resume(it) } }

    /** The browser cannot draw a PDF, so each page becomes an image of one. */
    override suspend fun pdf(pages: List<SheetPage>): ByteArray {
        val width = (SHEET_WIDTH * PAGE_SCALE).toInt()
        val height = (SHEET_HEIGHT * PAGE_SCALE).toInt()
        val images =
            pages.map { page ->
                yield()
                val bitmap = ImageBitmap(width, height)
                CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap), Size(width.toFloat(), height.toFloat())) {
                    scale(PAGE_SCALE, pivot = Offset.Zero) { page() }
                }
                val jpeg =
                    Image.makeFromBitmap(bitmap.asSkiaBitmap()).encodeToData(EncodedImageFormat.JPEG, JPEG_QUALITY)
                        ?: error("JPEG encoding failed")
                JpegPage(width, height, jpeg.bytes)
            }
        return imagePdf(images, SHEET_WIDTH, SHEET_HEIGHT)
    }
}
