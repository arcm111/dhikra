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
 * Persists the program and its library (as a JSON file, same format as exports), today's progress
 * and the user's learned reading speeds. All methods perform blocking I/O: call them off the main
 * thread.
 */
class ProgramRepository(context: Context) {

    private val appContext = context.applicationContext
    private val programFile = AtomicFile(File(appContext.filesDir, PROGRAM_FILE))
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): ProgramBundle {
        val seededVersion = prefs.getInt(KEY_DEFAULTS_VERSION, 0)
        val stored = if (seededVersion < DefaultProgram.VERSION) {
            // Seeded from older defaults (or never seeded): the program and library are replaced by
            // the current defaults. The previous file is kept aside, never read again by the app.
            backUpProgramFile(seededVersion)
            null
        } else runCatching {
            if (programFile.baseFile.exists()) {
                val json = programFile.readFully().decodeToString()
                ProgramJson.decode(json).also {
                    // Older files embedded the texts in the program: store the migrated library.
                    if (ProgramJson.isOutdated(json)) save(it)
                }
            } else null
        }.onFailure { Log.w(TAG, "Unable to read program, falling back to the defaults", it) }.getOrNull()
        return stored ?: seedDefaults()
    }

    /** Stores the default program and library, and starts the day's progress over. */
    private fun seedDefaults(): ProgramBundle = DefaultProgram.bundle().also { bundle ->
        save(bundle)
        prefs.edit {
            putInt(KEY_DEFAULTS_VERSION, DefaultProgram.VERSION)
            remove(KEY_PROGRESS_DAY)
            remove(KEY_PROGRESS)
        }
    }

    /** Copies the stored program aside, one file per replaced version so no backup overwrites another. */
    private fun backUpProgramFile(version: Int) {
        val file = programFile.baseFile
        if (!file.exists()) return
        runCatching { file.copyTo(File(appContext.filesDir, "program-backup-v$version.json"), overwrite = true) }
            .onFailure { Log.w(TAG, "Unable to back up the program", it) }
    }

    fun save(bundle: ProgramBundle) {
        val out = programFile.startWrite()
        try {
            out.write(ProgramJson.encode(bundle).encodeToByteArray())
            programFile.finishWrite(out)
        } catch (e: Exception) {
            programFile.failWrite(out)
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

    private fun decodeIntMap(json: String): Map<String, Int> = runCatching {
        val obj = JSONObject(json)
        obj.keys().asSequence().associateWith { obj.getInt(it) }
    }.getOrDefault(emptyMap())

    companion object {
        private const val TAG = "ProgramRepository"
        private const val PROGRAM_FILE = "program.json"
        private const val PREFS = "dhikra"
        private const val KEY_PROGRESS_DAY = "progress_day"
        private const val KEY_PROGRESS = "progress"
        private const val KEY_SPEEDS = "reading_speeds"
        private const val KEY_DEFAULT_SPEED = "default_reading_speed"
        private const val KEY_TEXT_SCALE = "text_scale"
        /** [DefaultProgram.VERSION] the stored program was seeded with. */
        private const val KEY_DEFAULTS_VERSION = "defaults_version"

        /** Comfortable recitation pace used before the app has learned the user's speed. */
        const val DEFAULT_LETTERS_PER_SECOND = 7f
        const val MIN_LETTERS_PER_SECOND = 1.5f
        const val MAX_LETTERS_PER_SECOND = 40f

        fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
}
