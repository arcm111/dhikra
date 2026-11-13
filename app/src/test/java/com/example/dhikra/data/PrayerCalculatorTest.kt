package com.example.dhikra.data

import com.batoulapps.adhan2.HighLatitudeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone
import kotlin.math.abs

/**
 * The calculation against published timetables, from Adhan's reference data (Shared/Times in
 * batoulapps/adhan-java): each row is a date and its six times, in the city's local time.
 */
class PrayerCalculatorTest {

    private class City(
        val latitude: Double,
        val longitude: Double,
        val zone: String,
        val method: PrayerMethod,
        val madhab: AsrMadhab,
        /** The source's own tolerance, in minutes. */
        val variance: Int,
        val highLatitudeRule: HighLatitudeRule? = null,
    )

    /** "2016-03-01 5:10 6:45 12:18 15:48 17:44 19:03": the date, then Fajr to Isha (24 h). */
    private fun check(city: City, vararg rows: String) {
        rows.forEach { row ->
            val parts = row.split(" ")
            val (year, month, day) = parts[0].split("-").map(String::toInt)
            val expected = parts.drop(1).map { hhmm -> hhmm.split(":").let { it[0].toInt() * 60 + it[1].toInt() } }
            val actual = PrayerCalculator.minutes(
                city.latitude, city.longitude, TimeZone.getTimeZone(city.zone), year, month, day,
                city.method, city.madhab, highLatitudeRule = city.highLatitudeRule,
            )
            assertNotNull(row, actual)
            Prayer.entries.forEachIndexed { index, prayer ->
                val got = actual!!.getValue(prayer)
                assertTrue(
                    "${parts[0]} $prayer: expected ${expected[index]}, got $got",
                    abs(got - expected[index]) <= city.variance,
                )
            }
        }
    }

    /**
     * London (moonsighting.com, 2016): the clocks go forward on 27 March and back on 30 October,
     * and from May to July the sun never sinks 18° below the horizon at 51.5°N, so Fajr and Isha
     * come from the high-latitude rule.
     */
    @Test
    fun london_acrossDaylightSaving_andHighLatitudeSummer() = check(
        City(51.507194, -0.116711, "Europe/London", PrayerMethod.MOON_SIGHTING_COMMITTEE, AsrMadhab.HANAFI,
            variance = 1, highLatitudeRule = HighLatitudeRule.MIDDLE_OF_THE_NIGHT),
        "2016-01-01 6:25 8:06 12:09 14:15 16:05 17:38",
        "2016-03-01 5:10 6:45 12:18 15:49 17:44 19:03", // GMT
        "2016-04-01 4:59 6:35 13:09 17:32 19:37 20:49", // BST: an hour later on the clock
        "2016-06-01 2:55 4:49 13:03 18:31 21:12 22:23",
        "2016-07-01 2:51 4:48 13:09 18:40 21:24 22:38",
        "2016-10-01 5:28 7:02 12:55 16:42 18:40 19:57", // BST
        "2016-11-01 5:18 6:55 11:49 14:43 16:36 18:00", // GMT again
    )

    @Test
    fun makkah_ummAlQura() = check(
        City(21.427009, 39.828685, "Asia/Riyadh", PrayerMethod.UMM_AL_QURA, AsrMadhab.SHAFI, variance = 1),
        "2016-01-05 5:38 7:00 12:26 15:31 17:52 19:22",
        "2016-07-08 4:18 5:45 12:26 15:43 19:07 20:37",
    )

    @Test
    fun ankara_turkey() = check(
        City(39.939382, 32.819713, "Europe/Istanbul", PrayerMethod.TURKEY, AsrMadhab.SHAFI, variance = 2),
        "2019-01-01 6:33 8:03 12:57 15:20 17:40 19:05",
        "2019-07-01 3:21 5:16 12:57 16:55 20:28 22:15",
    )

