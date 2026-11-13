package com.example.dhikra.ui.today

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerTimeCardTest {

    @Test
    fun theShareOfADaytimePeriod() {
        // Dhuhr 11:58 to Asr 13:52 (114 minutes), at 12:55: half way.
        assertEquals(0.5f, periodShare(11 * 60 + 58, 13 * 60 + 52, 12 * 60 + 55), 0.01f)
        assertEquals(0f, periodShare(11 * 60 + 58, 13 * 60 + 52, 11 * 60 + 58), 0.001f)
    }

    @Test
    fun theNightPeriodRunsPastMidnight() {
        // Isha 17:46 to Fajr 05:24 (698 minutes).
        val isha = 17 * 60 + 46
        val fajr = 5 * 60 + 24
        assertEquals(134f / 698f, periodShare(isha, fajr, 20 * 60), 0.001f) // 20:00, before midnight
        assertEquals(374f / 698f, periodShare(isha, fajr, 0), 0.001f)       // midnight
        assertEquals(1f, periodShare(isha, fajr, fajr), 0.001f)
        assertEquals(324, minutesLeft(fajr, 0))
    }

    @Test
    fun itTurnsRedInTheLastFifteenMinutes() {
        assertFalse(isClosing(16))
        assertTrue(isClosing(15))
        assertTrue(isClosing(0))
        assertEquals(15, minutesLeft(13 * 60 + 52, 13 * 60 + 37))
    }

    @Test
    fun crowdedLabelsTakeTwoRows() {
        // Asr, Maghrib and Isha close together: the middle one drops to the second row.
        assertEquals(listOf(0, 1, 0), labelRows(listOf(100f, 120f, 140f), minGap = 30f))
        // Far enough apart: one row, whatever their order.
        assertEquals(listOf(0, 0, 0), labelRows(listOf(300f, 0f, 150f), minGap = 30f))
    }
}
