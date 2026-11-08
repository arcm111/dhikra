package com.example.dhikra.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class PrayerTriggerTest {

    private val zone = TimeZone.getTimeZone("Asia/Riyadh")

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Calendar =
        Calendar.getInstance(zone).apply {
            clear()
            set(year, month - 1, day, hour, minute, 0)
        }

    // Fajr moves by a minute a day: 05:12 on the 8th, 05:13 on the 9th.
    private val fajr = mapOf("2026-11-08" to 5 * 60 + 12, "2026-11-09" to 5 * 60 + 13)

    @Test
    fun todaysTimeWhileItIsStillAhead() {
        val trigger = ReminderScheduler.nextPrayerTrigger(at(2026, 11, 8, 3, 0)) { fajr[it] }
        assertEquals(at(2026, 11, 8, 5, 12).timeInMillis, trigger)
    }

    @Test
    fun tomorrowsOwnTimeOnceTodaysHasPassed() {
        val trigger = ReminderScheduler.nextPrayerTrigger(at(2026, 11, 8, 6, 0)) { fajr[it] }
        assertEquals(at(2026, 11, 9, 5, 13).timeInMillis, trigger)
    }

    @Test
    fun atTheVeryMinute_itIsTomorrows() {
        // The alarm that just fired schedules the next one: not the same minute again.
        val trigger = ReminderScheduler.nextPrayerTrigger(at(2026, 11, 8, 5, 12)) { fajr[it] }
        assertEquals(at(2026, 11, 9, 5, 13).timeInMillis, trigger)
    }

    @Test
    fun noTimesNoTrigger() {
        assertNull(ReminderScheduler.nextPrayerTrigger(at(2026, 11, 8, 3, 0)) { null })
    }
}
