package io.github.arcm111.dhikra.reminder

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import io.github.arcm111.dhikra.MainActivity
import io.github.arcm111.dhikra.R
import io.github.arcm111.dhikra.data.ArabicText
import io.github.arcm111.dhikra.data.ItemType
import io.github.arcm111.dhikra.data.LibraryEntry
import io.github.arcm111.dhikra.ui.localized
import java.util.Calendar
import kotlin.random.Random

/**
 * Random reminders: library texts the user picked (the bell in the library), one of them shown a
 * few times a day at random times within the day's hours, the whole text in the notification.
 */
object InspirationReminders {
    const val ACTION = "io.github.arcm111.dhikra.action.INSPIRE"
    private const val CHANNEL_ID = "inspiration"
    private const val PREFS = "dhikra_inspiration"
    private const val KEY_IDS = "ids"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_PER_DAY = "per_day"
    private const val KEY_FROM = "from"
    private const val KEY_TO = "to"
    private const val KEY_PLANNED = "planned"
    private const val REQUEST_BASE = 0x1A5E0
    private const val NOTIFICATION_ID = 0x1A5E0
    const val MAX_PER_DAY = 8

    /** How the reminders come: on or off, how many a day, between which hours (minutes of the day). */
    data class Settings(
        val enabled: Boolean = true,
        val perDay: Int = 3,
        val fromMinutes: Int = 8 * 60,
        val toMinutes: Int = 22 * 60,
    )

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** The library texts picked for the reminders. */
    fun ids(context: Context): Set<String> = prefs(context).getStringSet(KEY_IDS, emptySet()).orEmpty().toSet()

    /** Picks [entryId] for the reminders, or drops it; plans the reminders anew. */
    fun toggle(context: Context, entryId: String): Set<String> {
        val ids = ids(context).let { if (entryId in it) it - entryId else it + entryId }
        prefs(context).edit { putStringSet(KEY_IDS, ids) }
        schedule(context, replan = true)
        return ids
    }

    fun settings(context: Context): Settings = prefs(context).let {
        Settings(
            enabled = it.getBoolean(KEY_ENABLED, true),
            perDay = it.getInt(KEY_PER_DAY, 3).coerceIn(1, MAX_PER_DAY),
            fromMinutes = it.getInt(KEY_FROM, 8 * 60),
            toMinutes = it.getInt(KEY_TO, 22 * 60),
        )
    }

    fun saveSettings(context: Context, settings: Settings) {
        prefs(context).edit {
            putBoolean(KEY_ENABLED, settings.enabled)
            putInt(KEY_PER_DAY, settings.perDay)
            putInt(KEY_FROM, settings.fromMinutes)
            putInt(KEY_TO, settings.toMinutes)
        }
        schedule(context, replan = true)
    }

    /**
     * The times (millis) of the coming reminders: [Settings.perDay] random times in today's hours
     * still ahead (from a few minutes from now), else in tomorrow's; spread so no two are too close.
     */
    fun plan(now: Calendar, settings: Settings, random: Random = Random.Default): List<Long> {
        val from = settings.fromMinutes.coerceIn(0, 24 * 60 - 1)
        val to = settings.toMinutes.coerceIn(from + 1, 24 * 60)
        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val today = maxOf(from, nowMinutes + 5) < to
        val start = if (today) maxOf(from, nowMinutes + 5) else from
        val day = (now.clone() as Calendar).apply {
            if (!today) add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        // Each reminder at a random minute of its own slice of the hours: spread over the day.
        val count = settings.perDay.coerceIn(1, MAX_PER_DAY)
        val slice = (to - start).toFloat() / count
        return (0 until count).map { i ->
            val minute = start + (slice * i + random.nextFloat() * slice).toInt()
            day.timeInMillis + minute * 60_000L
        }
    }

    /**
     * Arms the coming reminders: the ones planned and still ahead ([replan] false, after a reboot or
     * on opening the app), else a new plan — after the day's last one, the next day's.
     */
    fun schedule(context: Context, replan: Boolean = false) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        (0 until MAX_PER_DAY).forEach { alarms.cancel(alarmIntent(context, it)) }
        val settings = settings(context)
        if (!settings.enabled || ids(context).isEmpty()) {
            prefs(context).edit { remove(KEY_PLANNED) }
            return
        }
        val now = System.currentTimeMillis()
        val planned = prefs(context).getString(KEY_PLANNED, null)?.split(',')?.mapNotNull { it.toLongOrNull() }
            ?.filter { it > now }.orEmpty()
        val times = if (replan || planned.isEmpty()) plan(Calendar.getInstance(), settings) else planned
        prefs(context).edit { putString(KEY_PLANNED, times.joinToString(",")) }
        times.forEachIndexed { index, at ->
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, alarmIntent(context, index))
        }
    }

    private fun alarmIntent(context: Context, index: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_BASE + index,
            Intent(context, ReminderReceiver::class.java).setAction(ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    /** One of the picked texts, at random; null when none of them is in [entries] any more. */
    fun pick(context: Context, entries: List<LibraryEntry>, random: Random = Random.Default): LibraryEntry? {
        val ids = ids(context)
        return entries.filter { it.id in ids && it.text.isNotBlank() }.randomOrNull(random)
    }

    // ---------------------------------------------------------------- The notification

    @SuppressLint("MissingPermission")
    fun show(context: Context, entry: LibraryEntry) {
        @Suppress("NAME_SHADOWING") val context = context.localized()
        if (!ReminderScheduler.hasNotificationPermission(context)) return
        NotificationManagerCompat.from(context).createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.inspiration_channel))
                .setDescription(context.getString(R.string.inspiration_channel_desc))
                .build()
        )
        val open = PendingIntent.getActivity(
            context,
            REQUEST_BASE,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        // The whole text, expanded — the Quran with its ayah markers, as in the reader.
        val text = if (entry.type == ItemType.QURAN) ArabicText.withAyahMarkers(entry.text) else entry.text
        val excerpt = text.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
        val title = entry.title.ifBlank { context.getString(R.string.inspiration_title) }
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(accent(entry.type))
            .setContentTitle(title)
            .setContentText(excerpt)
            .setStyle(NotificationCompat.BigTextStyle().setBigContentTitle(title).bigText(text))
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setContentIntent(open)
            .setAutoCancel(true)
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
    }

    /** The collection's colour, for the notification's icon and app name. */
    private fun accent(type: ItemType): Int = when (type) {
        ItemType.QURAN -> 0xFF2E7D5B.toInt()
        ItemType.HADITH -> 0xFF5B6B82.toInt()
        ItemType.DUA -> 0xFF3B6EA8.toInt()
        else -> 0xFF7A5BA6.toInt()
    }
}
