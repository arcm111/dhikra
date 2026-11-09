package com.example.dhikra.data

import android.content.Context

/** The five prayers of the day, plus sunrise (shown in the timetable, never "the next prayer"). */
enum class Prayer {
    FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA;

    val isPrayer: Boolean get() = this != SUNRISE
}

/**
 * A place the user can pick for their prayer times: no GPS, only a name from the list. The
 * [countryCode] (ISO 3166), [region] (state / province) and IANA [timeZone] are known for the
 * cities of the bundled database; the times of a city are always in its own time zone.
 */
data class PrayerCity(
    val id: String,
    val name: String,
    val country: String,
    val countryCode: String = "",
    val region: String = "",
    val timeZone: String? = null,
    /** The names in English, for the English interface (empty when unknown). */
    val nameEn: String = "",
    val countryEn: String = "",
    val regionEn: String = "",
)

/**
 * One day of prayer times for [city], each as minutes after midnight in the city's time zone (a
 * time that falls after the next midnight, as Isha far north in summer, counts past 1440).
 */
data class PrayerTimetable(val city: PrayerCity, val times: Map<Prayer, Int>) {

    fun at(prayer: Prayer): Int = times.getValue(prayer)

    /**
     * The next prayer after [nowMinutes] (sunrise is skipped), and how long until it. After
     * the night prayer it is the next day's Fajr, read from the same table.
     */
    fun next(nowMinutes: Int): UpcomingPrayer {
        val prayers = Prayer.entries.filter { it.isPrayer }
        val today = prayers.firstOrNull { at(it) > nowMinutes }
        return if (today != null) {
            UpcomingPrayer(today, at(today), at(today) - nowMinutes, tomorrow = false)
        } else {
            val fajr = at(Prayer.FAJR)
            UpcomingPrayer(Prayer.FAJR, fajr, fajr + MINUTES_PER_DAY - nowMinutes, tomorrow = true)
        }
    }

    private companion object {
        const val MINUTES_PER_DAY = 24 * 60
    }
}

data class UpcomingPrayer(val prayer: Prayer, val atMinutes: Int, val inMinutes: Int, val tomorrow: Boolean)

/** Where prayer times come from. [day] is `yyyy-MM-dd`, as [ProgramRepository.today]. */
interface PrayerTimesSource {
    val cities: List<PrayerCity>

    fun city(id: String): PrayerCity? = cities.firstOrNull { it.id == id }

    /** The timetable of [cityId] on [day], or null for an unknown city. */
    fun timetable(cityId: String, day: String): PrayerTimetable?

    /** The cities whose name or country matches [query] (ignoring harakat and letter variants). */
    fun search(query: String): List<PrayerCity> {
        val key = ArabicText.searchKey(query.trim())
        if (key.isEmpty()) return cities
        return cities.filter {
            ArabicText.searchKey(it.name).contains(key) || ArabicText.searchKey(it.country).contains(key)
        }
    }
}

/**
 * The prayer times the app shows and alerts at: calculated on the device for the bundled cities
 * ([DevicePrayerTimes]).
 */
fun appPrayerTimes(context: Context): PrayerTimesSource = DevicePrayerTimes.get(context)

/**
 * Fixed sample prayer times — one approximate timetable per city, the same every day — for the
 * previews and the tests only; the app uses [appPrayerTimes].
 */
object MockPrayerTimes : PrayerTimesSource {

    private class Row(val city: PrayerCity, vararg val hhmm: String)

    private val rows = listOf(
        Row(PrayerCity("makkah", "مكة المكرمة", "السعودية"), "05:12", "06:30", "12:10", "15:16", "17:44", "19:14"),
        Row(PrayerCity("madinah", "المدينة المنورة", "السعودية"), "05:20", "06:41", "12:14", "15:15", "17:46", "19:16"),
        Row(PrayerCity("riyadh", "الرياض", "السعودية"), "04:44", "06:03", "11:37", "14:41", "17:10", "18:40"),
        Row(PrayerCity("jeddah", "جدة", "السعودية"), "05:15", "06:33", "12:13", "15:19", "17:48", "19:18"),
        Row(PrayerCity("dubai", "دبي", "الإمارات"), "05:16", "06:35", "12:06", "15:10", "17:37", "18:55"),
        Row(PrayerCity("doha", "الدوحة", "قطر"), "04:33", "05:52", "11:21", "14:26", "16:49", "18:19"),
        Row(PrayerCity("kuwait", "الكويت", "الكويت"), "04:47", "06:10", "11:35", "14:35", "16:59", "18:17"),
        Row(PrayerCity("amman", "عمّان", "الأردن"), "04:46", "06:12", "11:31", "14:27", "16:49", "18:09"),
        Row(PrayerCity("quds", "القدس", "فلسطين"), "04:32", "05:57", "11:20", "14:18", "16:43", "18:01"),
        Row(PrayerCity("damascus", "دمشق", "سوريا"), "04:48", "06:15", "11:33", "14:28", "16:51", "18:10"),
        Row(PrayerCity("beirut", "بيروت", "لبنان"), "04:43", "06:09", "11:28", "14:25", "16:47", "18:05"),
        Row(PrayerCity("baghdad", "بغداد", "العراق"), "04:58", "06:23", "11:45", "14:42", "17:06", "18:24"),
        Row(PrayerCity("cairo", "القاهرة", "مصر"), "04:46", "06:13", "11:39", "14:39", "17:04", "18:22"),
        Row(PrayerCity("tunis", "تونس", "تونس"), "05:39", "07:05", "12:13", "14:58", "17:21", "18:45"),
        Row(PrayerCity("algiers", "الجزائر", "الجزائر"), "06:07", "07:33", "12:43", "15:28", "17:52", "19:15"),
        Row(PrayerCity("casablanca", "الدار البيضاء", "المغرب"), "06:28", "07:52", "13:11", "16:00", "18:28", "19:46"),
        Row(PrayerCity("istanbul", "إسطنبول", "تركيا"), "06:08", "07:37", "12:32", "15:07", "17:27", "18:51"),
        Row(PrayerCity("karachi", "كراتشي", "باكستان"), "05:39", "06:57", "12:24", "15:30", "17:51", "19:09"),
        Row(PrayerCity("kuala_lumpur", "كوالالمبور", "ماليزيا"), "05:54", "07:08", "13:05", "16:25", "19:01", "20:12"),
        Row(PrayerCity("jakarta", "جاكرتا", "إندونيسيا"), "04:08", "05:24", "11:39", "15:00", "17:52", "19:05"),
        Row(PrayerCity("london", "لندن", "المملكة المتحدة"), "05:36", "07:12", "11:52", "13:52", "16:17", "17:52"),
        Row(PrayerCity("paris", "باريس", "فرنسا"), "06:13", "07:58", "12:42", "14:52", "17:22", "18:58"),
        Row(PrayerCity("new_york", "نيويورك", "الولايات المتحدة"), "05:15", "06:38", "11:46", "14:24", "16:46", "18:06"),
    )

    private val tables: Map<String, PrayerTimetable> = rows.associate { row ->
        val times = Prayer.entries.zip(row.hhmm.map(::minutes)).toMap()
        row.city.id to PrayerTimetable(row.city, times)
    }

    override val cities: List<PrayerCity> = rows.map { it.city }

    override fun timetable(cityId: String, day: String): PrayerTimetable? = tables[cityId]

    private fun minutes(hhmm: String): Int {
        val (h, m) = hhmm.split(':').map(String::toInt)
        return h * 60 + m
    }
}
