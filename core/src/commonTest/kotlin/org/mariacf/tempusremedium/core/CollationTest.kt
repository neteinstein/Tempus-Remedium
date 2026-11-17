package org.mariacf.tempusremedium.core

import org.mariacf.tempusremedium.core.input.Collation
import kotlin.test.Test
import kotlin.test.assertEquals

class CollationTest {
    @Test
    fun ignoresCaseAndAccents() {
        assertEquals("acido", Collation.key("Ácido"))
        assertEquals("ubelkeit", Collation.key("Übelkeit"))
        assertEquals("strasse", Collation.key("Straße"))
        assertEquals("perte d'appetit", Collation.key("Perte d’appétit"))
    }

    @Test
    fun sortsPortugueseSymptoms() {
        val sorted =
            Collation.sort(
                listOf("Vómitos", "Obstipação", "Náuseas", "Boca seca", "Ácido", "dor de cabeça", "Dor de barriga"),
            ) { it }
        assertEquals(listOf("Ácido", "Boca seca", "Dor de barriga", "dor de cabeça", "Náuseas", "Obstipação", "Vómitos"), sorted)
    }

    @Test
    fun sortsGermanUmlautsWithTheirVowel() {
        assertEquals(listOf("Atemnot", "Übelkeit", "Verstopfung"), Collation.sort(listOf("Verstopfung", "Übelkeit", "Atemnot")) { it })
    }
}
