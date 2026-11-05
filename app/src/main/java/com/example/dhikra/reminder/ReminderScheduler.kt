package com.example.dhikra.reminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import com.example.dhikra.MainActivity
import com.example.dhikra.R
import com.example.dhikra.data.ArabicText
import com.example.dhikra.data.Program
import com.example.dhikra.data.ProgramSection
import java.util.Calendar

/** Schedules one daily alarm per program section that has a reminder time. */
object ReminderScheduler {

    const val ACTION_REMIND = "com.example.dhikra.action.REMIND"
    const val EXTRA_SECTION_ID = "section_id"
    private const val CHANNEL_ID = "section_reminders"
    private const val PREFS = "dhikra_reminders"
    private const val KEY_SCHEDULED = "scheduled_sections"

    fun scheduleAll(context: Context, program: Program) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val previous = prefs.getStringSet(KEY_SCHEDULED, emptySet()).orEmpty()

        val scheduled = program.sections.mapNotNull { section ->
            val minutes = section.reminderMinutes ?: return@mapNotNull null
            schedule(context, alarmManager, section.id, nextTrigger(minutes))
            section.id
        }.toSet()

        (previous - scheduled).forEach { id ->
            alarmManager.cancel(alarmIntent(context, id))
        }
        prefs.edit { putStringSet(KEY_SCHEDULED, scheduled) }
    }

    fun canScheduleExactAlarms(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true

    private fun schedule(context: Context, alarmManager: AlarmManager, sectionId: String, triggerAt: Long) {
        val intent = alarmIntent(context, sectionId)
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

    fun hasNotificationPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    // Permission is verified by hasNotificationPermission() (POST_NOTIFICATIONS only exists on 33+).
    @SuppressLint("MissingPermission")
    fun showNotification(context: Context, section: ProgramSection) {
        if (!hasNotificationPermission(context)) return

        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(context.getString(R.string.reminder_channel))
                .setDescription(context.getString(R.string.reminder_channel_desc))
                .build()
        )

        val openIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .setData("dhikra://section/${section.id}".toUri())
                .putExtra(EXTRA_SECTION_ID, section.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val items = section.trackableItems
        val preview = items.joinToString("\n") { "• " + it.title.ifBlank { it.text }.take(60) }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.emerald))
            .setContentTitle("${section.icon} ${section.title}")
            .setContentText(
                context.getString(R.string.reminder_text, ArabicText.toArabicDigits(items.size))
            )
            .setStyle(NotificationCompat.BigTextStyle().bigText(preview))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .build()
        manager.notify(section.id.hashCode(), notification)
    }
}