    @Test
    fun dubai() = check(
        City(25.263056, 55.297222, "Asia/Dubai", PrayerMethod.DUBAI, AsrMadhab.SHAFI, variance = 1),
        "2016-01-01 5:42 7:01 12:25 15:24 17:44 19:03",
        "2016-07-01 4:02 5:29 12:25 15:48 19:16 20:43",
    )

    @Test
    fun theDaylightSavingShiftIsTheCitysOwn_notTheDevicesZone() {
        val london = TimeZone.getTimeZone("Europe/London")
        fun dhuhr(day: Int) = PrayerCalculator.minutes(
            51.5072, -0.1276, london, 2026, 3, day, PrayerMethod.MUSLIM_WORLD_LEAGUE, AsrMadhab.SHAFI,
        )!!.getValue(Prayer.DHUHR)
        // The clocks go forward on Sunday 29 March 2026: Dhuhr moves an hour later on the clock.
        val shift = dhuhr(29) - dhuhr(28)
        assertTrue("shift $shift", shift in 59..61)
    }

    @Test
    fun manualAdjustmentsMoveEachTime() {
        val zone = TimeZone.getTimeZone("Asia/Riyadh")
        fun times(adjustments: Map<Prayer, Int>) = PrayerCalculator.minutes(
            21.4225, 39.8262, zone, 2026, 11, 8, PrayerMethod.UMM_AL_QURA, AsrMadhab.SHAFI, adjustments,
        )!!
        val plain = times(emptyMap())
        val adjusted = times(mapOf(Prayer.FAJR to 3, Prayer.ISHA to -2))
        assertEquals(plain.getValue(Prayer.FAJR) + 3, adjusted.getValue(Prayer.FAJR))
        assertEquals(plain.getValue(Prayer.ISHA) - 2, adjusted.getValue(Prayer.ISHA))
        assertEquals(plain.getValue(Prayer.DHUHR), adjusted.getValue(Prayer.DHUHR))
    }

    /** Oslo at midsummer (59.9°N): the night never gets dark, yet every time comes out in order. */
    @Test
    fun highLatitude_osloMidsummer_isComplete_andInOrder() {
        val times = PrayerCalculator.minutes(
            59.9127, 10.7461, TimeZone.getTimeZone("Europe/Oslo"), 2026, 6, 21,
            PrayerMethod.MUSLIM_WORLD_LEAGUE, AsrMadhab.SHAFI,
        )
        assertNotNull(times)
        val ordered = Prayer.entries.map { times!!.getValue(it) }
        assertEquals(ordered.sorted(), ordered)
        // Sunrise about 03:54 and sunset about 22:44 local (CEST) on the longest day.
        assertTrue(abs(times!!.getValue(Prayer.SUNRISE) - (3 * 60 + 54)) <= 3)
        assertTrue(abs(times.getValue(Prayer.MAGHRIB) - (22 * 60 + 44)) <= 3)
    }

    /** Suva, Fiji (178.4°E), near the date line: the times belong to the asked day, in order. */
    @Test
    fun nearTheDateLine_suva() {
        val times = PrayerCalculator.minutes(
            -18.1416, 178.4419, TimeZone.getTimeZone("Pacific/Fiji"), 2026, 11, 8,
            PrayerMethod.MUSLIM_WORLD_LEAGUE, AsrMadhab.SHAFI,
        )!!
        val ordered = Prayer.entries.map { times.getValue(it) }
        assertEquals(ordered.sorted(), ordered)
        assertTrue("Dhuhr ${times.getValue(Prayer.DHUHR)}", times.getValue(Prayer.DHUHR) in 11 * 60..13 * 60)
    }

    @Test
    fun countryDefaults() {
        // The Moonsighting Committee's method is the default wherever the city.
        assertEquals(PrayerMethod.MOON_SIGHTING_COMMITTEE, PrayerDefaults.method("GB"))
        assertEquals(PrayerMethod.MOON_SIGHTING_COMMITTEE, PrayerDefaults.method("SA"))
        assertEquals(AsrMadhab.HANAFI, PrayerDefaults.madhab("PK"))
        assertEquals(AsrMadhab.SHAFI, PrayerDefaults.madhab("SA"))
    }
}
