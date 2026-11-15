package com.beebeeweb.cadence.core

import com.beebeeweb.cadence.core.Fixtures.BUBBLE
import com.beebeeweb.cadence.core.Fixtures.CHARGED_40
import com.beebeeweb.cadence.core.Fixtures.CHARGED_60
import com.beebeeweb.cadence.core.Fixtures.ICE_BEAM
import com.beebeeweb.cadence.core.Fixtures.THREE_TURN_FAST
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CountFormatterTest {

    private val sections = CountCalculator.sectionsFor(
        listOf(
            CountCalculator.countsFor(Moveset(BUBBLE, listOf(ICE_BEAM, CHARGED_60))),
            CountCalculator.countsFor(Moveset(BUBBLE, listOf(ICE_BEAM, CHARGED_40))),
        ),
    )

    private fun FormattedLine.highlighted(): String = text.substring(highlight!!.first, highlight!!.last + 1)

    @Test
    fun `fast move header appears once, then every charged move quickest first`() {
        val lines = CountFormatter.lines(sections)
        assertEquals(
            listOf(
                "Bubble (+11 / 3t)",
                "Fixture 40(40)   4x  12t",
                "Ice Beam(55)     5x  15t",
                "Fixture 60(60)   6x  18t",
            ),
            lines.map { it.text },
        )
    }

    @Test
    fun `only the fast move count is highlighted`() {
        val rows = CountFormatter.lines(sections).drop(1)
        assertEquals(listOf("4x", "5x", "6x"), rows.map { it.highlighted() })
    }

    @Test
    fun `header has no count highlight but is marked as a fast move`() {
        val lines = CountFormatter.lines(sections)
        assertNull(lines.first().highlight)
        assertEquals(listOf(true, false, false, false), lines.map { it.isFastMove })
    }

    @Test
    fun `charged moves show their energy cost in brackets`() {
        val rows = CountFormatter.lines(sections).drop(1)
        assertEquals(listOf("Fixture 40(40)", "Ice Beam(55)", "Fixture 60(60)"), rows.map { it.text.substring(0, 14).trimEnd() })
    }

    @Test
    fun `sections are separated by an empty line`() {
        val two = CountCalculator.sectionsFor(
            listOf(
                CountCalculator.countsFor(Moveset(BUBBLE, listOf(ICE_BEAM))),
                CountCalculator.countsFor(Moveset(THREE_TURN_FAST, listOf(CHARGED_40))),
            ),
        )
        assertEquals(
            listOf("Bubble (+11 / 3t)", "Ice Beam(55)   5x  15t", "", "Fixture Fast 3T (+10 / 3t)", "Fixture 40(40)   4x  12t"),
            CountFormatter.lines(two).map { it.text },
        )
    }

    @Test
    fun `no sections gives no lines`() {
        assertEquals(emptyList<FormattedLine>(), CountFormatter.lines(emptyList()))
    }
}
