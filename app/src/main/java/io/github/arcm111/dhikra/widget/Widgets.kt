package io.github.arcm111.dhikra.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Bundle
import android.text.TextPaint
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.res.ResourcesCompat
import io.github.arcm111.dhikra.MainActivity
import io.github.arcm111.dhikra.R
import io.github.arcm111.dhikra.reminder.ReminderScheduler
import io.github.arcm111.dhikra.data.ProgramRepository
import io.github.arcm111.dhikra.data.appPrayerTimes
import io.github.arcm111.dhikra.ui.AppLanguage
import io.github.arcm111.dhikra.ui.formatClock
import io.github.arcm111.dhikra.ui.localized
import io.github.arcm111.dhikra.ui.prayer.label
import io.github.arcm111.dhikra.ui.prayer.shownName
import io.github.arcm111.dhikra.ui.today.DayPeriod
import io.github.arcm111.dhikra.ui.today.buildDayTimeline
import io.github.arcm111.dhikra.ui.uiDigits
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * The home-screen widgets, in the dark green of the home tab's header:
 * - «وقت الصلاة»: as the header's prayer card — the time left to the next prayer on a draining
 *   ring (counting down live), its name and hour, the prayer going on with the city, and the
 *   day's prayers on a 24-hour line;
 * - «التقويم الهجري»: the Hijri month (Umm al-Qura), each day with its Gregorian date under it,
 *   today in gold.
 * Both read what the app has stored, and are redrawn when the prayer changes or the day turns.
 */
object Widgets {

    /** Extra of [MainActivity]'s intent: a daily counter to open (its library entry id). */
    const val EXTRA_COUNTER_ID = "counter_id"

    /** Broadcast to [PrayerWidget]: the widgets' time to be redrawn has come. */
    internal const val ACTION_TICK = "io.github.arcm111.dhikra.widget.TICK"

    private val refreshing = AtomicBoolean(false)

    /**
     * Redraws every widget on the home screen, off the main thread. A burst of calls makes one
     * redraw, a moment after the first.
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
        val prayer = manager.getAppWidgetIds(ComponentName(context, PrayerWidget::class.java))
        val hijri = manager.getAppWidgetIds(ComponentName(context, HijriWidget::class.java))
        if (prayer.isEmpty() && hijri.isEmpty()) {
            cancelTick(context)
            return
        }
        val repository = ProgramRepository(context)
        val source = appPrayerTimes(context)
        val city = repository.loadPrayerCity()
        val zone = city?.let { source.city(it)?.timeZone }?.let(TimeZone::getTimeZone) ?: TimeZone.getDefault()
        val now = System.currentTimeMillis()
        val timetable = city?.let { source.timetable(it, ProgramRepository.today()) }
        var nextUpdate = nextMidnight(now, zone)
        prayer.forEach { id ->
            val (views, until) = prayerViews(context, timetable, zone, now, manager.getAppWidgetOptions(id))
            manager.updateAppWidget(id, views)
            if (until != null) nextUpdate = minOf(nextUpdate, until)
        }
        hijri.forEach { id -> manager.updateAppWidget(id, hijriViews(context, zone, now, manager.getAppWidgetOptions(id))) }
        scheduleTick(context, nextUpdate)
    }

    // ---------------------------------------------------------------- The prayer time

    /** The prayer widget, and when it next needs redrawing (null: at midnight). */
    private fun prayerViews(
        context: Context,
        timetable: io.github.arcm111.dhikra.data.PrayerTimetable?,
        zone: TimeZone,
        now: Long,
        options: Bundle,
    ): Pair<RemoteViews, Long?> {
        val res = context.localized()
        val rtl = AppLanguage.current.isRtl
        val views = RemoteViews(context.packageName, if (rtl) R.layout.widget_prayer else R.layout.widget_prayer_ltr)
        views.setOnClickPendingIntent(R.id.prayer_root, openApp(context, 0))
        if (timetable == null) {
            views.setViewVisibility(R.id.prayer_content, View.GONE)
            views.setViewVisibility(R.id.prayer_empty, View.VISIBLE)
            views.setTextViewText(R.id.prayer_empty, res.getString(R.string.day_pick_city))
            return views to null
        }
        views.setViewVisibility(R.id.prayer_content, View.VISIBLE)
        views.setViewVisibility(R.id.prayer_empty, View.GONE)

        val clock = Calendar.getInstance(zone).apply { timeInMillis = now }
        val second = clock.get(Calendar.HOUR_OF_DAY) * 3600 + clock.get(Calendar.MINUTE) * 60 + clock.get(Calendar.SECOND)
        val nowMinutes = second / 60
        val periods = buildDayTimeline(emptyList(), timetable, nowMinutes).periods
        val index = periods.indexOfFirst { it.current }.coerceAtLeast(0)
        val period = periods[index]
        val next = periods[(index + 1) % periods.size]
        val prayer = period.prayer ?: return views to null
        val nextPrayer = next.prayer ?: return views to null

        val secondsLeft = Math.floorMod(period.end * 60 - second, SECONDS_PER_DAY)
        val length = Math.floorMod(period.end - period.start, 24 * 60).let { if (it == 0) 24 * 60 else it } * 60
        val elapsed = (Math.floorMod(second - period.start * 60, SECONDS_PER_DAY).toFloat() / length).coerceIn(0f, 1f)
        val lastHour = secondsLeft < 3600
        // The light gold of the day's line while time is ample, its orange in the last hour.
        val accent = if (lastHour) ORANGE else MUSTARD
        val density = context.resources.displayMetrics.density

        views.setTextViewText(R.id.prayer_next_name, res.getString(nextPrayer.label))
        views.setTextColor(R.id.prayer_next_name, accent)
        views.setTextViewText(R.id.prayer_next_time, formatClock(next.start))
        views.setImageViewBitmap(R.id.prayer_ring, timerRing((RING_DP * density).toInt(), elapsed, accent, rtl))
        // The time left, as on the header's dial: «٧:٣٤» (h:mm), redrawn every minute.
        views.setTextViewText(R.id.prayer_countdown, uiDigits("%d:%02d".format(secondsLeft / 3600, secondsLeft % 3600 / 60)))
        views.setContentDescription(R.id.prayer_countdown, res.getString(R.string.widget_prayer_left, res.getString(nextPrayer.label)))

        views.setTextViewText(R.id.prayer_title, res.getString(R.string.prayer_time_title, res.getString(prayer.label)))
        views.setTextViewText(R.id.prayer_city, timetable.city.shownName)
        val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH).takeIf { it > 0 } ?: DEFAULT_WIDTH_DP
        val stripWidth = ((widthDp - GLASS_DP - STRIP_INSET_DP) * density).toInt().coerceAtLeast((80 * density).toInt())
        views.setImageViewBitmap(
            R.id.prayer_strip,
            dayStrip(context, stripWidth, density, periods, index, nowMinutes, accent, rtl),
        )

