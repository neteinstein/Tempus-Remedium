package org.mariacf.tempusremedium.core

import org.mariacf.tempusremedium.core.data.JpegPage
import org.mariacf.tempusremedium.core.data.imagePdf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImagePdfTest {
    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 1, 2, 3, 0xFF.toByte(), 0xD9.toByte())

    private fun latin1(bytes: ByteArray) = bytes.joinToString("") { (it.toInt() and 0xFF).toChar().toString() }

    @Test
    fun onePagePerImage() {
        val pdf = latin1(imagePdf(listOf(JpegPage(10, 20, jpeg), JpegPage(10, 20, jpeg)), 595f, 842f))
        assertTrue(pdf.startsWith("%PDF-1.4"))
        assertTrue(pdf.trimEnd().endsWith("%%EOF"))
        assertTrue("/Count 2" in pdf)
        assertTrue("/MediaBox [0 0 595 842]" in pdf)
        assertEquals(2, Regex("/Subtype /Image /Width 10 /Height 20").findAll(pdf).count())
        assertTrue("q 595 0 0 842 0 0 cm /Im1 Do Q" in pdf)
    }

    @Test
    fun crossReferencePointsAtEveryObject() {
        val pdf = latin1(imagePdf(listOf(JpegPage(10, 20, jpeg)), 595f, 842f))
        val xrefAt = pdf.substringAfterLast("startxref\n").substringBefore("\n").toInt()
        assertTrue(pdf.startsWith("xref", xrefAt))
        val offsets =
            pdf
                .substring(xrefAt)
                .lines()
                .filter { it.endsWith(" 00000 n ") }
                .map { it.take(10).toInt() }
        assertEquals(5, offsets.size)
        offsets.forEachIndexed { index, offset -> assertTrue(pdf.startsWith("${index + 1} 0 obj", offset)) }
    }
}
