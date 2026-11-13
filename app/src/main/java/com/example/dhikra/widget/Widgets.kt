package com.example.dhikra.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.dhikra.MainActivity
import com.example.dhikra.R
import com.example.dhikra.data.DailyCounter
import com.example.dhikra.data.ProgramRepository
import com.example.dhikra.data.appPrayerTimes
import com.example.dhikra.reminder.ReminderScheduler
import com.example.dhikra.ui.AppLanguage
import com.example.dhikra.ui.UiState
import com.example.dhikra.ui.formatClock
import com.example.dhikra.ui.localized
import com.example.dhikra.ui.prayer.label
import com.example.dhikra.ui.today.buildDayTimeline
import com.example.dhikra.ui.today.sections
import com.example.dhikra.ui.uiDigits
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.math.roundToInt
import com.example.dhikra.data.CounterStore

/**
 * The home-screen widgets: «تقدّم اليوم» (today's progress and the sections of the period at hand)
 * and «العدّاد اليومي» (the day's counters, each counted on from the home screen). Both read what
 * the app has stored, so they show the same day as the app.
 */
object Widgets {

    /** Extra of [MainActivity]'s intent: a daily counter to open (its library entry id). */
    const val EXTRA_COUNTER_ID = "counter_id"

    private const val TODAY_ROWS = 4
    private const val COUNTER_ROWS = 3

    private val refreshing = AtomicBoolean(false)

    /**
     * Redraws every widget on the home screen, off the main thread. A burst of calls (counting
     * quickly) makes one redraw, a moment after the first.
     */
    fun refresh(context: Context) {
        if (!refreshing.compareAndSet(false, true)) return
        val app = context.applicationContext
        thread(name = "widgets") {
            Thread.sleep(REFRESH_DELAY_MS)
            refreshing.set(false)
            runCatching { updateAll(app) }
        }
    }

    private const val REFRESH_DELAY_MS = 400L

