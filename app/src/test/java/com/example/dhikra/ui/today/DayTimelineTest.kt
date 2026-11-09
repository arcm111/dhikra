package com.example.dhikra.ui.today

import com.example.dhikra.data.MockPrayerTimes
import com.example.dhikra.data.Prayer
import com.example.dhikra.data.ProgramSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DayTimelineTest {

    // Makkah: Fajr 05:12, sunrise 06:30, Dhuhr 12:10, Asr 15:16, Maghrib 17:44, Isha 19:14.
    private val makkah = MockPrayerTimes.timetable("makkah", "2026-11-06")!!

    private fun section(id: String, minutes: Int?) = ProgramSection(id = id, title = id, reminderMinutes = minutes)

    private val night = section("night", 4 * 60) // after midnight: still the night of Isha
    private val fajr = section("fajr", 5 * 60 + 12) // the very minute of Fajr
    private val sunrise = section("sunrise", 6 * 60 + 45)
    private val duha = section("duha", 9 * 60)
    private val asr = section("asr", 16 * 60 + 30)
    private val sleep = section("sleep", 22 * 60 + 30)
    private val allDay = section("allDay", null)
    private val sections = listOf(allDay, duha, sleep, asr, fajr, night, sunrise)

    /** Each period's entries as short labels: sections by id, the current time as "now". */
    private fun DayTimeline.layout(): Map<Prayer?, List<String>> = periods.associate { period ->
        period.prayer to period.entries.map {
            when (it) {
                is PeriodEntry.Section -> it.section.id
                is PeriodEntry.Now -> "now"
            }
        }
    }

    @Test
    fun periodsRunFromOnePrayerToTheNext() {
        val periods = buildDayTimeline(sections, makkah, nowMinutes = 10 * 60).periods
        assertEquals(Prayer.entries, periods.map { it.prayer })
        assertEquals(5 * 60 + 12, periods.first().start)
        assertEquals(6 * 60 + 30, periods.first().end) // Fajr until sunrise
        val isha = periods.last()
        assertEquals(19 * 60 + 14, isha.start)
        assertEquals(5 * 60 + 12, isha.end) // the night runs to the next Fajr
    }

    @Test
    fun sectionsGoInThePeriodTheirTimeFallsIn() {
        val layout = buildDayTimeline(sections, makkah, nowMinutes = 10 * 60).layout()
        assertEquals(listOf("fajr"), layout[Prayer.FAJR])
        assertEquals(listOf("sunrise", "duha", "now"), layout[Prayer.SUNRISE])
        assertEquals(emptyList<String>(), layout[Prayer.DHUHR])
        assertEquals(listOf("asr"), layout[Prayer.ASR])
        // After midnight is still the night: 22:30 before 04:00.
        assertEquals(listOf("sleep", "night"), layout[Prayer.ISHA])
    }

    @Test
    fun untimedSectionsAreSetApart() {
        val timeline = buildDayTimeline(sections, makkah, nowMinutes = 10 * 60)
        assertEquals(listOf(allDay), timeline.untimed)
        assertEquals(false, timeline.layout().values.flatten().contains("allDay"))
    }

    @Test
    fun theCurrentPeriodHoldsNow_andTheComingPrayerCarriesItsCountdown() {
        val timeline = buildDayTimeline(sections, makkah, nowMinutes = 13 * 60)
        assertEquals(listOf(Prayer.DHUHR), timeline.periods.filter { it.current }.map { it.prayer })
        assertEquals(2, timeline.currentIndex)
        val asr = timeline.periods.first { it.prayer == Prayer.ASR }
        assertEquals(15 * 60 + 16 - 13 * 60, asr.untilPrayer)
        assertNull(timeline.periods.first { it.prayer == Prayer.DHUHR }.untilPrayer)
    }

    @Test
    fun afterMidnight_theNightIsCurrent_andFajrIsComing() {
        val timeline = buildDayTimeline(sections, makkah, nowMinutes = 2 * 60 + 30)
        assertEquals(Prayer.ISHA, timeline.periods[timeline.currentIndex].prayer)
        assertEquals(listOf("sleep", "now", "night"), timeline.layout()[Prayer.ISHA])
        assertEquals(2 * 60 + 42, timeline.periods.first { it.prayer == Prayer.FAJR }.untilPrayer)
    }

    @Test
    fun theCurrentTimeComesAfterWhatStartsThatMinute() {
        val layout = buildDayTimeline(sections, makkah, nowMinutes = 5 * 60 + 12).layout()
        assertEquals(listOf("fajr", "now"), layout[Prayer.FAJR])
    }

    @Test
    fun periodsOverEarlierToday_arePassed_theNightIsNot() {
        val periods = buildDayTimeline(sections, makkah, nowMinutes = 16 * 60).periods.associateBy { it.prayer }
        assertEquals(true, periods.getValue(Prayer.FAJR).passed)
        assertEquals(true, periods.getValue(Prayer.DHUHR).passed)
        assertEquals(false, periods.getValue(Prayer.ASR).passed) // the current one
        assertEquals(false, periods.getValue(Prayer.MAGHRIB).passed)
        assertEquals(false, periods.getValue(Prayer.ISHA).passed)
        // Its sections are passed with it.
        val sunriseEntries = periods.getValue(Prayer.SUNRISE).entries.filterIsInstance<PeriodEntry.Section>()
        assertEquals(listOf(true, true), sunriseEntries.map { it.passed })
    }

    @Test
    fun afterMidnight_nothingOfTheComingDayIsPassed() {
        val periods = buildDayTimeline(sections, makkah, nowMinutes = 2 * 60).periods
        assertEquals(emptyList<Prayer?>(), periods.filter { it.passed }.map { it.prayer })
    }

    /** The current section is read on the city's clock, like the periods: the last one started. */
    @Test
    fun nowSection_isTheLastStartedInTheCurrentPeriod() {
        // 23:00 in the city: «sleep» (22:30) has started, «night» (04:00) not yet.
        val late = buildDayTimeline(sections, makkah, nowMinutes = 23 * 60)
        assertEquals("sleep", late.nowSectionId)
        // 05:00: both night sections have started; the earlier one is passed.
        val early = buildDayTimeline(sections, makkah, nowMinutes = 5 * 60)
        assertEquals("night", early.nowSectionId)
        val isha = early.periods.last().entries.filterIsInstance<PeriodEntry.Section>()
        assertEquals(listOf("sleep" to true, "night" to false), isha.map { it.section.id to it.passed })
        // 13:00: the Dhuhr period has no section yet.
        assertEquals(null, buildDayTimeline(sections, makkah, nowMinutes = 13 * 60).nowSectionId)
    }

    @Test
    fun withoutCity_onePeriodHoldsTheDay() {
        val timeline = buildDayTimeline(sections, timetable = null, nowMinutes = 5 * 60)
        assertEquals(listOf(null), timeline.periods.map { it.prayer })
        assertEquals(listOf("night", "now", "fajr", "sunrise", "duha", "asr", "sleep"), timeline.layout()[null])
        assertEquals(0, timeline.currentIndex)
    }
}
