package io.github.arcm111.dhikra.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import kotlin.random.Random

/** When the random reminders come: within the day's hours, spread out, the next day once over. */
class InspirationPlanTest {

    private fun at(hour: Int, minute: Int = 0): Calendar = Calendar.getInstance().apply {
        set(2026, Calendar.NOVEMBER, 17, hour, minute, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun minuteOfDay(millis: Long): Int = Calendar.getInstance().apply { timeInMillis = millis }
        .let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }

    private fun dayOf(millis: Long): Int = Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.DAY_OF_YEAR)

    private val settings = InspirationReminders.Settings(perDay = 4, fromMinutes = 8 * 60, toMinutes = 20 * 60)

    @Test
    fun beforeTheHours_theDaysRemindersFallWithinThem_inOrder() {
        repeat(50) { seed ->
            val times = InspirationReminders.plan(at(6), settings, Random(seed))
            assertEquals(4, times.size)
            assertEquals(times.sorted(), times)
            assertTrue(times.all { minuteOfDay(it) in 8 * 60 until 20 * 60 && dayOf(it) == dayOf(at(6).timeInMillis) })
        }
    }

    @Test
    fun theReminders_areSpread_oneInEachSliceOfTheHours() {
        val times = InspirationReminders.plan(at(6), settings, Random(7)).map(::minuteOfDay)
        // Twelve hours, four reminders: one in each three hours.
        times.forEachIndexed { i, minute -> assertTrue(minute in (8 + 3 * i) * 60 until (8 + 3 * (i + 1)) * 60) }
    }

    @Test
    fun duringTheHours_onlyWhatIsLeftOfThemIsUsed() {
        val now = at(15)
        val times = InspirationReminders.plan(now, settings, Random(3))
        assertTrue(times.all { it > now.timeInMillis && minuteOfDay(it) < 20 * 60 })
    }

    @Test
    fun afterTheHours_theNextDaysAreUsed() {
        val now = at(21)
        val times = InspirationReminders.plan(now, settings, Random(3))
        assertTrue(times.all { dayOf(it) == dayOf(now.timeInMillis) + 1 && minuteOfDay(it) in 8 * 60 until 20 * 60 })
    }
}
