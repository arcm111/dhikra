package io.github.arcm111.dhikra.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** The bundled city database (assets/cities.db) and the prayer times read from it, as the app does. */
@RunWith(AndroidJUnit4::class)
class CityDatabaseTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val cities = CityDatabase.get(context)

    @Test
    fun aCityHasItsCountryRegionPlaceAndTimeZone() {
        val makkah = cities.city(104515)
        assertNotNull(makkah)
        assertEquals("مكة المكرمة", makkah!!.name)
        assertEquals("SA", makkah.countryCode)
        assertEquals("Asia/Riyadh", makkah.timeZone)
        assertTrue(makkah.region.isNotBlank())
        assertEquals(21.4, makkah.latitude, 0.1)
        assertNull(cities.city(1))
    }

    @Test
    fun search_findsCitiesByArabicOrLatinName_namedOnesFirst() {
        assertEquals(104515L, cities.search(ArabicText.searchKey("مكة")).first().id)
        assertEquals(2643743L, cities.search(ArabicText.searchKey("London")).first().id)
        // A country's name lists its cities, the most populous first.
        assertTrue(cities.search(ArabicText.searchKey("السعودية")).all { it.countryCode == "SA" })
        // LIKE wildcards are taken literally.
        assertTrue(cities.search("%").isEmpty())
    }

    @Test
    fun countriesRegionsAndCapitals() {
        assertTrue(cities.countries().size > 200)
        assertEquals(108410L, cities.capital("SA")?.id) // الرياض
        assertEquals(360630L, cities.capital("eg")?.id) // القاهرة
        val region = cities.regions("SA").first()
        assertTrue(cities.cities(region.id).all { it.countryCode == "SA" })
        assertTrue(cities.capitals().size > 200)
    }

    @Test
    fun theAppsPrayerTimesComeFromTheDatabase() {
        val source = appPrayerTimes(context)
        val table = source.timetable("104515", "2026-11-08")
        assertNotNull(table)
        val times = Prayer.entries.map { table!!.at(it) }
        assertEquals(times.sorted(), times)
        // A city picked from the earlier mock table still resolves.
        assertEquals("104515", source.city("makkah")?.id)
    }
}
