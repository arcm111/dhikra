package com.example.dhikra.ui.today

import com.example.dhikra.data.Prayer
import com.example.dhikra.data.PrayerTimetable
import com.example.dhikra.data.ProgramSection

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

/** The day at a glance: its prayer [periods] in order, and the sections with no time ([untimed]). */
data class DayTimeline(val periods: List<DayPeriod>, val untimed: List<ProgramSection>) {
    /** Position of the period the current time falls in. */
    val currentIndex: Int get() = periods.indexOfFirst { it.current }

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
