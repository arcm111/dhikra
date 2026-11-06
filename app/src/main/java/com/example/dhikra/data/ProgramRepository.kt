package com.example.dhikra.data

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
        // Seeded from older defaults (or never seeded): the default program is replaced by the
        // current one. The previous file is kept aside, never read again by the app.
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
     * Adds the current default program (replacing an older one) and its texts to [stored], or
     * starts with it alone; the other programs and library texts are kept. Today's progress
     * starts over.
     */
    private fun seedDefaults(stored: ProgramStore?): ProgramStore {
        val defaults = DefaultProgram.bundle()
        val store = if (stored == null) {
            ProgramStore(listOf(defaults.program), defaults.program.id, defaults.library)
        } else {
            val others = stored.programs.filterNot { it.id == DefaultProgram.PROGRAM_ID }
            val program = defaults.program.withIdsOutside(others.flatMapTo(mutableSetOf()) { it.ids })
            stored.copy(library = stored.library.merge(defaults.library)).add(program)
        }
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

    fun saveTextScale(scale: Float) = prefs.edit { putFloat(KEY_TEXT_SCALE, scale) }

    /** The city the user picked for their prayer times, or null when none was picked yet. */
    fun loadPrayerCity(): String? = prefs.getString(KEY_PRAYER_CITY, null)

    fun savePrayerCity(cityId: String) = prefs.edit { putString(KEY_PRAYER_CITY, cityId) }

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
        private const val KEY_DEFAULT_SPEED = "default_reading_speed"
        private const val KEY_TEXT_SCALE = "text_scale"
        private const val KEY_PRAYER_CITY = "prayer_city"
        /** [DefaultProgram.VERSION] the stored program was seeded with. */
        private const val KEY_DEFAULTS_VERSION = "defaults_version"

        /** Comfortable recitation pace used before the app has learned the user's speed. */
        const val DEFAULT_LETTERS_PER_SECOND = 7f
        const val MIN_LETTERS_PER_SECOND = 1.5f
        const val MAX_LETTERS_PER_SECOND = 40f

        fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
}
