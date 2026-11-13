package com.example.dhikra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class SectionClockTest {

    private val london = TimeZone.getTimeZone("Europe/London")
    private val winter = 1_762_862_400_000L // 2025-11-11 12:00 UTC: London on GMT, Madinah 3 hours ahead
    private val summer = 1_752_235_200_000L // 2025-07-11 12:00 UTC: London on BST, Madinah 2 hours ahead

    @Test
    fun madinahToLocal_followsDaylightSaving() {
        assertEquals(2 * 60, SectionClock.madinahToLocal(5 * 60, london, winter))
        assertEquals(3 * 60, SectionClock.madinahToLocal(5 * 60, london, summer))
    }

    @Test
    fun madinahToLocal_wrapsPastMidnight_andBack() {
        assertEquals(22 * 60 + 30, SectionClock.madinahToLocal(1 * 60 + 30, london, winter))
        assertEquals(1 * 60 + 30, SectionClock.localToMadinah(22 * 60 + 30, london, winter))
    }

    @Test
    fun aMadinahSection_isReadOnTheLocalClockInTheSchedule_andKeepsItsOwnTime() {
        val section = ProgramSection(id = "s", title = "s", reminderMinutes = 5 * 60, zone = SectionZone.MADINAH)
        val schedule = Program(name = "p", id = "p", sections = listOf(section)).withPrayerTimes(null, london, winter)
        assertEquals(2 * 60, schedule.sections[0].reminderMinutes)
        assertEquals(5 * 60, section.reminderMinutes)
    }

    @Test
    fun aSectionTiedToAPrayer_ignoresTheZone() {
        val tied = ProgramSection(id = "s", title = "s", reminderMinutes = 5 * 60, zone = SectionZone.MADINAH, anchor = SectionAnchor(Prayer.FAJR))
        assertFalse(tied.onMadinahClock)
        assertTrue(tied.copy(anchor = null).onMadinahClock)
    }

    @Test
    fun theZone_roundTripsThroughJson_andLocalIsTheDefault() {
        val library = Library()
        val program = Program(
            name = "p", id = "p",
            sections = listOf(
                ProgramSection(id = "m", title = "m", reminderMinutes = 5 * 60, zone = SectionZone.MADINAH),
                ProgramSection(id = "l", title = "l", reminderMinutes = 6 * 60),
            ),
        )
        val decoded = ProgramJson.decode(ProgramJson.encode(ProgramBundle(program, library))).program
        assertEquals(SectionZone.MADINAH, decoded.sections[0].zone)
        assertEquals(5 * 60, decoded.sections[0].reminderMinutes)
        assertEquals(SectionZone.LOCAL, decoded.sections[1].zone)
        assertFalse(ProgramJson.encode(ProgramBundle(program, library)).contains("\"zone\":\"local\""))
    }
}
