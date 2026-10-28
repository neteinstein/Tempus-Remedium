package org.mariacf.tempusremedium.android

import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mariacf.tempusremedium.ui.FileTransfer
import org.mariacf.tempusremedium.ui.sheet.SHEET_HEIGHT
import org.mariacf.tempusremedium.ui.sheet.SHEET_WIDTH
import org.mariacf.tempusremedium.ui.sheet.SheetPage
import java.io.ByteArrayOutputStream

/**
 * Saves through the system's "save as" screen and opens through its file picker (no storage
 * permission needed). Create it while [activity] is being created: it registers for results.
 */
class AndroidFileTransfer(
    private val activity: ComponentActivity,
) : FileTransfer {
    private val create = Pending<Uri?>()
    private val pick = Pending<Uri?>()

    // The type is set per call, so the contract is built with a placeholder and asks with the name only.
    private var saveType = "application/octet-stream"
    private val createLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(
            object : ActivityResultContract<String, Uri?>() {
                override fun createIntent(
                    context: android.content.Context,
                    input: String,
                ) = ActivityResultContracts.CreateDocument(saveType).createIntent(context, input)

                override fun parseResult(
                    resultCode: Int,
                    intent: android.content.Intent?,
                ) = ActivityResultContracts.CreateDocument(saveType).parseResult(resultCode, intent)
            },
            create::complete,
        )
    private val openLauncher = activity.registerForActivityResult(ActivityResultContracts.OpenDocument(), pick::complete)

    override suspend fun save(
        name: String,
        type: String,
        bytes: ByteArray,
    ) {
        saveType = type
        val uri = create.await { createLauncher.launch(name) } ?: return
        withContext(Dispatchers.IO) {
            activity.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) }
        }
    }

    override suspend fun open(): String? {
        // Some file managers label JSON as plain text or as bytes.
        val uri = pick.await { openLauncher.launch(arrayOf("application/json", "text/*", "application/octet-stream")) } ?: return null
        return withContext(Dispatchers.IO) {
            runCatching { activity.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } }.getOrNull()
        }
    }

    /** A vector PDF: text stays text, in the app's fonts. */
    override suspend fun pdf(pages: List<SheetPage>): ByteArray =
        withContext(Dispatchers.Default) {
            val document = PdfDocument()
            try {
                pages.forEachIndexed { index, page ->
                    val info = PdfDocument.PageInfo.Builder(SHEET_WIDTH.toInt(), SHEET_HEIGHT.toInt(), index + 1).create()
                    val pdfPage = document.startPage(info)
                    CanvasDrawScope().draw(
                        Density(1f),
                        LayoutDirection.Ltr,
                        Canvas(pdfPage.canvas),
                        Size(SHEET_WIDTH, SHEET_HEIGHT),
                    ) { page() }
                    document.finishPage(pdfPage)
                }
                ByteArrayOutputStream().also(document::writeTo).toByteArray()
            } finally {
                document.close()
            }
        }

    /** One request at a time; a second one replaces the first, which gets nothing. */
    private class Pending<T> {
        private var deferred: CompletableDeferred<T?>? = null

        fun complete(value: T?) {
            deferred?.complete(value)
            deferred = null
        }

        suspend fun await(launch: () -> Unit): T? {
            deferred?.complete(null)
            val next = CompletableDeferred<T?>()
            deferred = next
            launch()
            return next.await()
        }
    }
}
