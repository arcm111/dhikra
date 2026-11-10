package com.example.dhikra.ui

import com.example.dhikra.data.ArabicText

/** A number of the interface in the app's language: Arabic-Indic digits in Arabic, Latin in English. */
fun uiDigits(value: String): String =
    if (AppLanguage.current == AppLanguage.AR) ArabicText.toArabicDigits(value) else ArabicText.toLatinDigits(value)

fun uiDigits(value: Int): String = uiDigits(value.toString())

/**
 * A 12-hour time of day in the app's language: "٥:١٥ ص" in Arabic, "5:15 AM" in English (a time past
 * midnight wraps to the next day).
 */
fun formatTime(minutesOfDay: Int): String {
    val minutes = Math.floorMod(minutesOfDay, 24 * 60)
    val h = minutes / 60
    val m = minutes % 60
    val h12 = (h % 12).let { if (it == 0) 12 else it }
    val time = "$h12:" + m.toString().padStart(2, '0')
    return if (AppLanguage.current == AppLanguage.AR) {
        ArabicText.toArabicDigits(time) + " " + (if (h < 12) "ص" else "م")
    } else {
        time + " " + (if (h < 12) "AM" else "PM")
    }
}

/**
 * A 24-hour time of day, as a calendar's column shows it: "١٣:٠٤" / "٥:٢٤" in Arabic, "13:04" /
 * "5:24" in English (no leading zero: the Arabic zero is a dot).
 */
fun formatClock(minutesOfDay: Int): String {
    val minutes = Math.floorMod(minutesOfDay, 24 * 60)
    return uiDigits((minutes / 60).toString() + ":" + (minutes % 60).toString().padStart(2, '0'))
}
