package org.mariacf.tempusremedium.core

import kotlinx.datetime.LocalTime
import org.mariacf.tempusremedium.core.input.FieldInput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FieldInputTest {
    @Test
    fun timeDigitsDropsSeparatorsAndKeepsFourDigits() {
        assertEquals("2000", FieldInput.timeDigits("20:00"))
        assertEquals("2000", FieldInput.timeDigits("2000"))
        assertEquals("2000", FieldInput.timeDigits("20:001"))
        assertEquals("", FieldInput.timeDigits(":"))
    }

    @Test
    fun timeDigitsPadsDigitsThatCannotStartAnHour() {
        assertEquals("08", FieldInput.timeDigits("8"))
        assertEquals("0805", FieldInput.timeDigits("805"))
        assertEquals("025", FieldInput.timeDigits("25"))
        assertEquals("1", FieldInput.timeDigits("1"))
        assertEquals("23", FieldInput.timeDigits("23"))
    }

    @Test
    fun formatTimeDigitsInsertsTheColon() {
        assertEquals("", FieldInput.formatTimeDigits(""))
        assertEquals("20", FieldInput.formatTimeDigits("20"))
        assertEquals("20:5", FieldInput.formatTimeDigits("205"))
        assertEquals("20:00", FieldInput.formatTimeDigits("2000"))
    }

    @Test
    fun parseTimeDigitsNeedsAFullValidTime() {
        assertEquals(LocalTime(20, 0), FieldInput.parseTimeDigits("2000"))
        assertEquals(LocalTime(0, 5), FieldInput.parseTimeDigits("0005"))
        assertNull(FieldInput.parseTimeDigits("205"))
        assertNull(FieldInput.parseTimeDigits("2460"))
        assertNull(FieldInput.parseTimeDigits("2075"))
        assertNull(FieldInput.parseTimeDigits("20:0"))
    }

    @Test
    fun timeToDigitsRoundTrips() {
        assertEquals("0805", FieldInput.timeToDigits(LocalTime(8, 5)))
        assertEquals(LocalTime(8, 5), FieldInput.parseTimeDigits(FieldInput.timeToDigits(LocalTime(8, 5))))
    }

    @Test
    fun decimalKeepsOneSeparator() {
        assertEquals("1,25", FieldInput.decimal("1,2.5"))
        assertEquals("6.5", FieldInput.decimal("6.5"))
        assertEquals("12", FieldInput.decimal("1a2"))
        assertEquals("12345", FieldInput.decimal("123456", maxLength = 5))
    }

    @Test
    fun integerKeepsDigits() {
        assertEquals("30", FieldInput.integer("3-0"))
        assertEquals("1234", FieldInput.integer("12345"))
    }
}
