package com.example.dhikra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CounterStoreTest {

    private val key = DailyCounter.key("istighfar")

    @Test
    fun countingAddsOneAndKeepsTheRestOfTheDay() {
        val progress = mapOf(key to 504, "section/morning" to 3)
        val counted = CounterStore.withDelta(progress, "istighfar", 1)
        assertEquals(505, counted[key])
        assertEquals(3, counted["section/morning"])
    }

    @Test
    fun aNewDayStartsFromNothing() {
        // The stored day is old: the repository hands back an empty day.
        assertEquals(1, CounterStore.withDelta(emptyMap(), "istighfar", 1)[key])
    }

    @Test
    fun undoingStopsAtZeroAndDropsTheCount() {
        assertEquals(503, CounterStore.withDelta(mapOf(key to 504), "istighfar", -1)[key])
        assertFalse(key in CounterStore.withDelta(mapOf(key to 1), "istighfar", -1))
        assertFalse(key in CounterStore.withDelta(emptyMap(), "istighfar", -1))
    }
}
