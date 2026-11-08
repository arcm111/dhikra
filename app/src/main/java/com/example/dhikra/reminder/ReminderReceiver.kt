package com.example.dhikra.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.dhikra.data.appPrayerTimes
import com.example.dhikra.data.Prayer
import com.example.dhikra.data.ProgramRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Fires when a section's reminder time or a prayer's time arrives: notifies with the chosen sound
 * and schedules the next day. Also stops the azan when its notification's stop button is tapped.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ReminderScheduler.ACTION_REMIND -> {
                val sectionId = intent.getStringExtra(ReminderScheduler.EXTRA_SECTION_ID) ?: return
                goAsyncOnIo(context) { appContext ->
                    // Only the program being followed has reminders.
                    val repository = ProgramRepository(appContext)
                    val bundle = repository.load().bundle
                    bundle.program.resolvedWith(bundle.library).section(sectionId)
                        ?.let { ReminderScheduler.showNotification(appContext, it, repository.loadSectionSound()) }
                    ReminderScheduler.scheduleAll(appContext, bundle.program)
                }
            }

            ReminderScheduler.ACTION_PRAYER -> {
                val prayer = intent.getStringExtra(ReminderScheduler.EXTRA_PRAYER)
                    ?.let { name -> Prayer.entries.firstOrNull { it.name == name } } ?: return
                goAsyncOnIo(context) { appContext ->
                    val repository = ProgramRepository(appContext)
                    val alert = repository.loadPrayerAlerts()[prayer]
                    val source = appPrayerTimes(appContext)
                    // "Today" in the city's own time zone: the day whose prayer is due now.
                    val timetable = repository.loadPrayerCity()?.let { cityId ->
                        val zone = source.city(cityId)?.timeZone?.let(TimeZone::getTimeZone) ?: TimeZone.getDefault()
                        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = zone }.format(Date())
                        source.timetable(cityId, today)
                    }
                    if (alert?.enabled == true && timetable != null) {
                        ReminderScheduler.showPrayerNotification(appContext, prayer, timetable, alert.sound)
                    }
                    ReminderScheduler.schedulePrayers(appContext)
                }
            }

            // Cancelling the notification stops the sound it is playing.
            ReminderScheduler.ACTION_STOP_SOUND -> {
                val id = intent.getIntExtra(ReminderScheduler.EXTRA_NOTIFICATION_ID, -1)
                if (id != -1) NotificationManagerCompat.from(context).cancel(id)
            }
        }
    }
}

/** Restores reminders and prayer alerts after reboot, app update or clock/timezone changes. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in RESCHEDULE_ACTIONS) return
        goAsyncOnIo(context) { appContext ->
            ReminderScheduler.scheduleAll(appContext, ProgramRepository(appContext).load().active)
            ReminderScheduler.schedulePrayers(appContext)
        }
    }

    private companion object {
        val RESCHEDULE_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
        )
    }
}

private fun BroadcastReceiver.goAsyncOnIo(context: Context, block: (Context) -> Unit) {
    val pending = goAsync()
    val appContext = context.applicationContext
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        try {
            block(appContext)
        } finally {
            pending.finish()
        }
    }
}
