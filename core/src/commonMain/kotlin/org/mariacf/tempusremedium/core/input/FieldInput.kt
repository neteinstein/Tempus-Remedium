package org.mariacf.tempusremedium.core.input

import kotlinx.datetime.LocalTime

/**
 * Sanitising of what people type into numeric fields. The fields use a number keypad, which on many
 * keyboards has no `:` and only one of `,` / `.`, so the separators are inserted or normalised here
 * instead of being typed.
 */
object FieldInput {
    /**
     * Keeps the digits of a time being typed, at most four ("HHMM"); the field shows them as `HH:MM`.
     * A first digit that cannot start an hour ("8" -> "08") and a pair that is not an hour
     * ("25" -> "025") get a leading zero, so "805" means 08:05.
     */
    fun timeDigits(input: String): String {
        val digits = input.filter(Char::isDigit)
        val padded =
            when {
                digits.isNotEmpty() && digits[0] > '2' -> "0$digits"
                digits.length >= 2 && digits.take(2).toInt() > 23 -> "0$digits"
                else -> digits
            }
        return padded.take(4)
    }

    /** "2000" -> "20:00", "205" -> "20:5", "20" -> "20". */
    fun formatTimeDigits(digits: String): String = if (digits.length > 2) digits.take(2) + ":" + digits.drop(2) else digits

    /** "2000" -> 20:00; null unless there are exactly four digits forming a valid time. */
    fun parseTimeDigits(digits: String): LocalTime? {
        if (digits.length != 4 || !digits.all(Char::isDigit)) return null
        val hour = digits.take(2).toInt()
        val minute = digits.drop(2).toInt()
        return if (hour in 0..23 && minute in 0..59) LocalTime(hour, minute) else null
    }

    /** 08:05 -> "0805". */
    fun timeToDigits(time: LocalTime): String = time.hour.toString().padStart(2, '0') + time.minute.toString().padStart(2, '0')

    /**
     * Keeps digits and a single decimal separator (`,` or `.`, the first one typed), at most
     * [maxLength] characters: "1,2.5" -> "1,25".
     */
    fun decimal(
        input: String,
        maxLength: Int = 6,
    ): String {
        var separatorSeen = false
        return buildString {
            input.forEach { c ->
                when {
                    c.isDigit() -> {
                        append(c)
                    }

                    (c == ',' || c == '.') && !separatorSeen -> {
                        separatorSeen = true
                        append(c)
                    }
                }
            }
        }.take(maxLength)
    }

    /** Digits only, at most [maxLength]. */
    fun integer(
        input: String,
        maxLength: Int = 4,
    ): String = input.filter(Char::isDigit).take(maxLength)
}
