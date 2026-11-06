package com.example.dhikra.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.annotation.WorkerThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * The Quran text bundled with the app: `assets/quran.db`, built from the Tanzil Uthmani text by
 * `scripts/build-quran-db.sh`, opened read-only.
 *
 * SQLite can't open a file inside the APK, so the database is copied once into the no-backup
 * files (it ships with the app: there is nothing to back up).
 */
class QuranDatabase private constructor(context: Context) : QuranSource {

    private val appContext = context.applicationContext

    @Volatile
    private var database: SQLiteDatabase? = null

    @Volatile
    private var surahs: List<Surah>? = null

    /** This database for the screens: read on the IO dispatcher. */
    val lookup: QuranLookup = object : QuranLookup {
        override suspend fun surahs(): List<Surah> = withContext(Dispatchers.IO) { this@QuranDatabase.surahs() }

        override suspend fun passage(ref: QuranRef): QuranPassage? =
            withContext(Dispatchers.IO) { this@QuranDatabase.passage(ref) }
    }

    /** The 114 surahs in order (read once), or an empty list when they can't be read. */
    @WorkerThread
    fun surahs(): List<Surah> = surahs ?: try {
        database().rawQuery("SELECT sura, name, ayat FROM surah ORDER BY sura", null).use { cursor ->
            buildList { while (cursor.moveToNext()) add(Surah(cursor.getInt(0), cursor.getString(1), cursor.getInt(2))) }
        }.also { surahs = it }
    } catch (e: Exception) {
        Log.w(TAG, "Unable to read the surahs", e)
        emptyList()
    }

    @WorkerThread
    override fun passage(ref: QuranRef): QuranPassage? {
        if (!ref.isValid) return null
        return try {
            val texts = database().rawQuery(
                "SELECT text FROM ayah WHERE sura = ? AND aya BETWEEN ? AND ? ORDER BY aya",
                arrayOf(ref.sura.toString(), ref.from.toString(), ref.to.toString()),
            ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(0)) } }
            Quran.passage(ref, texts) ?: null.also { Log.w(TAG, "Incomplete passage $ref") }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to read $ref", e)
            null
        }
    }

    private fun database(): SQLiteDatabase = database ?: synchronized(this) {
        database ?: open().also { database = it }
    }

    private fun open(): SQLiteDatabase {
        val file = File(appContext.noBackupFilesDir, FILE_NAME)
        if (!file.exists()) install(file)
        return SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
    }

    /**
     * Copies the asset to [target] through a temporary file, so a copy cut short is never opened.
     * Copies of older versions (and leftovers of an interrupted copy) are removed first.
     */
    private fun install(target: File) {
        val dir = target.parentFile ?: throw IOException("No directory for $target")
        dir.listFiles { file -> file.name.startsWith(FILE_PREFIX) }?.forEach { it.delete() }
        val partial = File(dir, "$FILE_NAME.partial")
        appContext.assets.open(ASSET).use { input -> partial.outputStream().use { input.copyTo(it) } }
        if (!partial.renameTo(target)) throw IOException("Unable to install $target")
    }

    companion object {
        private const val TAG = "QuranDatabase"
        private const val ASSET = "quran.db"

        /** Version of the asset: bump it whenever the asset changes, so installed copies are replaced. */
        private const val VERSION = 1
        private const val FILE_PREFIX = "quran-v"
        private const val FILE_NAME = "$FILE_PREFIX$VERSION.db"

        @Volatile
        private var instance: QuranDatabase? = null

        fun get(context: Context): QuranDatabase = instance ?: synchronized(this) {
            instance ?: QuranDatabase(context).also { instance = it }
        }
    }
}
