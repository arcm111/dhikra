package io.github.arcm111.dhikra.data

import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.CalculationParameters
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.HighLatitudeRule
import com.batoulapps.adhan2.Madhab
import com.batoulapps.adhan2.PrayerAdjustments
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import java.util.Calendar
import java.util.TimeZone
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * How Fajr and Isha are reckoned: the conventions of the main authorities. Stored by name.
 * [MOROCCO] is not one of Adhan's methods: it is built from its angles (Fajr 19°, Isha 17°).
 */
enum class PrayerMethod {
    MUSLIM_WORLD_LEAGUE,
    UMM_AL_QURA,
    EGYPTIAN,
    KARACHI,
    DUBAI,
    KUWAIT,
    QATAR,
    TURKEY,
    SINGAPORE,
    NORTH_AMERICA,
    MOON_SIGHTING_COMMITTEE,
    MOROCCO;

    internal val parameters: CalculationParameters
        get() = when (this) {
            MUSLIM_WORLD_LEAGUE -> CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters
            UMM_AL_QURA -> CalculationMethod.UMM_AL_QURA.parameters
            EGYPTIAN -> CalculationMethod.EGYPTIAN.parameters
            KARACHI -> CalculationMethod.KARACHI.parameters
            DUBAI -> CalculationMethod.DUBAI.parameters
            KUWAIT -> CalculationMethod.KUWAIT.parameters
            QATAR -> CalculationMethod.QATAR.parameters
            TURKEY -> CalculationMethod.TURKEY.parameters
            SINGAPORE -> CalculationMethod.SINGAPORE.parameters
            NORTH_AMERICA -> CalculationMethod.NORTH_AMERICA.parameters
            MOON_SIGHTING_COMMITTEE -> CalculationMethod.MOON_SIGHTING_COMMITTEE.parameters
            MOROCCO -> CalculationParameters(
                fajrAngle = 19.0,
                ishaAngle = 17.0,
                method = CalculationMethod.OTHER,
                methodAdjustments = PrayerAdjustments(dhuhr = 1),
            )
        }

    companion object {
        fun fromName(name: String?): PrayerMethod? = entries.firstOrNull { it.name == name }
    }
}

/** How the Asr shadow is reckoned: the majority (Shafi'i, Maliki, Hanbali) or the Hanafi school. */
enum class AsrMadhab {
    SHAFI,
    HANAFI;

    internal val adhan: Madhab get() = if (this == HANAFI) Madhab.HANAFI else Madhab.SHAFI

    companion object {
        fun fromName(name: String?): AsrMadhab? = entries.firstOrNull { it.name == name }
    }
}

/**
 * The user's choices for the prayer times: a [method] and an Asr [madhab] (null: the city's
 * country default, see [PrayerDefaults]), and minutes added to (or taken from) each time.
 */
data class PrayerSettings(
    val method: PrayerMethod? = null,
    val madhab: AsrMadhab? = null,
    val adjustments: Map<Prayer, Int> = emptyMap(),
) {
    fun adjustment(prayer: Prayer): Int = adjustments[prayer] ?: 0

    companion object {
        /** How far a time can be moved by hand, in minutes. */
        const val MAX_ADJUSTMENT = 30
    }
}

/**
 * The defaults when the user has not chosen: the Moonsighting Committee's method (لجنة رؤية الهلال)
 * everywhere, and the Asr school most used in each country (ISO 3166 code).
 */
object PrayerDefaults {

    /** The method used until the user picks one, wherever the city. */
    val METHOD = PrayerMethod.MOON_SIGHTING_COMMITTEE

    private val hanafi = setOf(
        "PK", "IN", "BD", "AF", "TR", // South Asia and Turkey
        "UZ", "TJ", "KG", "KZ", "TM", // Central Asia
        "BA", "AL", "XK", "MK", // the Balkans
    )

    @Suppress("UNUSED_PARAMETER")
    fun method(countryCode: String): PrayerMethod = METHOD

    fun madhab(countryCode: String): AsrMadhab = if (countryCode.uppercase() in hanafi) AsrMadhab.HANAFI else AsrMadhab.SHAFI
}

/** Prayer times calculated on the device with the Adhan library. */
@OptIn(ExperimentalTime::class)
object PrayerCalculator {

    private const val MINUTES_PER_DAY = 24 * 60

