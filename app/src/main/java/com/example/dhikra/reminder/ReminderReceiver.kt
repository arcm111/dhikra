package com.example.dhikra.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.dhikra.data.ProgramRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Fires when a section's reminder time arrives: notifies and schedules the next day. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderScheduler.ACTION_REMIND) return
        val sectionId = intent.getStringExtra(ReminderScheduler.EXTRA_SECTION_ID) ?: return
        goAsyncOnIo(context) { appContext ->
            // Only the program being followed has reminders.
            val bundle = ProgramRepository(appContext).load().bundle
            bundle.program.resolvedWith(bundle.library).section(sectionId)
                ?.let { ReminderScheduler.showNotification(appContext, it) }
            ReminderScheduler.scheduleAll(appContext, bundle.program)
        }
    }
}

/** Restores reminders after reboot, app update or clock/timezone changes. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in RESCHEDULE_ACTIONS) return
        goAsyncOnIo(context) { appContext ->
            ReminderScheduler.scheduleAll(appContext, ProgramRepository(appContext).load().active)
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