        // Redrawn on the next minute: the countdown, the ring and the line move on (and the prayer
        // changes on a minute too).
        return views to now + (60 - second % 60) * 1000L
    }

    /**
     * The header's timer dial: a ring of ticks, those of the time left lit in [color], going out one
     * by one from the top as [elapsed] (0 to 1) grows, the other way round from the progress rings.
     */
    private fun timerRing(size: Int, elapsed: Float, color: Int, rtl: Boolean): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val unit = size / 60f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 2f * unit
            strokeCap = Paint.Cap.ROUND
        }
        val c = size / 2f
        val radius = c - 2f * unit
        val tick = 3.5f * unit
        val direction = if (rtl) 1 else -1
        for (i in 0 until TIMER_TICKS) {
            val share = i / TIMER_TICKS.toFloat()
            val angle = -PI / 2 + direction * 2 * PI * share
            paint.color = if (share >= elapsed) color else 0x24FFFFFF
            canvas.drawLine(
                c + ((radius - tick) * cos(angle)).toFloat(), c + ((radius - tick) * sin(angle)).toFloat(),
                c + (radius * cos(angle)).toFloat(), c + (radius * sin(angle)).toFloat(),
                paint,
            )
        }
        return bitmap
    }

    /**
     * The day's prayers on a 24-hour line, as on the header: a mark at each prayer, its letter above
     * and its time below — the one going on in gold — the time going on filled up to now, and a
     * gold tick at now. Labels too close to fit side by side take a second row.
     */
    private fun dayStrip(
        context: Context,
        width: Int,
        density: Float,
        periods: List<DayPeriod>,
        current: Int,
        nowMinutes: Int,
        accent: Int,
        rtl: Boolean,
    ): Bitmap {
        val res = context.localized()
        val font = font(context, R.font.ibm_plex_sans_arabic_medium)
        val bold = font(context, R.font.ibm_plex_sans_arabic_bold)
        fun x(minutes: Int): Float {
            val f = Math.floorMod(minutes, 24 * 60) / (24f * 60f)
            val inner = width - 2 * EDGE * density
            return EDGE * density + if (rtl) inner * (1 - f) else inner * f
        }
        val positions = periods.map { x(it.start) }
        val letters = periods.map { period ->
            period.prayer?.let { res.getString(it.label) }.orEmpty().let { name -> name.removePrefix("ال").ifEmpty { name }.take(1) }
        }
        val times = periods.map { formatClock(it.start) }
        val letterRows = labelRows(positions, 12 * density)
        val timeRows = labelRows(positions, 30 * density)
        val letterRowHeight = 16 * density
        val lineHeight = 12 * density
        val timeRowHeight = 15 * density
        val height = ((letterRows.maxOrNull() ?: 0) + 1) * letterRowHeight + lineHeight + ((timeRows.maxOrNull() ?: 0) + 1) * timeRowHeight
        val bitmap = Bitmap.createBitmap(width, height.toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val text = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11 * density
            textAlign = Paint.Align.CENTER
        }
        fun label(value: String, centre: Float, baseline: Float) {
            val half = text.measureText(value) / 2
            canvas.drawText(value, centre.coerceIn(half, width - half), baseline, text)
        }
        periods.indices.forEach { i ->
            text.typeface = if (i == current) bold else font
            text.color = if (i == current) MUSTARD else 0x99FFFFFF.toInt()
            label(letters[i], positions[i], letterRows[i] * letterRowHeight + 12 * density)
        }
        val lineTop = ((letterRows.maxOrNull() ?: 0) + 1) * letterRowHeight
        val y = lineTop + lineHeight / 2
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 3 * density
            strokeCap = Paint.Cap.ROUND
            color = 0x29FFFFFF
        }
        canvas.drawLine(x(0), y, x(24 * 60 - 1), y, line)
        // The time going on, up to now (two pieces when it runs past midnight).
        line.color = accent
        val start = periods[current].start
        fun fill(from: Int, to: Int) = canvas.drawLine(x(from), y, if (to >= 24 * 60) x(24 * 60 - 1) else x(to), y, line)
        if (nowMinutes >= start) fill(start, nowMinutes) else {
            fill(start, 24 * 60)
            fill(0, nowMinutes)
        }
        val mark = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 1.5f * density
            strokeCap = Paint.Cap.ROUND
        }
        positions.forEachIndexed { i, px ->
            mark.color = if (i == current) MUSTARD else 0x8CFFFFFF.toInt()
            canvas.drawLine(px, y - 4 * density, px, y + 4 * density, mark)
        }
        mark.color = MUSTARD
        mark.strokeWidth = 2 * density
        canvas.drawLine(x(nowMinutes), lineTop, x(nowMinutes), lineTop + lineHeight, mark)
        val timesTop = lineTop + lineHeight
        text.typeface = font
        periods.indices.forEach { i ->
            text.color = if (i == current) 0xFFFFFFFF.toInt() else 0x8CFFFFFF.toInt()
            label(times[i], positions[i], timesTop + timeRows[i] * timeRowHeight + 11 * density)
        }
        return bitmap
    }

    /** The row of each label at [positions], a later one on the next row when it is closer than [gap]. */
    private fun labelRows(positions: List<Float>, gap: Float): List<Int> {
        val rows = MutableList(positions.size) { 0 }
        val order = positions.indices.sortedBy { positions[it] }
        val lastInRow = mutableMapOf<Int, Float>()
        for (i in order) {
            var row = 0
            while (lastInRow[row]?.let { positions[i] - it < gap } == true) row++
            rows[i] = row
            lastInRow[row] = positions[i]
        }
        return rows
    }

    // ---------------------------------------------------------------- The Hijri month

    private fun hijriViews(context: Context, zone: TimeZone, now: Long, options: Bundle): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_hijri)
        val density = context.resources.displayMetrics.density
        val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH).takeIf { it > 0 } ?: DEFAULT_WIDTH_DP
        val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT).takeIf { it > 0 } ?: DEFAULT_HIJRI_HEIGHT_DP
        // Drawn at the widget's size (not past a sharp enough limit), so the days are not stretched.
        val scale = min(density, MAX_BITMAP_PX / maxOf(widthDp, heightDp).toFloat())
        views.setImageViewBitmap(
            R.id.hijri_image,
            hijriMonth(context, (widthDp * scale).toInt().coerceAtLeast(100), (heightDp * scale).toInt().coerceAtLeast(100), scale, zone, now),
        )
        views.setOnClickPendingIntent(R.id.hijri_root, openApp(context, 1))
        return views
    }

    /**
     * The Hijri month of [now]: its name and year in gold with the Gregorian months it spans, the
     * weekdays (Friday in gold), and a cell for each day — the Hijri day large, the Gregorian one
     * small under it (the first of a Gregorian month in gold) — today on a gold tile.
     */
    private fun hijriMonth(context: Context, width: Int, height: Int, unit: Float, zone: TimeZone, now: Long): Bitmap {
        val arabic = AppLanguage.current == AppLanguage.AR
        val rtl = AppLanguage.current.isRtl
        val icuZone = android.icu.util.TimeZone.getTimeZone(zone.id)
        val hijri = android.icu.util.IslamicCalendar(icuZone, android.icu.util.ULocale("ar")).apply {
            calculationType = android.icu.util.IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
            timeInMillis = now
        }
        val today = hijri.get(android.icu.util.Calendar.DAY_OF_MONTH)
        val days = hijri.getActualMaximum(android.icu.util.Calendar.DAY_OF_MONTH)
        hijri.set(android.icu.util.Calendar.DAY_OF_MONTH, 1)
        hijri.set(android.icu.util.Calendar.HOUR_OF_DAY, 12)
        val firstWeekday = hijri.get(android.icu.util.Calendar.DAY_OF_WEEK)
        val dates = (1..days).map { day ->
            hijri.set(android.icu.util.Calendar.DAY_OF_MONTH, day)
            hijri.timeInMillis
        }
        val gregorian = Calendar.getInstance(zone)
        fun gregorianDay(millis: Long) = gregorian.apply { timeInMillis = millis }.get(Calendar.DAY_OF_MONTH)

        val numbers = if (arabic) "ar@numbers=arab" else "en"
        fun format(skeleton: String, locale: String, millis: Long) =
            android.icu.text.DateFormat.getInstanceForSkeleton(skeleton, android.icu.util.ULocale(locale))
                .apply { timeZone = icuZone }
                .format(Date(millis))
        val monthTitle = format("yMMMM", if (arabic) "ar@calendar=islamic-umalqura;numbers=arab" else "en@calendar=islamic-umalqura", now)
        val first = dates.first()
        val last = dates.last()
        val span = if (format("yM", "en", first) == format("yM", "en", last)) format("yMMMM", numbers, first)
        else if (format("y", "en", first) == format("y", "en", last)) format("MMMM", numbers, first) + " – " + format("yMMMM", numbers, last)
        else format("yMMMM", numbers, first) + " – " + format("yMMMM", numbers, last)

        // The week: from Saturday in Arabic, as the phone has it otherwise.
        val weekStart = if (arabic) Calendar.SATURDAY else Calendar.getInstance().firstDayOfWeek
        // Arabic by the usual letters (السبت س، الأحد ح، الاثنين ن …): the narrow forms repeat «أ».
        val weekdayNames = if (arabic) arrayOf("", "ح", "ن", "ث", "ر", "خ", "ج", "س")
        else android.icu.text.DateFormatSymbols(android.icu.util.ULocale("en"))
            .getWeekdays(android.icu.text.DateFormatSymbols.STANDALONE, android.icu.text.DateFormatSymbols.SHORT)
        val offset = Math.floorMod(firstWeekday - weekStart, 7)
        val rows = (offset + days + 6) / 7

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val semibold = font(context, R.font.ibm_plex_sans_arabic_semibold)
        val medium = font(context, R.font.ibm_plex_sans_arabic_medium)
        val regular = font(context, R.font.ibm_plex_sans_arabic_regular)
        val pad = 14 * unit
        val text = TextPaint(Paint.ANTI_ALIAS_FLAG)

        // The month: its name and year, the Gregorian months under it.
        text.typeface = semibold
        text.textSize = 17 * unit
        text.color = MUSTARD
        text.textAlign = if (rtl) Paint.Align.RIGHT else Paint.Align.LEFT
        val startX = if (rtl) width - pad else pad
        var y = pad + 17 * unit
        canvas.drawText(fit(monthTitle, text, width - 2 * pad), startX, y, text)
        text.typeface = regular
        text.textSize = 12 * unit
        text.color = 0x99FFFFFF.toInt()
        y += 18 * unit
        canvas.drawText(fit(span, text, width - 2 * pad), startX, y, text)

        // The weekdays.
        val gridLeft = pad * 0.6f
        val cellWidth = (width - 2 * gridLeft) / 7
        fun columnX(column: Int) = if (rtl) width - gridLeft - (column + 0.5f) * cellWidth else gridLeft + (column + 0.5f) * cellWidth
        y += 22 * unit
        text.textAlign = Paint.Align.CENTER
        text.typeface = medium
        text.textSize = 11 * unit
        for (column in 0 until 7) {
            val weekday = (weekStart - 1 + column) % 7 + 1
            text.color = if (weekday == Calendar.FRIDAY) MUSTARD else 0x8CFFFFFF.toInt()
            canvas.drawText(weekdayNames[weekday], columnX(column), y, text)
        }
        y += 8 * unit
        val divider = Paint().apply { color = 0x1FFFFFFF; strokeWidth = unit }
        canvas.drawLine(gridLeft, y, width - gridLeft, y, divider)
        y += 4 * unit

        // The days.
        val cellHeight = (height - y - pad * 0.6f) / rows
        val hijriSize = min(cellHeight * 0.42f, cellWidth * 0.42f).coerceAtMost(20 * unit)
        val gregorianSize = hijriSize * 0.6f
        val tile = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = MUSTARD }
        for (day in 1..days) {
            val cell = offset + day - 1
            val cx = columnX(cell % 7)
            val top = y + (cell / 7) * cellHeight
            val isToday = day == today
            if (isToday) {
                val w = min(cellWidth, cellHeight) * 0.92f
                canvas.drawRoundRect(RectF(cx - w / 2, top + (cellHeight - w) / 2, cx + w / 2, top + (cellHeight + w) / 2), w * 0.28f, w * 0.28f, tile)
            }
            val block = hijriSize + gregorianSize * 1.15f
            val base = top + (cellHeight - block) / 2 + hijriSize * 0.92f
            text.typeface = if (isToday) semibold else medium
            text.textSize = hijriSize
            text.color = when {
                isToday -> DEEP_GREEN
                day < today -> 0x99FFFFFF.toInt()
                else -> 0xEBFFFFFF.toInt()
            }
            canvas.drawText(uiDigits(day), cx, base, text)
            val gDay = gregorianDay(dates[day - 1])
            text.typeface = regular
            text.textSize = gregorianSize
            text.color = when {
                isToday -> 0xCC0E3122.toInt()
                gDay == 1 -> MUSTARD
                else -> 0x80FFFFFF.toInt()
            }
            canvas.drawText(uiDigits(gDay), cx, base + gregorianSize * 1.2f, text)
        }
        return bitmap
    }

    /** [value] cut with an ellipsis to [width] in [paint]. */
    private fun fit(value: String, paint: TextPaint, width: Float): String =
        android.text.TextUtils.ellipsize(value, paint, width, android.text.TextUtils.TruncateAt.END).toString()

    // ---------------------------------------------------------------- Shared

    private fun font(context: Context, id: Int): Typeface =
        runCatching { ResourcesCompat.getFont(context, id) }.getOrNull() ?: Typeface.DEFAULT

    private fun openApp(context: Context, request: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(context, request, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun nextMidnight(now: Long, zone: TimeZone): Long = Calendar.getInstance(zone).apply {
        timeInMillis = now
        add(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 5)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun tickIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, PrayerWidget::class.java).setAction(ACTION_TICK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /**
     * The next redraw, on time while the screen is on; it never wakes the phone (a home screen not
     * looked at needs no redrawing — the first alarm after it wakes catches up).
     */
    private fun scheduleTick(context: Context, at: Long) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        runCatching {
            if (ReminderScheduler.canScheduleExactAlarms(context)) alarms.setExact(AlarmManager.RTC, at, tickIntent(context))
            else alarms.set(AlarmManager.RTC, at, tickIntent(context))
        }
    }

    private fun cancelTick(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(tickIntent(context))
    }

    private const val SECONDS_PER_DAY = 24 * 3600
    private const val TIMER_TICKS = 48
    private const val RING_DP = 60
    /** The glass pane's width and its margins, and the text column's padding (dp). */
    private const val GLASS_DP = 96
    private const val STRIP_INSET_DP = 34
    /** The day line's ends kept in from the edges, so labels at 00:00 and 24:00 are not cut (dp). */
    private const val EDGE = 6
    private const val DEFAULT_WIDTH_DP = 320
    private const val DEFAULT_HIJRI_HEIGHT_DP = 300
    private const val MAX_BITMAP_PX = 1400f

    private const val MUSTARD = 0xFFE6CF86.toInt()
    private const val ORANGE = 0xFFE9A46A.toInt()
    private const val DEEP_GREEN = 0xFF0E3122.toInt()
}

/** «وقت الصلاة»: the time left to the next prayer and the day's prayers, as on the header. */
class PrayerWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) = updateAsync(context)

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) =
        updateAsync(context)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Widgets.ACTION_TICK) updateAsync(context) else super.onReceive(context, intent)
    }
}

/** «التقويم الهجري»: the Hijri month with the Gregorian dates, today in gold. */
class HijriWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) = updateAsync(context)

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) =
        updateAsync(context)
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
