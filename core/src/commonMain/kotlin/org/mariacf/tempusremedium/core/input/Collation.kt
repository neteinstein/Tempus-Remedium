package org.mariacf.tempusremedium.core.input

/**
 * Alphabetical order for labels in the app's languages: case and accents are ignored, so
 * "Ácido" sits with the A and "Übelkeit" with the U (common code has no locale collator).
 */
object Collation {
    private val folded: Map<Char, String> =
        buildMap {
            fun fold(
                chars: String,
                to: String,
            ) = chars.forEach { put(it, to) }
            fold("àáâãäåā", "a")
            fold("çć", "c")
            fold("èéêëē", "e")
            fold("ìíîïī", "i")
            fold("ñ", "n")
            fold("òóôõöøō", "o")
            fold("ùúûüū", "u")
            fold("ýÿ", "y")
            fold("ß", "ss")
            fold("æ", "ae")
            fold("œ", "oe")
            // Typographic apostrophes and hyphens sort as their plain forms.
            fold("’‘", "'")
        }

    /** The key [text] sorts by. */
    fun key(text: String): String =
        buildString(text.length) {
            text.lowercase().forEach { char -> append(folded[char] ?: char.toString()) }
        }

    /** [items] in alphabetical order of [label]; equal keys keep their order. */
    fun <T> sort(
        items: List<T>,
        label: (T) -> String,
    ): List<T> = items.sortedBy { key(label(it)) }
}
