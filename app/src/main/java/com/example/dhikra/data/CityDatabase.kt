package com.example.dhikra.data

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.annotation.WorkerThread
import java.io.File
import java.io.IOException

/** A city of the bundled database, with what the prayer times need: its place and time zone. */
data class CityRecord(
    val id: Long,
    val name: String,
    val nameEn: String,
    val countryCode: String,
    val country: String,
    val region: String,
    val latitude: Double,
    val longitude: Double,
    val timeZone: String,
)

/** What the prayer times read of the city database (an interface, so they can be tested on the JVM). */
interface CityLookup {
    fun city(id: Long): CityRecord?

    /** The cities matching [key], a key made by [ArabicText.searchKey]. */
    fun search(key: String, limit: Int = 60): List<CityRecord>

    /** Every country's capital. */
    fun capitals(): List<CityRecord>
}

/** A country of the bundled database, with its capital's id. */
data class CountryRecord(val code: String, val name: String, val capitalId: Long?)

/** A state / province of the bundled database. */
data class RegionRecord(val id: Long, val countryCode: String, val name: String)

/**
 * The cities bundled with the app: `assets/cities.db`, built from GeoNames (cities of 15,000
 * people or more, with countries, states / provinces and time zones) by
 * `scripts/build-cities-db.py`, opened read-only. Names are Arabic where GeoNames has one.
 *
 * As for the Quran text, SQLite can't open a file inside the APK, so the database is copied once
 * into the no-backup files. Every query reads the database: call them off the main thread.
 */
class CityDatabase private constructor(context: Context) : CityLookup {

    private val appContext = context.applicationContext

    @Volatile
    private var database: SQLiteDatabase? = null

    @WorkerThread
    override fun city(id: Long): CityRecord? = query("$CITY_SELECT WHERE c.id = ?", id.toString()).firstOrNull()

    /**
     * The cities whose name — or whose state or country's name — contains [key], a key made by
     * [ArabicText.searchKey]; the most populous first.
     */
    @WorkerThread
    override fun search(key: String, limit: Int): List<CityRecord> {
        if (key.isBlank()) return emptyList()
        val like = "%" + key.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"
        return query(
            "$CITY_SELECT WHERE c.search LIKE ?1 ESCAPE '\\' OR r.search LIKE ?1 ESCAPE '\\' " +
                "OR co.search LIKE ?1 ESCAPE '\\' " +
                // Cities named so first, then those of a state or country named so.
                "ORDER BY c.search LIKE ?1 ESCAPE '\\' DESC, c.population DESC LIMIT $limit",
            like,
        )
    }

    /** Every country's capital, by the country's name (its normalised key: hamza forms sort together). */
    @WorkerThread
    override fun capitals(): List<CityRecord> = query("$CITY_SELECT WHERE c.id = co.capital ORDER BY co.search")

    /** The capital of [countryCode] (ISO 3166), if the database has one. */
    @WorkerThread
    fun capital(countryCode: String): CityRecord? =
        query("$CITY_SELECT WHERE co.code = ? AND c.id = co.capital", countryCode.uppercase()).firstOrNull()

    @WorkerThread
    fun countries(): List<CountryRecord> = read("SELECT code, name, capital FROM country ORDER BY search") { cursor ->
        CountryRecord(cursor.getString(0), cursor.getString(1), if (cursor.isNull(2)) null else cursor.getLong(2))
    }

    /** The states / provinces of [countryCode]. */
    @WorkerThread
    fun regions(countryCode: String): List<RegionRecord> =
        read("SELECT id, country, name FROM region WHERE country = ? ORDER BY name", countryCode.uppercase()) { cursor ->
            RegionRecord(cursor.getLong(0), cursor.getString(1), cursor.getString(2))
        }

    /** The cities of a state / province, the most populous first. */
    @WorkerThread
    fun cities(regionId: Long): List<CityRecord> =
        query("$CITY_SELECT WHERE c.region = ? ORDER BY c.population DESC", regionId.toString())

    private fun query(sql: String, vararg args: String): List<CityRecord> = read(sql, *args) { cursor ->
        CityRecord(
            id = cursor.getLong(0),
            name = cursor.getString(1),
            nameEn = cursor.getString(2),
            countryCode = cursor.getString(3),
            country = cursor.getString(4),
            region = cursor.getString(5) ?: "",
            latitude = cursor.getDouble(6),
            longitude = cursor.getDouble(7),
            timeZone = cursor.getString(8),
        )
    }

    private fun <T> read(sql: String, vararg args: String, row: (Cursor) -> T): List<T> = try {
        database().rawQuery(sql, args).use { cursor -> buildList { while (cursor.moveToNext()) add(row(cursor)) } }
    } catch (e: Exception) {
        Log.w(TAG, "Unable to read the cities", e)
        emptyList()
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
        private const val TAG = "CityDatabase"
        private const val ASSET = "cities.db"

        /** Version of the asset: bump it whenever the asset changes, so installed copies are replaced. */
        private const val VERSION = 1
        private const val FILE_PREFIX = "cities-v"
        private const val FILE_NAME = "$FILE_PREFIX$VERSION.db"

        private const val CITY_SELECT =
            "SELECT c.id, c.name, c.name_en, c.country, co.name, r.name, c.lat, c.lon, t.name " +
                "FROM city c JOIN country co ON co.code = c.country " +
                "LEFT JOIN region r ON r.id = c.region JOIN timezone t ON t.id = c.timezone"

        @Volatile
        private var instance: CityDatabase? = null

        fun get(context: Context): CityDatabase = instance ?: synchronized(this) {
            instance ?: CityDatabase(context).also { instance = it }
        }
    }
}
