package com.beebeeweb.cadence.core

import com.beebeeweb.cadence.core.Fixtures.BUBBLE
import com.beebeeweb.cadence.core.Fixtures.CHARGED_32
import com.beebeeweb.cadence.core.Fixtures.CHARGED_40
import com.beebeeweb.cadence.core.Fixtures.CHARGED_60
import com.beebeeweb.cadence.core.Fixtures.ICE_BEAM
import com.beebeeweb.cadence.core.Fixtures.THREE_TURN_FAST
import com.beebeeweb.cadence.core.Fixtures.TWO_TURN_FAST
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CountCalculatorTest {

    @Test
    fun `bubble reaches ice beam in five moves and fifteen turns`() {
        val result = CountCalculator.count(BUBBLE, ICE_BEAM)
        assertEquals(5, result.fastMoves)
        assertEquals(15, result.turns)
    }

    @Test
    fun `four two-turn fast moves take eight turns`() {
        val result = CountCalculator.count(TWO_TURN_FAST, CHARGED_32)
        assertEquals(4, result.fastMoves)
        assertEquals(8, result.turns)
    }

    @Test
    fun `four three-turn fast moves take twelve turns`() {
        val result = CountCalculator.count(THREE_TURN_FAST, CHARGED_40)
        assertEquals(4, result.fastMoves)
        assertEquals(12, result.turns)
    }

    @Test
    fun `count rounds up when energy does not divide evenly`() {
        val result = CountCalculator.count(BUBBLE, CHARGED_60) // 60 / 11 = 5.45
        assertEquals(6, result.fastMoves)
        assertEquals(18, result.turns)
    }

    @Test
    fun `counts are listed quickest first and keep input order on ties`() {
        val moveset = Moveset(BUBBLE, listOf(CHARGED_60, ICE_BEAM))
        val counts = CountCalculator.countsFor(moveset).counts
        assertEquals(listOf("Ice Beam", "Fixture 60"), counts.map { it.move.name })
    }

    @Test
    fun `fast move must gain energy`() {
        assertThrows(IllegalArgumentException::class.java) { FastMove("X", "X", energyGain = 0, turns = 2) }
    }

    @Test
    fun `fast move must take at least one turn`() {
        assertThrows(IllegalArgumentException::class.java) { FastMove("X", "X", energyGain = 5, turns = 0) }
    }

    @Test
    fun `charged move must cost energy`() {
        assertThrows(IllegalArgumentException::class.java) { ChargedMove("X", "X", energyCost = 0) }
    }

    @Test
    fun `movesets sharing a fast move merge into one section without duplicate charged moves`() {
        val sections = CountCalculator.sectionsFor(
            listOf(
                CountCalculator.countsFor(Moveset(BUBBLE, listOf(ICE_BEAM, CHARGED_60))),
                CountCalculator.countsFor(Moveset(BUBBLE, listOf(ICE_BEAM, CHARGED_40))),
            ),
        )
        assertEquals(1, sections.size)
        assertEquals(listOf("Fixture 40", "Ice Beam", "Fixture 60"), sections.single().counts.map { it.move.name })
    }

    @Test
    fun `different fast moves stay separate in order of first appearance`() {
        val sections = CountCalculator.sectionsFor(
            listOf(
                CountCalculator.countsFor(Moveset(THREE_TURN_FAST, listOf(CHARGED_40))),
                CountCalculator.countsFor(Moveset(BUBBLE, listOf(ICE_BEAM))),
                CountCalculator.countsFor(Moveset(THREE_TURN_FAST, listOf(CHARGED_60))),
            ),
        )
        assertEquals(listOf("Fixture Fast 3T", "Bubble"), sections.map { it.fast.name })
        assertEquals(listOf("Fixture 40", "Fixture 60"), sections[0].counts.map { it.move.name })
    }

    @Test
    fun `no movesets give no sections`() {
        assertEquals(emptyList<FastMoveSection>(), CountCalculator.sectionsFor(emptyList()))
    }
}
