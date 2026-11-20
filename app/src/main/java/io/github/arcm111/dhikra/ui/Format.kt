package io.github.arcm111.dhikra.ui

import io.github.arcm111.dhikra.data.ArabicText

/** A number of the interface in the app's language: Arabic-Indic digits in Arabic, Latin in English. */
fun uiDigits(value: String): String =
    if (AppLanguage.current == AppLanguage.AR) ArabicText.toArabicDigits(value) else ArabicText.toLatinDigits(value)

fun uiDigits(value: Int): String = uiDigits(value.toString())

/**
 * A time of day, everywhere in the app: 24-hour, "١٣:٠٤" / "٥:٢٤" in Arabic, "13:04" / "5:24" in
 * English (no leading zero: the Arabic zero is a dot; a time past midnight wraps).
 */
fun formatClock(minutesOfDay: Int): String {
    val minutes = Math.floorMod(minutesOfDay, 24 * 60)
    return uiDigits((minutes / 60).toString() + ":" + (minutes % 60).toString().padStart(2, '0'))
}
