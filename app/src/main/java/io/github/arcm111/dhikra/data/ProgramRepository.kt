package io.github.arcm111.dhikra.data

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.core.util.AtomicFile
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Persists the user's programs and the library they share (as a JSON file, see
 * [ProgramJson.encodeStore]), today's progress and the user's learned reading speeds. The Quran
 * passages of the library are stored as references and get their text from [quran]. All methods
 * perform blocking I/O: call them off the main thread.
 */
class ProgramRepository(
    context: Context,
    private val quran: QuranSource = QuranDatabase.get(context),
) {

    private val appContext = context.applicationContext
    private val storeFile = AtomicFile(File(appContext.filesDir, STORE_FILE))
    /** The single program file of the versions before several programs; read once, then kept as is. */
    private val legacyFile = AtomicFile(File(appContext.filesDir, LEGACY_PROGRAM_FILE))
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** The stored programs and library (or the defaults), with the text of the Quran passages. */
    fun load(): ProgramStore = withQuranText(loadStored())

    /** The stored programs and library without the Quran passages' text: enough for titles (the widgets). */
    fun loadWithoutQuranText(): ProgramStore = loadStored()

    /** [store] with the text of its library's Quran passages filled in. */
    fun withQuranText(store: ProgramStore): ProgramStore =
        store.copy(library = store.library.withQuranText(quran))

    /** [bundle] with the text of its library's Quran passages filled in. */
    fun withQuranText(bundle: ProgramBundle): ProgramBundle =
        bundle.copy(library = bundle.library.withQuranText(quran))

    private fun loadStored(): ProgramStore {
        val stored = runCatching { readStored() }
            .onFailure {
                Log.w(TAG, "Unable to read the programs, falling back to the defaults", it)
                backUp(storeFile, "programs-unreadable.json")
            }
            .getOrNull()
        val seededVersion = prefs.getInt(KEY_DEFAULTS_VERSION, 0)
        if (stored != null && seededVersion >= DefaultProgram.VERSION) return stored
        // Seeded from older defaults (or never seeded): the built-in programs are replaced by (or
        // joined by) the current ones. The previous file is kept aside, never read again by the app.
        if (stored != null) backUp(storeFile, "programs-backup-v$seededVersion.json")
        return seedDefaults(stored)
    }

    /** The stored programs; the single program of older versions becomes the first one. */
    private fun readStored(): ProgramStore? {
        if (storeFile.baseFile.exists()) {
            return ProgramJson.decodeStore(storeFile.readFully().decodeToString())
        }
        if (!legacyFile.baseFile.exists()) return null
        // Its ids are kept, so today's progress and the learned reading speeds carry over.
        val bundle = ProgramJson.decode(legacyFile.readFully().decodeToString())
        val program = bundle.program.copy(id = DefaultProgram.PROGRAM_ID)
        return ProgramStore(listOf(program), program.id, bundle.library).also(::save)
    }

    /**
     * Adds the current built-in programs (replacing older versions) and their texts to [stored],
     * keeping the user's own programs and the one being followed; or, the first time, starts with
     * the built-in programs alone, following the starter. Today's progress starts over.
     */
    private fun seedDefaults(stored: ProgramStore?): ProgramStore {
        val defaults = DefaultProgram.store()
        val store = stored?.withDefaults(defaults) ?: defaults
        save(store)
        prefs.edit {
            putInt(KEY_DEFAULTS_VERSION, DefaultProgram.VERSION)
            remove(KEY_PROGRESS_DAY)
            remove(KEY_PROGRESS)
        }
        return store
    }

    /** Copies [file] aside as [name], without overwriting an earlier copy. */
    private fun backUp(file: AtomicFile, name: String) {
        val source = file.baseFile
        val target = File(appContext.filesDir, name)
        if (!source.exists() || target.exists()) return
        runCatching { source.copyTo(target) }.onFailure { Log.w(TAG, "Unable to back up $source", it) }
    }

    fun save(store: ProgramStore) {
        val out = storeFile.startWrite()
        try {
            out.write(ProgramJson.encodeStore(store).encodeToByteArray())
            storeFile.finishWrite(out)
        } catch (e: Exception) {
            storeFile.failWrite(out)
            throw e
        }
    }

    // ---- Daily progress: itemId -> completed repetitions, reset every day. ----

    fun loadProgress(day: String): Map<String, Int> {
        if (prefs.getString(KEY_PROGRESS_DAY, null) != day) return emptyMap()
        return prefs.getString(KEY_PROGRESS, null)?.let(::decodeIntMap).orEmpty()
    }

    fun saveProgress(day: String, progress: Map<String, Int>) = prefs.edit {
        putString(KEY_PROGRESS_DAY, day)
        putString(KEY_PROGRESS, JSONObject(progress.filterValues { it > 0 }).toString())
    }

    // ---- The daily counter's adhkar and the favourites: lists of ids, kept across days. ----

    fun loadCounters(): List<String> = loadList(KEY_COUNTERS)

    fun saveCounters(entryIds: List<String>) = saveList(KEY_COUNTERS, entryIds)

    fun loadFavourites(): List<String> = loadList(KEY_FAVOURITES)

    fun saveFavourites(refs: List<String>) = saveList(KEY_FAVOURITES, refs)

    private fun loadList(key: String): List<String> =
        prefs.getString(key, null)?.let { json ->
            runCatching {
                val array = org.json.JSONArray(json)
                (0 until array.length()).map { array.getString(it) }.distinct()
            }.getOrNull()
        }.orEmpty()

    private fun saveList(key: String, values: List<String>) =
        prefs.edit { putString(key, org.json.JSONArray(values).toString()) }

    // ---- Reading speed (letters per second), learned per item plus a global default. ----

    fun loadSpeeds(): Map<String, Float> =
        prefs.getString(KEY_SPEEDS, null)?.let { json ->
            runCatching {
                val obj = JSONObject(json)
                obj.keys().asSequence().associateWith { obj.getDouble(it).toFloat() }
            }.getOrNull()
        }.orEmpty()

    fun saveSpeeds(speeds: Map<String, Float>) =
        prefs.edit { putString(KEY_SPEEDS, JSONObject(speeds.mapValues { it.value.toDouble() }).toString()) }

    fun loadDefaultSpeed(): Float = prefs.getFloat(KEY_DEFAULT_SPEED, DEFAULT_LETTERS_PER_SECOND)

    fun saveDefaultSpeed(speed: Float) = prefs.edit { putFloat(KEY_DEFAULT_SPEED, speed) }

    // ---- Display preferences ----

    fun loadTextScale(): Float = prefs.getFloat(KEY_TEXT_SCALE, 1f)

    /** The floating counter's size, a share of its default (the bubble follows a change at once). */
    fun loadOverlayScale(): Float = prefs.getFloat(KEY_OVERLAY_SCALE, 1f)

    fun saveOverlayScale(scale: Float) = prefs.edit { putFloat(KEY_OVERLAY_SCALE, scale) }

    /** How the floating counter fades while left alone (the bubble follows a change at once). */
    fun loadOverlayFade(): OverlayFade = OverlayFade(
        enabled = prefs.getBoolean(OverlayFade.KEY_ENABLED, true),
        opacity = prefs.getFloat(OverlayFade.KEY_OPACITY, OverlayFade.DEFAULT_OPACITY)
            .coerceIn(OverlayFade.MIN_OPACITY, OverlayFade.MAX_OPACITY),
        delaySeconds = prefs.getInt(OverlayFade.KEY_DELAY, OverlayFade.DEFAULT_DELAY_SECONDS)
            .coerceIn(OverlayFade.MIN_DELAY_SECONDS, OverlayFade.MAX_DELAY_SECONDS),
    )

    fun saveOverlayFade(fade: OverlayFade) = prefs.edit {
        putBoolean(OverlayFade.KEY_ENABLED, fade.enabled)
        putFloat(OverlayFade.KEY_OPACITY, fade.opacity)
        putInt(OverlayFade.KEY_DELAY, fade.delaySeconds)
    }

    fun saveTextScale(scale: Float) = prefs.edit { putFloat(KEY_TEXT_SCALE, scale) }

    /** The city the user picked for their prayer times, or null when none was picked yet. */
    fun loadPrayerCity(): String? = prefs.getString(KEY_PRAYER_CITY, null)

    fun savePrayerCity(cityId: String) = prefs.edit { putString(KEY_PRAYER_CITY, cityId) }

    /** Whether the first launch's "choose your city" question was answered (a city, or later). */
    fun loadCityAsked(): Boolean = prefs.getBoolean(KEY_CITY_ASKED, false)

    fun saveCityAsked() = prefs.edit { putBoolean(KEY_CITY_ASKED, true) }

    // ---- Reminder sounds ----

    /** The sound of the sections' reminders; [ReminderSound.Default] until one is chosen. */
    fun loadSectionSound(): ReminderSound = ReminderSound.fromKey(prefs.getString(KEY_SECTION_SOUND, null))

    fun saveSectionSound(sound: ReminderSound) = prefs.edit { putString(KEY_SECTION_SOUND, sound.key) }

    /** The alert of each of the five prayers (sunrise has none); all off until turned on. */
    fun loadPrayerAlerts(): Map<Prayer, PrayerAlert> =
        Prayer.entries.filter { it.isPrayer }.associateWith { PrayerAlert.decode(prefs.getString(prayerAlertKey(it), null)) }

    fun savePrayerAlert(prayer: Prayer, alert: PrayerAlert) =
        prefs.edit { putString(prayerAlertKey(prayer), alert.encode()) }

    private fun prayerAlertKey(prayer: Prayer) = KEY_PRAYER_ALERT_PREFIX + prayer.name

    // ---- Prayer-time calculation ----

    /** The method, Asr school and per-prayer adjustments the user chose (none: the country's defaults). */
    fun loadPrayerSettings(): PrayerSettings = PrayerSettings(
        method = PrayerMethod.fromName(prefs.getString(KEY_PRAYER_METHOD, null)),
        madhab = AsrMadhab.fromName(prefs.getString(KEY_PRAYER_MADHAB, null)),
        adjustments = Prayer.entries.associateWith { prefs.getInt(KEY_PRAYER_ADJUST_PREFIX + it.name, 0) }
            .filterValues { it != 0 },
    )

    fun savePrayerSettings(settings: PrayerSettings) = prefs.edit {
        settings.method?.let { putString(KEY_PRAYER_METHOD, it.name) } ?: remove(KEY_PRAYER_METHOD)
        settings.madhab?.let { putString(KEY_PRAYER_MADHAB, it.name) } ?: remove(KEY_PRAYER_MADHAB)
        Prayer.entries.forEach { prayer ->
            val minutes = settings.adjustment(prayer)
            if (minutes == 0) remove(KEY_PRAYER_ADJUST_PREFIX + prayer.name)
            else putInt(KEY_PRAYER_ADJUST_PREFIX + prayer.name, minutes)
        }
    }

    private fun decodeIntMap(json: String): Map<String, Int> = runCatching {
        val obj = JSONObject(json)
        obj.keys().asSequence().associateWith { obj.getInt(it) }
    }.getOrDefault(emptyMap())

    companion object {
        private const val TAG = "ProgramRepository"
        private const val STORE_FILE = "programs.json"
        private const val LEGACY_PROGRAM_FILE = "program.json"
        private const val PREFS = "dhikra"
        private const val KEY_PROGRESS_DAY = "progress_day"
        private const val KEY_PROGRESS = "progress"
        private const val KEY_SPEEDS = "reading_speeds"
        private const val KEY_COUNTERS = "daily_counters"
        private const val KEY_FAVOURITES = "favourites"
        private const val KEY_DEFAULT_SPEED = "default_reading_speed"
        private const val KEY_TEXT_SCALE = "text_scale"
        private const val KEY_OVERLAY_SCALE = "overlay_scale"
        private const val KEY_PRAYER_CITY = "prayer_city"
        private const val KEY_CITY_ASKED = "city_asked"
        private const val KEY_SECTION_SOUND = "section_reminder_sound"
        private const val KEY_PRAYER_ALERT_PREFIX = "prayer_alert_"
        private const val KEY_PRAYER_METHOD = "prayer_method"
        private const val KEY_PRAYER_MADHAB = "prayer_madhab"
        private const val KEY_PRAYER_ADJUST_PREFIX = "prayer_adjust_"
        /** [DefaultProgram.VERSION] the stored program was seeded with. */
        private const val KEY_DEFAULTS_VERSION = "defaults_version"

        /** Comfortable recitation pace used before the app has learned the user's speed. */
        const val DEFAULT_LETTERS_PER_SECOND = 7f
        const val MIN_LETTERS_PER_SECOND = 1.5f
        const val MAX_LETTERS_PER_SECOND = 40f

        fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
}