    internal fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val today = manager.getAppWidgetIds(ComponentName(context, TodayWidget::class.java))
        val counters = manager.getAppWidgetIds(ComponentName(context, CountersWidget::class.java))
        if (today.isEmpty() && counters.isEmpty()) return
        val state = snapshot(context)
        if (today.isNotEmpty()) manager.updateAppWidget(today, todayViews(context, state))
        if (counters.isNotEmpty()) manager.updateAppWidget(counters, counterViews(context, state))
    }

    /** The day as the app has stored it: the program followed, today's progress and the counters. */
    private fun snapshot(context: Context): UiState {
        val repository = ProgramRepository(context)
        val store = repository.loadWithoutQuranText()
        val day = ProgramRepository.today()
        val timetable = repository.loadPrayerCity()?.let { appPrayerTimes(context).timetable(it, day) }
        val program = store.active
        return UiState(
            loading = false,
            program = program,
            library = store.library,
            schedule = program.resolvedWith(store.library).withPrayerTimes(timetable),
            day = day,
            progress = repository.loadProgress(day),
            prayerTimes = timetable,
            counters = repository.loadCounters(),
        )
    }

    private fun todayViews(context: Context, state: UiState): RemoteViews {
        val res = context.localized()
        val views = RemoteViews(context.packageName, if (AppLanguage.current.isRtl) R.layout.widget_today else R.layout.widget_today_ltr)
        val sections = state.schedule.sections
        val withSteps = sections.filter { it.trackableItems.isNotEmpty() }
        val total = withSteps.sumOf { it.trackableItems.size }
        val done = withSteps.sumOf { state.completedIn(it).coerceAtMost(it.trackableItems.size) }
        val percent = if (total == 0) 0 else (done * 100f / total).roundToInt()
        views.setTextViewText(R.id.today_percent, res.getString(R.string.progress_percent, uiDigits(percent)))
        views.setTextViewText(R.id.today_title, res.getString(R.string.today_progress))
        views.setProgressBar(R.id.today_bar, 100, percent, false)
        views.setTextViewText(R.id.today_steps, res.getString(R.string.progress_steps, uiDigits(done), uiDigits(total)))
        views.setOnClickPendingIntent(R.id.today_header, openApp(context, 0, null))

        // The period at hand, as on the home tab: the one going on, or the next with something left.
        val zone = state.prayerTimes?.city?.timeZone?.let(TimeZone::getTimeZone) ?: TimeZone.getDefault()
        val now = Calendar.getInstance(zone).let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
        val timeline = buildDayTimeline(sections, state.prayerTimes, now)
        val period = timeline.periodAtHand { state.completedIn(it) >= it.trackableItems.size }
        val rows = period?.sections.orEmpty()
        val periodLine = listOfNotNull(
            res.getString(R.string.widget_now),
            period?.prayer?.let { res.getString(it.label) },
            rows.size.takeIf { it > 0 }?.let { res.resources.getQuantityString(R.plurals.section_count, it, uiDigits(it)) },
        ).joinToString("  ·  ")
        views.setTextViewText(R.id.today_period, periodLine)

        val rowIds = listOf(
            Row(R.id.row1, R.id.row1_title, R.id.row1_time, R.id.row1_status, null),
            Row(R.id.row2, R.id.row2_title, R.id.row2_time, R.id.row2_status, R.id.divider2),
            Row(R.id.row3, R.id.row3_title, R.id.row3_time, R.id.row3_status, R.id.divider3),
            Row(R.id.row4, R.id.row4_title, R.id.row4_time, R.id.row4_status, R.id.divider4),
        )
        rowIds.forEachIndexed { index, ids ->
            val section = rows.getOrNull(index)
            val shown = if (section != null) View.VISIBLE else View.GONE
            views.setViewVisibility(ids.row, shown)
            ids.divider?.let { views.setViewVisibility(it, shown) }
            if (section == null) return@forEachIndexed
            val steps = section.trackableItems.size
            val finished = steps > 0 && state.completedIn(section) >= steps
            views.setTextViewText(ids.title, section.title)
            views.setTextViewText(ids.time, section.reminderMinutes?.let(::formatClock).orEmpty())
            views.setTextViewText(ids.status, if (finished) "✓" else uiDigits("${state.completedIn(section)}/$steps"))
            views.setTextColor(ids.status, if (finished) GOLD_INK else GREEN)
            views.setOnClickPendingIntent(ids.row, openApp(context, 100 + index) { putExtra(ReminderScheduler.EXTRA_SECTION_ID, section.id) })
        }
        views.setViewVisibility(R.id.today_empty, if (rows.isEmpty()) View.VISIBLE else View.GONE)
        views.setTextViewText(
            R.id.today_empty,
            res.getString(if (sections.isEmpty()) R.string.home_now_no_program else R.string.home_now_empty),
        )
        return views
    }

    private fun counterViews(context: Context, state: UiState): RemoteViews {
        val res = context.localized()
        val views = RemoteViews(context.packageName, if (AppLanguage.current.isRtl) R.layout.widget_counters else R.layout.widget_counters_ltr)
        views.setTextViewText(R.id.counters_header, res.getString(R.string.counter_title))
        views.setOnClickPendingIntent(R.id.counters_header, openApp(context, 1, null))
        val entries = state.counters.mapNotNull(state.library::entry)
        val rows = listOf(
            CounterSlot(R.id.counter1, R.id.counter1_title, R.id.counter1_count, R.id.counter1_plus, null),
            CounterSlot(R.id.counter2, R.id.counter2_title, R.id.counter2_count, R.id.counter2_plus, R.id.dividerc2),
            CounterSlot(R.id.counter3, R.id.counter3_title, R.id.counter3_count, R.id.counter3_plus, R.id.dividerc3),
        )
        rows.forEachIndexed { index, ids ->
            val entry = entries.getOrNull(index)
            val shown = if (entry != null) View.VISIBLE else View.GONE
            views.setViewVisibility(ids.row, shown)
            ids.divider?.let { views.setViewVisibility(it, shown) }
            if (entry == null) return@forEachIndexed
            val count = state.counterCount(entry.id)
            views.setTextViewText(ids.title, entry.title.ifBlank { entry.text })
            views.setTextViewText(ids.count, uiDigits("$count/${DailyCounter.target(count)}"))
            // Isolated left to right, so the plus stays before the one: «+١», not «١+».
            views.setTextViewText(ids.plus, "\u2066" + uiDigits("+1") + "\u2069")
            views.setOnClickPendingIntent(ids.row, openApp(context, 200 + index) { putExtra(EXTRA_COUNTER_ID, entry.id) })
            views.setOnClickPendingIntent(ids.plus, countIntent(context, 300 + index, entry.id))
        }
        views.setViewVisibility(R.id.counters_empty, if (entries.isEmpty()) View.VISIBLE else View.GONE)
        views.setTextViewText(R.id.counters_empty, res.getString(R.string.widget_counters_empty))
        return views
    }

    private fun openApp(context: Context, request: Int, extras: (Intent.() -> Unit)?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .apply { extras?.invoke(this) }
        return PendingIntent.getActivity(context, request, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun countIntent(context: Context, request: Int, entryId: String): PendingIntent {
        val intent = Intent(context, WidgetActionReceiver::class.java)
            .setAction(WidgetActionReceiver.ACTION_COUNT)
            .putExtra(EXTRA_COUNTER_ID, entryId)
        return PendingIntent.getBroadcast(context, request, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private data class Row(val row: Int, val title: Int, val time: Int, val status: Int, val divider: Int?)
    private data class CounterSlot(val row: Int, val title: Int, val count: Int, val plus: Int, val divider: Int?)

    private const val GREEN = 0xFF1B4E38.toInt()
    private const val GOLD_INK = 0xFF8C5A12.toInt()
}

/** «تقدّم اليوم»: today's progress and the sections of the period at hand. */
class TodayWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) = updateAsync(context)
}

/** «العدّاد اليومي»: the day's counters, each counted on from the home screen. */
class CountersWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) = updateAsync(context)
}

private fun BroadcastReceiver.updateAsync(context: Context) {
    val pending = goAsync()
    val app = context.applicationContext
    thread(name = "widgets") {
        try {
            runCatching { Widgets.updateAll(app) }
        } finally {
            pending.finish()
        }
    }
}

/** A counter's «+١» on the widget: one more for today, saved as the app saves it. */
class WidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_COUNT) return
        val entryId = intent.getStringExtra(Widgets.EXTRA_COUNTER_ID) ?: return
        val pending = goAsync()
        val app = context.applicationContext
        thread(name = "widget-count") {
            try {
                CounterStore.addNow(app, entryId, 1)
                runCatching { Widgets.updateAll(app) }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_COUNT = "com.example.dhikra.widget.COUNT"
    }
}
