package com.example.dhikra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReadingSpeedsTest {

    @Test
    fun aRepetitionMovesTheSpeedHalfWayAndTheDefaultAQuarter() {
        // 20 letters in 2 s: 10 letters a second, from 6 (own) and 8 (default).
        val learned = ReadingSpeeds.learn(itemSpeed = 6f, defaultSpeed = 8f, letters = 20, elapsedMs = 2_000)!!
        assertEquals(8f, learned.item, 0.001f)
        assertEquals(8.5f, learned.default, 0.001f)
    }

    @Test
    fun aSlipOfTheFingerTeachesNothing() {
        assertNull(ReadingSpeeds.learn(7f, 7f, letters = 20, elapsedMs = ReadingSpeeds.MIN_MEASURABLE_MS - 1))
        assertNull(ReadingSpeeds.learn(7f, 7f, letters = 0, elapsedMs = 2_000))
    }

    @Test
    fun anImpossiblePaceIsHeldToTheLimits() {
        val learned = ReadingSpeeds.learn(7f, 7f, letters = 1_000, elapsedMs = 1_000)!!
        assertEquals((7f + ProgramRepository.MAX_LETTERS_PER_SECOND) / 2, learned.item, 0.001f)
    }

    @Test
    fun aCounterKeepsItsOwnSpeed() {
        assertEquals("counter/istighfar", ReadingSpeeds.counterKey("istighfar"))
    }
}
