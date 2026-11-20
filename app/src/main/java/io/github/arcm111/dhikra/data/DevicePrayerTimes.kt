package io.github.arcm111.dhikra.data

import android.content.Context
import androidx.annotation.WorkerThread
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap

/**
 * The real prayer times: calculated on the device with Adhan ([PrayerCalculator]) for the cities
 * of the bundled GeoNames database ([CityDatabase]), each in its own time zone, with the method
 * and Asr school of the city's country unless the user chose others ([settings]). No network.
 *
 * A city's id is its GeoNames id; the ids of the earlier mock table ("makkah", "london"…) still
 * resolve, so a city picked before keeps working. Lookups read the database: call them off the
 * main thread (the screens' first lookup happens while the app loads).
 */
class DevicePrayerTimes(
    private val database: CityLookup,
    private val settings: () -> PrayerSettings,
) : PrayerTimesSource {

    private val records = ConcurrentHashMap<Long, CityRecord>()

    /** Each country's capital: what the city picker lists before anything is typed. */
    override val cities: List<PrayerCity> by lazy { database.capitals().map(::toCity) }

    @WorkerThread
    override fun city(id: String): PrayerCity? = record(id)?.let(::toCity)

    @WorkerThread
    override fun timetable(cityId: String, day: String): PrayerTimetable? {
        val record = record(cityId) ?: return null
        val date = day.split("-").mapNotNull(String::toIntOrNull).takeIf { it.size == 3 } ?: return null
        val chosen = settings()
        val times = PrayerCalculator.minutes(
            latitude = record.latitude,
            longitude = record.longitude,
            timeZone = TimeZone.getTimeZone(record.timeZone),
            year = date[0],
            month = date[1],
            day = date[2],
            method = chosen.method ?: PrayerDefaults.method(record.countryCode),
            madhab = chosen.madhab ?: PrayerDefaults.madhab(record.countryCode),
            adjustments = chosen.adjustments,
        ) ?: return null
        return PrayerTimetable(toCity(record), times)
    }

    @WorkerThread
    override fun search(query: String): List<PrayerCity> {
        val key = ArabicText.searchKey(query.trim())
        if (key.isEmpty()) return cities
        return database.search(key).onEach { records[it.id] = it }.map(::toCity)
    }

    private fun record(id: String): CityRecord? {
        val geonameId = LEGACY_IDS[id] ?: id.toLongOrNull() ?: return null
        return records[geonameId] ?: database.city(geonameId)?.also { records[geonameId] = it }
    }

    private fun toCity(record: CityRecord) = PrayerCity(
        id = record.id.toString(),
        name = record.name,
        country = record.country,
        countryCode = record.countryCode,
        region = record.region,
        timeZone = record.timeZone,
        nameEn = record.nameEn,
        countryEn = record.countryEn,
        regionEn = record.regionEn,
    )

    companion object {
        /** The cities of the mock table the app used before, by their GeoNames ids. */
        private val LEGACY_IDS = mapOf(
            "makkah" to 104515L, "madinah" to 109223L, "riyadh" to 108410L, "jeddah" to 105343L,
            "dubai" to 292223L, "doha" to 290030L, "kuwait" to 285787L, "amman" to 250441L,
            "quds" to 281184L, "damascus" to 170654L, "beirut" to 276781L, "baghdad" to 98182L,
            "cairo" to 360630L, "tunis" to 2464470L, "algiers" to 2507480L, "casablanca" to 2553604L,
            "istanbul" to 745044L, "karachi" to 1174872L, "kuala_lumpur" to 1735161L, "jakarta" to 1642911L,
            "london" to 2643743L, "paris" to 2988507L, "new_york" to 5128581L,
        )

        @Volatile
        private var instance: DevicePrayerTimes? = null

        fun get(context: Context): DevicePrayerTimes = instance ?: synchronized(this) {
            instance ?: run {
                val appContext = context.applicationContext
                DevicePrayerTimes(CityDatabase.get(appContext)) { ProgramRepository(appContext).loadPrayerSettings() }
                    .also { instance = it }
            }
        }
    }
}
