package io.github.arcm111.dhikra.ui.today

import io.github.arcm111.dhikra.data.Prayer
import io.github.arcm111.dhikra.data.PrayerTimetable
import io.github.arcm111.dhikra.data.ProgramSection

private const val MINUTES_PER_DAY = 24 * 60

/** Something placed inside a [DayPeriod], at [minutes] after midnight. */
sealed interface PeriodEntry {
    val minutes: Int

    /**
     * A section of the program, at its reminder time; [passed] once a later section of the
     * current period has started, or its period is over.
     */
    data class Section(val section: ProgramSection, override val minutes: Int, val passed: Boolean = false) : PeriodEntry

    /** The current time. */
    data class Now(override val minutes: Int) : PeriodEntry
}

/**
 * A stretch of the day from one prayer time ([start]) to the next ([end]; the night period of
 * العشاء runs past midnight to the next الفجر), holding the sections whose time falls inside it
 * and, when it is the [current] one, the current time.
 *
 * Without a city there are no prayer times: one period with no [prayer] holds the whole day.
 */
data class DayPeriod(
    val prayer: Prayer?,
    val start: Int,
    val end: Int,
    val current: Boolean,
    /** Minutes until [prayer] when it is the coming prayer, else null. */
    val untilPrayer: Int?,
    val entries: List<PeriodEntry>,
    /** The period ended earlier today. */
    val passed: Boolean = false,
)

/** The sections of a period, in their order. */
val DayPeriod.sections: List<ProgramSection> get() = entries.filterIsInstance<PeriodEntry.Section>().map { it.section }

/** The day at a glance: its prayer [periods] in order, and the sections with no time ([untimed]). */
data class DayTimeline(val periods: List<DayPeriod>, val untimed: List<ProgramSection>) {
    /** Position of the period the current time falls in. */
    val currentIndex: Int get() = periods.indexOfFirst { it.current }

    /**
     * The period to show as "now": the one going on if a section of it is still to do ([finished]
     * tells), else the next one that has such a section; the one going on when nothing is left.
     */
    fun periodAtHand(finished: (ProgramSection) -> Boolean): DayPeriod? {
        val from = currentIndex.coerceAtLeast(0)
        return periods.drop(from).firstOrNull { period -> period.sections.any { !finished(it) } }
            ?: periods.getOrNull(currentIndex)
    }

    /**
     * The section whose time it is: the last one to have started in the current period, read on
     * the city's clock like the periods (so it always agrees with them). Null before the period's
     * first section.
     */
    val nowSectionId: String?
        get() = periods.firstOrNull { it.current }?.entries
            ?.takeWhile { it !is PeriodEntry.Now }
            ?.filterIsInstance<PeriodEntry.Section>()
            ?.lastOrNull()?.section?.id
}

/** Marks what the current time has left behind: the sections before the current one, and [period]'s
 * own sections when the whole period is over. */
private fun markPassed(period: DayPeriod): DayPeriod {
    if (period.passed) {
        return period.copy(entries = period.entries.map { if (it is PeriodEntry.Section) it.copy(passed = true) else it })
    }
    if (!period.current) return period
    val nowAt = period.entries.indexOfFirst { it is PeriodEntry.Now }
    val lastStarted = period.entries.subList(0, nowAt.coerceAtLeast(0)).indexOfLast { it is PeriodEntry.Section }
    return period.copy(
        entries = period.entries.mapIndexed { index, entry ->
            if (entry is PeriodEntry.Section && index < lastStarted) entry.copy(passed = true) else entry
        }
    )
}

/**
 * Splits the day into the periods of [timetable]'s prayers (الفجر → الشروق → الظهر → العصر →
 * المغرب → العشاء → next الفجر) and places each timed section of [sections] in the period its
 * reminder time falls in, in time order from the period's start; sections of the same minute keep
 * the program's order, and the current time [nowMinutes] comes after what starts that minute.
 * Without a [timetable], one period holds every timed section.
 */
