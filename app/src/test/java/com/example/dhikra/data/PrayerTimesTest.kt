package com.example.dhikra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerTimesTest {

    private val makkah = MockPrayerTimes.timetable("makkah", "2026-11-06")!!

    @Test
    fun next_isTheFirstPrayerStillAhead() {
        // Dhuhr 12:10; at 11:00 it is 70 minutes away.
        val next = makkah.next(11 * 60)
        assertEquals(Prayer.DHUHR, next.prayer)
        assertEquals(12 * 60 + 10, next.atMinutes)
        assertEquals(70, next.inMinutes)
        assertFalse(next.tomorrow)
    }

    @Test
    fun next_skipsSunrise() {
        // Between Fajr (05:12) and sunrise (06:30): the next prayer is Dhuhr, not sunrise.
        assertEquals(Prayer.DHUHR, makkah.next(6 * 60).prayer)
    }

    @Test
    fun next_atThePrayerTime_isTheFollowingOne() {
        assertEquals(Prayer.ASR, makkah.next(makkah.at(Prayer.DHUHR)).prayer)
    }

    @Test
    fun next_afterIsha_isTomorrowsFajr() {
        val next = makkah.next(23 * 60)
        assertEquals(Prayer.FAJR, next.prayer)
        assertTrue(next.tomorrow)
        assertEquals(60 + 5 * 60 + 12, next.inMinutes)
    }

    @Test
    fun everyCity_hasAllTimesInOrder() {
        MockPrayerTimes.cities.forEach { city ->
            val table = MockPrayerTimes.timetable(city.id, "2026-11-06")
            assertNotNull(city.id, table)
            val times = Prayer.entries.map { table!!.at(it) }
            assertEquals(city.id, times.sorted(), times)
        }
        assertEquals(MockPrayerTimes.cities.size, MockPrayerTimes.cities.map { it.id }.toSet().size)
    }

    @Test
    fun unknownCity_hasNoTimetable() {
        assertNull(MockPrayerTimes.timetable("atlantis", "2026-11-06"))
    }

    @Test
    fun search_ignoresHamzaAndMatchesCountry() {
        assertEquals(listOf("istanbul"), MockPrayerTimes.search("اسطنبول").map { it.id })
        assertTrue(MockPrayerTimes.search("السعودية").map { it.id }.containsAll(listOf("makkah", "riyadh")))
        assertEquals(MockPrayerTimes.cities, MockPrayerTimes.search("  "))
    }
}
