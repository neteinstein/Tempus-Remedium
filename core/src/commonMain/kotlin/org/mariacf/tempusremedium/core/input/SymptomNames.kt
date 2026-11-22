package org.mariacf.tempusremedium.core.input

/**
 * The symptom field holds one or more names separated by commas ("Náuseas, Vómitos"), so a
 * single entry can record several symptoms felt together. The suggestion chips toggle names in
 * and out of that list; matching ignores case and accents.
 */
object SymptomNames {
    private const val SEPARATOR = ", "

    /** The names in [text], trimmed, without empty ones. */
    fun split(text: String): List<String> = text.split(',').map(String::trim).filter(String::isNotEmpty)

    /** Whether [name] is one of the names in [text]. */
    fun contains(
        text: String,
        name: String,
    ): Boolean = split(text).any { Collation.key(it) == Collation.key(name.trim()) }

    /** [text] with [name] removed if it was there, or added at the end if it was not. */
    fun toggle(
        text: String,
        name: String,
    ): String {
        val names = split(text)
        val key = Collation.key(name.trim())
        val kept = names.filterNot { Collation.key(it) == key }
        return (if (kept.size == names.size) names + name.trim() else kept).joinToString(SEPARATOR)
    }

    /** [text] as it is saved: the names joined by ", " (stray commas and spaces dropped). */
    fun normalize(text: String): String = split(text).joinToString(SEPARATOR)
}
