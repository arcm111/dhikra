package com.example.dhikra.reminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import com.example.dhikra.MainActivity
import com.example.dhikra.R
import com.example.dhikra.data.appPrayerTimes
import com.example.dhikra.data.ArabicText
import com.example.dhikra.data.Prayer
import com.example.dhikra.data.PrayerTimetable
import com.example.dhikra.data.Program
import com.example.dhikra.data.ProgramRepository
import com.example.dhikra.data.ProgramSection
import com.example.dhikra.data.ReminderSound
import com.example.dhikra.ui.formatClock
import com.example.dhikra.ui.localized
import com.example.dhikra.ui.prayer.shownName
import com.example.dhikra.ui.prayer.label
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import com.example.dhikra.ui.uiDigits

/**
 * Schedules one daily alarm per program section that has a reminder time, and one per prayer
 * whose alert is on; posts their notifications, each on the channel of its chosen sound.
 */
object ReminderScheduler {

    const val ACTION_REMIND = "com.example.dhikra.action.REMIND"
    const val ACTION_PRAYER = "com.example.dhikra.action.PRAYER"
    const val ACTION_STOP_SOUND = "com.example.dhikra.action.STOP_SOUND"
    const val EXTRA_SECTION_ID = "section_id"
    const val EXTRA_PRAYER = "prayer"
    const val EXTRA_NOTIFICATION_ID = "notification_id"

    /** The sections' channel with the default sound: the one every reminder used before. */
    private const val CHANNEL_ID = "section_reminders"
    private const val PRAYER_CHANNEL_ID = "prayer_alerts"
    private const val PREFS = "dhikra_reminders"
    private const val KEY_SCHEDULED = "scheduled_sections"
    private const val PRAYER_NOTIFICATION_BASE = 0x5A1A0

    fun scheduleAll(context: Context, program: Program) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val previous = prefs.getStringSet(KEY_SCHEDULED, emptySet()).orEmpty()

        // Sections tied to a prayer are reminded at that prayer's time each day, in the city's time.
        val city = ProgramRepository(context).loadPrayerCity()
        val source = appPrayerTimes(context)
        val zone = city?.let { source.city(it)?.timeZone }?.let(TimeZone::getTimeZone) ?: TimeZone.getDefault()
        val scheduled = program.sections.mapNotNull { section ->
            val anchor = section.anchor
            val trigger = if (anchor != null && city != null) {
                nextPrayerTrigger(Calendar.getInstance(zone)) { day ->
                    source.timetable(city, day)?.at(anchor.prayer)?.plus(anchor.offsetMinutes)
                }
            } else {
                null
            } ?: section.reminderMinutes?.let(::nextTrigger) ?: return@mapNotNull null
            schedule(context, alarmManager, alarmIntent(context, section.id), trigger)
            section.id
        }.toSet()

