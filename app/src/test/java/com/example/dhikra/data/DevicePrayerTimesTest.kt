package com.example.dhikra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class DevicePrayerTimesTest {

    private val makkah = CityRecord(
        104515, "مكة المكرمة", "Makkah", "SA", "السعودية", "مكة المكرمة", 21.42664, 39.82563, "Asia/Riyadh",
        countryEn = "Saudi Arabia", regionEn = "Makkah Region",
    )
    private val karachi = CityRecord(1174872, "كراتشي", "Karachi", "PK", "باكستان", "السند", 24.8608, 67.0104, "Asia/Karachi")
    private val london = CityRecord(2643743, "لندن", "London", "GB", "المملكة المتحدة", "إنجلترا", 51.50853, -0.12574, "Europe/London")

    private val lookup = object : CityLookup {
        val all = listOf(makkah, karachi, london)
        var searched: String? = null
        override fun city(id: Long) = all.firstOrNull { it.id == id }
        // Like the database, which keeps the names' normalised keys.
        override fun search(key: String, limit: Int) =
            all.filter { ArabicText.searchKey(it.name).contains(key) }.also { searched = key }
        override fun capitals() = listOf(london)
    }

    private var settings = PrayerSettings()
    private val source = DevicePrayerTimes(lookup) { settings }

    @Test
    fun aCityIsItsGeoNamesRecord_withItsTimeZone() {
        val city = source.city("104515")
        assertEquals(
            PrayerCity(
                "104515", "مكة المكرمة", "السعودية", "SA", "مكة المكرمة", "Asia/Riyadh",
                // The English names, for the English interface.
                nameEn = "Makkah", countryEn = "Saudi Arabia", regionEn = "Makkah Region",
            ),
            city,
        )
        assertNull(source.city("999"))
        assertNull(source.city("atlantis"))
    }

    @Test
    fun citiesPickedFromTheOldMockTableStillResolve() {
        assertEquals("104515", source.city("makkah")?.id)
        assertEquals("2643743", source.timetable("london", "2026-11-08")?.city?.id)
    }

    @Test
    fun timetableUsesTheDefaultMethod_andTheCountrysSchool() {
        val day = "2026-11-08"
        val table = source.timetable("104515", day)!!
        val expected = PrayerCalculator.minutes(
            makkah.latitude, makkah.longitude, TimeZone.getTimeZone("Asia/Riyadh"), 2026, 11, 8,
            // The Moonsighting Committee's method by default, even in Makkah.
            PrayerMethod.MOON_SIGHTING_COMMITTEE, AsrMadhab.SHAFI,
        )
        assertEquals(expected, table.times)
        // Karachi reckons Asr by the Hanafi school: later than the majority's.
        val hanafiAsr = source.timetable("1174872", day)!!.at(Prayer.ASR)
        settings = PrayerSettings(madhab = AsrMadhab.SHAFI)
        assertTrue(source.timetable("1174872", day)!!.at(Prayer.ASR) < hanafiAsr)
    }

    @Test
    fun theUsersChoicesOverrideTheDefaults() {
        val day = "2026-11-08"
        val fajr = source.timetable("104515", day)!!.at(Prayer.FAJR)
        settings = PrayerSettings(method = PrayerMethod.EGYPTIAN)
        assertNotEquals(fajr, source.timetable("104515", day)!!.at(Prayer.FAJR))
        settings = PrayerSettings(adjustments = mapOf(Prayer.FAJR to 2))
        assertEquals(fajr + 2, source.timetable("104515", day)!!.at(Prayer.FAJR))
    }

    @Test
    fun aBadDayHasNoTimetable() {
        assertNull(source.timetable("104515", "08/11/2026"))
        assertNotNull(source.timetable("104515", "2026-11-08"))
    }

    @Test
    fun searchUsesTheNormalisedKey_andBlankListsTheCapitals() {
        assertEquals(listOf("2643743"), source.search("  ").map { it.id })
        assertEquals(listOf("104515"), source.search("مكة").map { it.id })
        assertEquals("مكه", lookup.searched) // ta marbuta unified, as in the database's keys
    }
}
