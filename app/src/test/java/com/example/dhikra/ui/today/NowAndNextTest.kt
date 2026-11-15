package com.example.dhikra.ui.today

import com.example.dhikra.data.ProgramSection
import org.junit.Assert.assertEquals
import org.junit.Test

class NowAndNextTest {

    private fun section(id: String, minutes: Int?) = ProgramSection(id = id, title = id, reminderMinutes = minutes)

    private fun ids(group: NowAndNext) = group.now.map { it.id } to group.next.map { it.id }

    // Asr 13:52 to Maghrib 16:20.
    private val asr = 13 * 60 + 52
    private val maghrib = 16 * 60 + 20
    private val day = listOf(
        section("dhuhr", 11 * 60 + 58),
        section("asr", asr),
        section("quran", 16 * 60),
        section("late", 16 * 60 + 10),
        section("maghrib", maghrib),
        section("untimed", null),
    )

    @Test
    fun theLatestDueAndTheEarliestBeforeTheNextPrayer() {
        // 15:00: Asr's section is due; the Quran session comes next (16:10's waits behind it).
        assertEquals(listOf("asr") to listOf("quran"), ids(nowAndNext(day, asr, maghrib, 15 * 60)))
        // 16:05: the Quran session is due now (the earlier Asr one is left out).
        assertEquals(listOf("quran") to listOf("late"), ids(nowAndNext(day, asr, maghrib, 16 * 60 + 5)))
    }

    @Test
    fun sectionsSharingTheTimeAllShow() {
        val withTwin = day + section("twin", 16 * 60) + section("twinLate", 16 * 60 + 10)
        assertEquals(listOf("quran", "twin") to listOf("late", "twinLate"), ids(nowAndNext(withTwin, asr, maghrib, 16 * 60 + 5)))
    }

    @Test
    fun nextGoesOnIntoTheNextPrayerTime() {
        // 16:15: nothing left in Asr's time; Maghrib's own section (16:20) is next all the same.
        assertEquals(listOf("late") to listOf("maghrib"), ids(nowAndNext(day, asr, maghrib, 16 * 60 + 15)))
    }

    @Test
    fun afterTheDaysLastSectionNextIsTomorrowsFirst() {
        // 23:00 in Isha's time: Isha's section is due; tomorrow's Dhuhr section comes next.
        val isha = 17 * 60 + 46
        val fajr = 5 * 60 + 24
        val evening = listOf(section("dhuhr", 11 * 60 + 58), section("isha", 18 * 60))
        assertEquals(listOf("isha") to listOf("dhuhr"), ids(nowAndNext(evening, isha, fajr, 23 * 60)))
    }

    @Test
    fun beforeTheFirstSectionOfThePrayerTime() {
        // 14:00, Asr's time with only the 16:00 session in it: nothing due yet, the session next.
        val only = listOf(section("dhuhr", 11 * 60 + 58), section("quran", 16 * 60))
        assertEquals(emptyList<String>() to listOf("quran"), ids(nowAndNext(only, asr, maghrib, 14 * 60)))
    }

    @Test
    fun theNightRunsPastMidnight() {
        val isha = 17 * 60 + 46
        val fajr = 5 * 60 + 24
        val night = listOf(section("isha", 18 * 60), section("qiyam", 3 * 60), section("fajr", 5 * 60 + 30))
        // 01:00: Isha's section is due; the night prayer comes before Fajr; Fajr's own is not of this time.
        assertEquals(listOf("isha") to listOf("qiyam"), ids(nowAndNext(night, isha, fajr, 60)))
    }
}
