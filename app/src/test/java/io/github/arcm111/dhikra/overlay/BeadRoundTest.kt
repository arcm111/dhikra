package io.github.arcm111.dhikra.overlay

import org.junit.Assert.assertEquals
import org.junit.Test

class BeadRoundTest {

    private fun round(count: Int) = OverlayCounterView.roundOf(count)

    @Test
    fun noBeadBeforeTheFirstCount() = assertEquals(0, round(0))

    @Test
    fun theThirtyThirdCountLightsEveryBead() {
        assertEquals(1, round(1))
        assertEquals(33, round(33))
    }

    @Test
    fun aNewRoundStartsAgainAtOne() {
        assertEquals(1, round(34))
        assertEquals(33, round(66))
        assertEquals(1, round(67))
        // ٧١٤ = 21 rounds of 33 and 21 more.
        assertEquals(21, round(714))
    }
}