    /**
     * The times of the six [Prayer]s at ([latitude], [longitude]) on the date [year]-[month]-[day]
     * of that place, in minutes after its midnight in [timeZone] (the city's own zone, daylight
     * saving included). A time that falls on the next day (Isha far north in summer) counts past
     * 1440. Null when the times cannot be calculated (the sun does not rise or set that day).
     *
     * [highLatitudeRule] bounds Fajr and Isha where twilight lasts all night; by default the one
     * Adhan recommends for the latitude.
     */
    fun minutes(
        latitude: Double,
        longitude: Double,
        timeZone: TimeZone,
        year: Int,
        month: Int,
        day: Int,
        method: PrayerMethod,
        madhab: AsrMadhab,
        adjustments: Map<Prayer, Int> = emptyMap(),
        highLatitudeRule: HighLatitudeRule? = null,
    ): Map<Prayer, Int>? {
        val asked = calculate(latitude, longitude, timeZone, year, month, day, year, month, day, method, madhab, adjustments, highLatitudeRule)
            ?: return null
        // Near the date line Adhan 0.0.6 can work out a neighbouring day (fixed in 0.0.7, which
        // needs Kotlin 2.4): when Dhuhr lands on another local date, ask for the date next to it.
        val shift = Math.floorDiv(asked.getValue(Prayer.DHUHR), MINUTES_PER_DAY)
        if (shift == 0) return asked
        val other = Calendar.getInstance(timeZone).apply {
            clear()
            set(year, month - 1, day)
            add(Calendar.DAY_OF_MONTH, -shift)
        }
        return calculate(
            latitude, longitude, timeZone, year, month, day,
            other.get(Calendar.YEAR), other.get(Calendar.MONTH) + 1, other.get(Calendar.DAY_OF_MONTH),
            method, madhab, adjustments, highLatitudeRule,
        )?.takeIf { Math.floorDiv(it.getValue(Prayer.DHUHR), MINUTES_PER_DAY) == 0 } ?: asked
    }

    /** The times Adhan gives for [askYear]-[askMonth]-[askDay], counted from [year]-[month]-[day]'s midnight. */
    private fun calculate(
        latitude: Double,
        longitude: Double,
        timeZone: TimeZone,
        year: Int,
        month: Int,
        day: Int,
        askYear: Int,
        askMonth: Int,
        askDay: Int,
        method: PrayerMethod,
        madhab: AsrMadhab,
        adjustments: Map<Prayer, Int>,
        highLatitudeRule: HighLatitudeRule?,
    ): Map<Prayer, Int>? {
        val coordinates = Coordinates(latitude, longitude)
        val parameters = method.parameters.copy(
            madhab = madhab.adhan,
            highLatitudeRule = highLatitudeRule ?: HighLatitudeRule.recommendedFor(coordinates),
            prayerAdjustments = PrayerAdjustments(
                fajr = adjustments[Prayer.FAJR] ?: 0,
                sunrise = adjustments[Prayer.SUNRISE] ?: 0,
                dhuhr = adjustments[Prayer.DHUHR] ?: 0,
                asr = adjustments[Prayer.ASR] ?: 0,
                maghrib = adjustments[Prayer.MAGHRIB] ?: 0,
                isha = adjustments[Prayer.ISHA] ?: 0,
            ),
        )
        val times = try {
            PrayerTimes(coordinates, DateComponents(askYear, askMonth, askDay), parameters)
        } catch (_: Exception) {
            return null // Adhan fails when the sun neither rises nor sets (polar day / night).
        }
        val midnight = Calendar.getInstance(timeZone).apply {
            clear()
            set(year, month - 1, day)
        }
        fun local(instant: Instant): Int {
            val at = Calendar.getInstance(timeZone).apply { timeInMillis = instant.toEpochMilliseconds() }
            val days = daysBetween(midnight, at)
            return days * MINUTES_PER_DAY + at.get(Calendar.HOUR_OF_DAY) * 60 + at.get(Calendar.MINUTE)
        }
        return mapOf(
            Prayer.FAJR to local(times.fajr),
            Prayer.SUNRISE to local(times.sunrise),
            Prayer.DHUHR to local(times.dhuhr),
            Prayer.ASR to local(times.asr),
            Prayer.MAGHRIB to local(times.maghrib),
            Prayer.ISHA to local(times.isha),
        )
    }

    /** Whole calendar days from [from]'s date to [to]'s, both in their (same) time zone. */
    private fun daysBetween(from: Calendar, to: Calendar): Int {
        val start = Calendar.getInstance(from.timeZone).apply {
            clear()
            set(from.get(Calendar.YEAR), from.get(Calendar.MONTH), from.get(Calendar.DAY_OF_MONTH))
        }
        val end = Calendar.getInstance(to.timeZone).apply {
            clear()
            set(to.get(Calendar.YEAR), to.get(Calendar.MONTH), to.get(Calendar.DAY_OF_MONTH))
        }
        // Rounded: a daylight-saving day is 23 or 25 hours long.
        return Math.round((end.timeInMillis - start.timeInMillis) / 86_400_000.0).toInt()
    }
}