        (previous - scheduled).forEach { id ->
            alarmManager.cancel(alarmIntent(context, id))
        }
        prefs.edit { putStringSet(KEY_SCHEDULED, scheduled) }
    }

    /**
     * Schedules the next alert of every prayer whose alert is on, at its time in the city the user
     * picked, and cancels the others (and all of them while no city is picked).
     */
    fun schedulePrayers(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val repository = ProgramRepository(context)
        val city = repository.loadPrayerCity()
        val alerts = repository.loadPrayerAlerts()
        val source = appPrayerTimes(context)
        // The times are the city's: its days and clock, whatever the phone's own time zone.
        val zone = city?.let { source.city(it)?.timeZone }?.let(TimeZone::getTimeZone) ?: TimeZone.getDefault()
        Prayer.entries.filter { it.isPrayer }.forEach { prayer ->
            val intent = prayerIntent(context, prayer)
            val trigger = if (city != null && alerts[prayer]?.enabled == true) {
                nextPrayerTrigger(Calendar.getInstance(zone)) { day -> source.timetable(city, day)?.at(prayer) }
            } else {
                null
            }
            if (trigger != null) schedule(context, alarmManager, intent, trigger) else alarmManager.cancel(intent)
        }
    }

    fun canScheduleExactAlarms(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true

    private fun schedule(context: Context, alarmManager: AlarmManager, intent: PendingIntent, triggerAt: Long) {
        try {
            if (canScheduleExactAlarms(context)) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intent)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intent)
        }
    }

    /** Next occurrence of [minutesOfDay], today if still ahead, otherwise tomorrow. */
    fun nextTrigger(minutesOfDay: Int, now: Calendar = Calendar.getInstance()): Long {
        val trigger = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, minutesOfDay / 60)
            set(Calendar.MINUTE, minutesOfDay % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (trigger.timeInMillis <= now.timeInMillis + 1_000) trigger.add(Calendar.DAY_OF_YEAR, 1)
        return trigger.timeInMillis
    }

    /**
     * The next time a prayer comes after [now]: today's time if still ahead, else tomorrow's (prayer
     * times move from day to day). [minutesOn] gives the prayer's time on a day (`yyyy-MM-dd`), or
     * null when there is none. Days and times are read in [now]'s time zone: give it the city's.
     */
    fun nextPrayerTrigger(now: Calendar, minutesOn: (day: String) -> Int?): Long? {
        val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = now.timeZone }
        for (offset in 0..1) {
            val day = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
            val minutes = minutesOn(dayFormat.format(day.time)) ?: return null
            day.set(Calendar.HOUR_OF_DAY, minutes / 60)
            day.set(Calendar.MINUTE, minutes % 60)
            day.set(Calendar.SECOND, 0)
            day.set(Calendar.MILLISECOND, 0)
            if (day.timeInMillis > now.timeInMillis + 1_000) return day.timeInMillis
        }
        return null
    }

    private fun alarmIntent(context: Context, sectionId: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, ReminderReceiver::class.java)
                .setAction(ACTION_REMIND)
                .setData("dhikra://reminder/$sectionId".toUri())
                .putExtra(EXTRA_SECTION_ID, sectionId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun prayerIntent(context: Context, prayer: Prayer): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, ReminderReceiver::class.java)
                .setAction(ACTION_PRAYER)
                .setData("dhikra://prayer/${prayer.name}".toUri())
                .putExtra(EXTRA_PRAYER, prayer.name),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun hasNotificationPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    // ---------------------------------------------------------------- Channels

    /** The bundled azan, as a sound a channel can play. */
    fun azanUri(context: Context): Uri = "android.resource://${context.packageName}/${R.raw.azan}".toUri()

    /** The title of a sound picked from the phone, or null when it cannot be read. */
    fun deviceSoundTitle(context: Context, uri: String): String? = runCatching {
        RingtoneManager.getRingtone(context, uri.toUri())?.getTitle(context)
    }.getOrNull()

    /**
     * A notification channel plays the one sound it was created with, so each sound has its own
     * channel; the default sound keeps the [base] channel the app has always used.
     */
    private fun channelId(base: String, sound: ReminderSound): String = when (sound) {
        ReminderSound.Default -> base
        ReminderSound.Silent -> "${base}_silent"
        ReminderSound.Azan -> "${base}_azan"
        is ReminderSound.Device -> "${base}_sound_" + Integer.toHexString(sound.uri.hashCode())
    }

    /** Creates (if needed) the channel of [sound] for [base] and returns its id. */
    private fun ensureChannel(context: Context, base: String, sound: ReminderSound): String {
        @Suppress("NAME_SHADOWING") val context = context.localized() // in the app's language
        val id = channelId(base, sound)
        val (name, description) = when (base) {
            PRAYER_CHANNEL_ID -> R.string.prayer_channel to R.string.prayer_channel_desc
            else -> R.string.reminder_channel to R.string.reminder_channel_desc
        }
        val soundName = when (sound) {
            ReminderSound.Default -> null
            ReminderSound.Silent -> context.getString(R.string.sound_silent)
            ReminderSound.Azan -> context.getString(R.string.sound_azan)
            is ReminderSound.Device -> deviceSoundTitle(context, sound.uri) ?: context.getString(R.string.sound_device_fallback)
        }
        val builder = NotificationChannelCompat.Builder(id, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(soundName?.let { context.getString(R.string.channel_with_sound, context.getString(name), it) } ?: context.getString(name))
            .setDescription(context.getString(description))
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        when (sound) {
            ReminderSound.Default -> Unit
            ReminderSound.Silent -> builder.setSound(null, null)
            ReminderSound.Azan -> builder.setSound(azanUri(context), attributes)
            is ReminderSound.Device -> builder.setSound(sound.uri.toUri(), attributes)
        }
        NotificationManagerCompat.from(context).createNotificationChannel(builder.build())
        return id
    }

    /**
     * Removes the app's sound channels no reminder uses any more (after a sound was changed), so
     * the phone's notification settings only list the sounds in use. The default channels stay.
     */
    fun removeUnusedChannels(context: Context) {
        val repository = ProgramRepository(context)
        val inUse = buildSet {
            add(CHANNEL_ID)
            add(PRAYER_CHANNEL_ID)
            add(channelId(CHANNEL_ID, repository.loadSectionSound()))
            repository.loadPrayerAlerts().values.filter { it.enabled }.forEach { add(channelId(PRAYER_CHANNEL_ID, it.sound)) }
        }
        val manager = NotificationManagerCompat.from(context)
        manager.notificationChannelsCompat
            .map { it.id }
            .filter { (it.startsWith(CHANNEL_ID) || it.startsWith(PRAYER_CHANNEL_ID)) && it !in inUse }
            .forEach(manager::deleteNotificationChannel)
    }

    // ---------------------------------------------------------------- Notifications

    private fun openAppIntent(context: Context, sectionId: String?, requestCode: Int): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java)
                .apply {
                    if (sectionId != null) {
                        setData("dhikra://section/$sectionId".toUri())
                        putExtra(EXTRA_SECTION_ID, sectionId)
                    }
                }
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    // Permission is verified by hasNotificationPermission() (POST_NOTIFICATIONS only exists on 33+).
    @SuppressLint("MissingPermission")
    fun showNotification(context: Context, section: ProgramSection, sound: ReminderSound = ReminderSound.Default) {
        @Suppress("NAME_SHADOWING") val context = context.localized() // in the app's language
        if (!hasNotificationPermission(context)) return

        val channel = ensureChannel(context, CHANNEL_ID, sound)
        val items = section.trackableItems
        val preview = items.joinToString("\n") { "• " + it.title.ifBlank { it.text }.take(60) }
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.brand_primary))
            .setContentTitle("${section.icon} ${section.title}")
            .setContentText(
                context.getString(R.string.reminder_text, uiDigits(items.size))
            )
            .setStyle(NotificationCompat.BigTextStyle().bigText(preview))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppIntent(context, section.id, 0))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(section.id.hashCode(), notification)
    }

    /**
     * «حان الآن وقت صلاة الفجر»: the prayer, its time and the city. With the azan the notification
     * carries a stop button; dismissing or opening it stops the azan too.
     */
    @SuppressLint("MissingPermission")
    fun showPrayerNotification(context: Context, prayer: Prayer, timetable: PrayerTimetable, sound: ReminderSound) {
        @Suppress("NAME_SHADOWING") val context = context.localized() // in the app's language
        if (!hasNotificationPermission(context)) return

        val channel = ensureChannel(context, PRAYER_CHANNEL_ID, sound)
        val id = PRAYER_NOTIFICATION_BASE + prayer.ordinal
        val name = context.getString(prayer.label)
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.brand_primary))
            .setContentTitle(context.getString(R.string.prayer_alert_title, name))
            .setContentText(
                context.getString(R.string.prayer_alert_text, formatClock(timetable.at(prayer)), timetable.city.shownName)
            )
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppIntent(context, null, id))
            .setAutoCancel(true)
        if (sound == ReminderSound.Azan) {
            val stop = PendingIntent.getBroadcast(
                context,
                id,
                Intent(context, ReminderReceiver::class.java)
                    .setAction(ACTION_STOP_SOUND)
                    .putExtra(EXTRA_NOTIFICATION_ID, id),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(0, context.getString(R.string.stop_azan), stop)
        }
        NotificationManagerCompat.from(context).notify(id, builder.build())
    }
}
