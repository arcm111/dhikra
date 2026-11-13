package com.example.dhikra.data

import java.util.TimeZone

/**
 * The clock a section's fixed time is read on: the user's own, or Madinah's. Sections tied to a
 * prayer always follow the local city's prayer times.
 */
enum class SectionZone(val key: String) {
    LOCAL("local"),
    MADINAH("madinah");

    companion object {
        fun fromKey(key: String?): SectionZone = entries.firstOrNull { it.key == key } ?: LOCAL
    }
}

/** Converts a time of day between Madinah's clock and the local one, daylight saving included. */
object SectionClock {
    /** Madinah's time zone (Saudi Arabia keeps one zone, without daylight saving). */
    val MADINAH: TimeZone = TimeZone.getTimeZone("Asia/Riyadh")

    private const val MINUTES_PER_DAY = 24 * 60

    /** How many minutes Madinah is ahead of [local] at the moment [now]. */
    fun shift(local: TimeZone, now: Long): Int = (MADINAH.getOffset(now) - local.getOffset(now)) / 60_000

    /** [minutes] on Madinah's clock, read on [local]'s clock at [now] (wrapping past midnight). */
    fun madinahToLocal(minutes: Int, local: TimeZone, now: Long = System.currentTimeMillis()): Int =
        Math.floorMod(minutes - shift(local, now), MINUTES_PER_DAY)

    /** [minutes] on [local]'s clock, read on Madinah's at [now]. */
    fun localToMadinah(minutes: Int, local: TimeZone, now: Long = System.currentTimeMillis()): Int =
        Math.floorMod(minutes + shift(local, now), MINUTES_PER_DAY)
}
