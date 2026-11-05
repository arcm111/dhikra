package com.example.dhikra.ui

import com.example.dhikra.data.ArabicText

/** "٥:١٥ ص" style 12-hour time with Arabic digits. */
fun formatTimeArabic(minutesOfDay: Int): String {
    val h = minutesOfDay / 60
    val m = minutesOfDay % 60
    val h12 = (h % 12).let { if (it == 0) 12 else it }
    val suffix = if (h < 12) "ص" else "م"
    return ArabicText.toArabicDigits("%d:%02d".format(h12, m)) + " " + suffix
}
