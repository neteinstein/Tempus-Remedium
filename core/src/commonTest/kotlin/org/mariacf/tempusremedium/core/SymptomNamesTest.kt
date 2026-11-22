package org.mariacf.tempusremedium.core

import org.mariacf.tempusremedium.core.input.SymptomNames
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SymptomNamesTest {
    @Test
    fun splitsOnCommasAndDropsBlanks() {
        assertEquals(listOf("Náuseas", "Vómitos"), SymptomNames.split(" Náuseas ,, Vómitos, "))
        assertEquals(emptyList(), SymptomNames.split("  "))
    }

    @Test
    fun containsIgnoresCaseAndAccents() {
        assertTrue(SymptomNames.contains("Náuseas, Vómitos", "vomitos"))
        assertFalse(SymptomNames.contains("Náuseas, Vómitos", "Tosse"))
        assertFalse(SymptomNames.contains("Dor de cabeça forte", "Dor de cabeça"))
    }

    @Test
    fun toggleAddsAtTheEndAndRemoves() {
        assertEquals("Vómitos", SymptomNames.toggle("", "Vómitos"))
        assertEquals("Náuseas, Vómitos", SymptomNames.toggle("Náuseas", "Vómitos"))
        assertEquals("Náuseas", SymptomNames.toggle("Náuseas, vómitos", "Vómitos"))
        assertEquals("", SymptomNames.toggle("Vómitos", "Vómitos"))
        assertEquals("dor nas costas, Tosse", SymptomNames.toggle("dor nas costas,", "Tosse"))
    }

    @Test
    fun normalizeJoinsWithCommaAndSpace() {
        assertEquals("Náuseas, Vómitos", SymptomNames.normalize(" Náuseas ,Vómitos , "))
    }
}