fun buildDayTimeline(
    sections: List<ProgramSection>,
    timetable: PrayerTimetable?,
    nowMinutes: Int,
): DayTimeline {
    val timed = sections.mapNotNull { section -> section.reminderMinutes?.let { PeriodEntry.Section(section, it) } }
    val untimed = sections.filter { it.reminderMinutes == null }
    val now = PeriodEntry.Now(nowMinutes)

    // Minutes from a period's start; the night period wraps past midnight.
    fun offset(minutes: Int, start: Int) = Math.floorMod(minutes - start, MINUTES_PER_DAY)
    // sortedWith is stable: sections of the same minute stay in program order.
    fun ordered(entries: List<PeriodEntry>, start: Int) = entries.sortedWith(
        compareBy<PeriodEntry> { offset(it.minutes, start) }.thenBy { if (it is PeriodEntry.Now) 1 else 0 }
    )

    if (timetable == null) {
        return DayTimeline(
            listOf(markPassed(DayPeriod(null, 0, MINUTES_PER_DAY, current = true, untilPrayer = null, ordered(timed + now, 0)))),
            untimed,
        )
    }
    val next = timetable.next(nowMinutes)
    val prayers = Prayer.entries
    val periods = prayers.mapIndexed { index, prayer ->
        val start = timetable.at(prayer)
        val end = timetable.at(prayers[(index + 1) % prayers.size])
        fun within(minutes: Int) = offset(minutes, start) < offset(end, start)
        val current = within(nowMinutes)
        markPassed(
            DayPeriod(
                prayer = prayer,
                start = start,
                end = end,
                current = current,
                untilPrayer = next.inMinutes.takeIf { prayer == next.prayer },
                entries = ordered(timed.filter { within(it.minutes) } + listOfNotNull(now.takeIf { current }), start),
                // Over earlier today: it ended before now (the night period, which runs past
                // midnight, is never "earlier today" unless it is the one going on).
                passed = !current && start < end && end <= nowMinutes,
            )
        )
    }
    return DayTimeline(periods, untimed)
}

// ---------------------------------------------------------------- The prayer-time card

/** Minutes before the next prayer from which the time going on is shown as closing (red). */
const val CLOSING_MINUTES = 15

/**
 * The share (0 to 1) of the period from [start] to [end] (minutes of the day) that has passed at
 * [now]; the period may run past midnight (Isha to Fajr).
 */
fun periodShare(start: Int, end: Int, now: Int): Float {
    val length = Math.floorMod(end - start, MINUTES_PER_DAY).let { if (it == 0) MINUTES_PER_DAY else it }
    val passed = Math.floorMod(now - start, MINUTES_PER_DAY)
    return (passed.toFloat() / length).coerceIn(0f, 1f)
}

/** Minutes from [now] until [end] (minutes of the day), across midnight if need be. */
fun minutesLeft(end: Int, now: Int): Int = Math.floorMod(end - now, MINUTES_PER_DAY)

/** Whether so few minutes are left of the time going on that it is shown as closing. */
fun isClosing(minutesLeft: Int): Boolean = minutesLeft <= CLOSING_MINUTES

/**
 * The row (0 or 1) of each label at [positions] along a line, so that two labels on one row are
 * at least [minGap] apart: in order along the line, a label takes the first row with room, and
 * the second of two that would clash drops to the next row (row 0 if neither has room).
 */
fun labelRows(positions: List<Float>, minGap: Float): List<Int> {
    val rows = IntArray(positions.size)
    val lastOnRow = floatArrayOf(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY)
    positions.indices.sortedBy { positions[it] }.forEach { i ->
        val p = positions[i]
        val row = when {
            p - lastOnRow[0] >= minGap -> 0
            p - lastOnRow[1] >= minGap -> 1
            else -> 0
        }
        rows[i] = row
        lastOnRow[row] = p
    }
    return rows.toList()
}

/** The sections of «الآن» and those coming up next, see [nowAndNext]. */
data class NowAndNext(val now: List<ProgramSection>, val next: List<ProgramSection>)

/**
 * [NowAndNext.now]: within the prayer time from [periodStart] to [periodEnd] (minutes of the day; it
 * may run past midnight), the timed sections whose time came last at [nowMinutes] — all of them when
 * several share that time. [NowAndNext.next]: the sections whose time comes first after [nowMinutes],
 * whatever prayer time they belong to (past midnight, tomorrow's first) — all of them when several
 * share it. Sections without a time are left out.
 */
fun nowAndNext(sections: List<ProgramSection>, periodStart: Int, periodEnd: Int, nowMinutes: Int): NowAndNext {
    fun offset(minutes: Int) = Math.floorMod(minutes - periodStart, MINUTES_PER_DAY)
    val now = offset(nowMinutes)
    val timed = sections.filter { it.reminderMinutes != null }.map { it to offset(it.reminderMinutes!!) }
    val due = timed.filter { (_, at) -> at <= now }
    val latest = due.maxOfOrNull { it.second }
    val current = due.filter { it.second == latest }.map { it.first }
    // The next to do, across the whole day: the soonest after now that is not already «now».
    val ahead = sections
        .filter { it.reminderMinutes != null && it !in current }
        .map { it to Math.floorMod(it.reminderMinutes!! - nowMinutes, MINUTES_PER_DAY) }
        .filter { (_, inMinutes) -> inMinutes > 0 }
    val soonest = ahead.minOfOrNull { it.second }
    return NowAndNext(now = current, next = ahead.filter { it.second == soonest }.map { it.first })
}
