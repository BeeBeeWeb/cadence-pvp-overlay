package com.beebeeweb.cadence.core

import com.beebeeweb.cadence.core.Fixtures.BUBBLE
import com.beebeeweb.cadence.core.Fixtures.CHARGED_60
import com.beebeeweb.cadence.core.Fixtures.ICE_BEAM
import org.junit.Assert.assertEquals
import org.junit.Test

class CountFormatterTest {

    private val counts = CountCalculator.countsFor(Moveset(BUBBLE, listOf(ICE_BEAM, CHARGED_60)))

    @Test
    fun `lines show count and turns per charged move`() {
        assertEquals(listOf("Ice Beam  5x  15t", "Fixture 60  6x  18t"), CountFormatter.lines(counts))
    }

    @Test
    fun `format adds a fast move header`() {
        assertEquals(
            "Bubble (+11 / 3t)\nIce Beam  5x  15t\nFixture 60  6x  18t",
            CountFormatter.format(counts),
        )
    }
}
