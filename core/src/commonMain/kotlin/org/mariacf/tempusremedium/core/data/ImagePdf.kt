package org.mariacf.tempusremedium.core.data

/** A JPEG image of one page, [width] × [height] pixels. */
class JpegPage(
    val width: Int,
    val height: Int,
    val jpeg: ByteArray,
)

/**
 * A PDF with one page per image, each stretched over a [pageWidth] × [pageHeight] point page.
 * The web has no PDF drawing of its own, so it draws the medications sheet into images and
 * wraps them with this.
 */
fun imagePdf(
    pages: List<JpegPage>,
    pageWidth: Float,
    pageHeight: Float,
): ByteArray {
    val out = PdfBytes()
    val offsets = mutableListOf<Int>()

    fun startObject() {
        offsets += out.size
        out.text("${offsets.size} 0 obj\n")
    }

    // 1: catalog, 2: page tree, then page, image and content objects (3 per page).
    val pageObjects = pages.indices.map { 3 + it * 3 }
    val width = pageWidth.pdfNumber()
    val height = pageHeight.pdfNumber()
    out.text("%PDF-1.4\n%âãÏÓ\n")
    startObject()
    out.text("<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")
    startObject()
    out.text("<< /Type /Pages /Kids [${pageObjects.joinToString(" ") { "$it 0 R" }}] /Count ${pages.size} >>\nendobj\n")
    pages.forEachIndexed { index, page ->
        val pageObject = pageObjects[index]
        startObject()
        out.text(
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 $width $height] " +
                "/Resources << /XObject << /Im$index ${pageObject + 1} 0 R >> >> /Contents ${pageObject + 2} 0 R >>\nendobj\n",
        )
        startObject()
        out.text(
            "<< /Type /XObject /Subtype /Image /Width ${page.width} /Height ${page.height} /ColorSpace /DeviceRGB " +
                "/BitsPerComponent 8 /Filter /DCTDecode /Length ${page.jpeg.size} >>\nstream\n",
        )
        out.bytes(page.jpeg)
        out.text("\nendstream\nendobj\n")
        val content = "q $width 0 0 $height 0 0 cm /Im$index Do Q"
        startObject()
        out.text("<< /Length ${content.length} >>\nstream\n$content\nendstream\nendobj\n")
    }
    val xref = out.size
    out.text("xref\n0 ${offsets.size + 1}\n0000000000 65535 f \n")
    offsets.forEach { out.text("${it.toString().padStart(10, '0')} 00000 n \n") }
    out.text("trailer\n<< /Size ${offsets.size + 1} /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n")
    return out.toByteArray()
}

private fun Float.pdfNumber(): String = if (this % 1f == 0f) toInt().toString() else toString()

private class PdfBytes {
    private val chunks = mutableListOf<ByteArray>()
    var size = 0
        private set

    /** PDF syntax is Latin-1: every char is one byte. */
    fun text(value: String) = bytes(ByteArray(value.length) { value[it].code.toByte() })

    fun bytes(value: ByteArray) {
        chunks += value
        size += value.size
    }

    fun toByteArray(): ByteArray {
        val result = ByteArray(size)
        var at = 0
        chunks.forEach {
            it.copyInto(result, at)
            at += it.size
        }
        return result
    }
}
