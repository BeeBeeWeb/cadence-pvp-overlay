package com.beebeeweb.cadence.core

import com.beebeeweb.cadence.core.TimingCalculator.ThrowPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TimingCalculatorTest {

    @Test
    fun twoTurnAgainstThreeTurn() {
        assertEquals(
            listOf(ThrowPoint(1, 1), ThrowPoint(4, 3), ThrowPoint(7, 5), ThrowPoint(10, 7)),
            TimingCalculator.throwPoints(2, 3),
        )
    }

    @Test
    fun twoTurnAgainstFiveTurn() {
        assertEquals(listOf(2, 7, 12, 17), TimingCalculator.throwPoints(2, 5).map { it.myFastMoves })
    }

    @Test
    fun threeTurnAgainstTwoTurnThrowsOnEveryOddMove() {
        assertEquals(listOf(1, 3, 5, 7), TimingCalculator.throwPoints(3, 2).map { it.myFastMoves })
    }

    @Test
    fun oneTurnAgainstThreeTurn() {
        assertEquals(listOf(2, 5, 8, 11), TimingCalculator.throwPoints(1, 3).map { it.myFastMoves })
    }

    @Test
    fun anythingGoesAgainstOneTurnMove() {
        assertEquals(listOf(1, 2, 3, 4), TimingCalculator.throwPoints(2, 1).map { it.myFastMoves })
    }

    @Test
    fun alignedMovesHaveNoOptimalThrow() {
        assertTrue(TimingCalculator.throwPoints(2, 4).isEmpty())
        assertTrue(TimingCalculator.isAligned(2, 4))
        assertTrue(TimingCalculator.isAligned(3, 3))
        assertFalse(TimingCalculator.isAligned(2, 3))
    }

    @Test
    fun firstReadyPicksFirstPointWithEnoughEnergy() {
        val points = TimingCalculator.throwPoints(2, 3)
        assertEquals(ThrowPoint(7, 5), TimingCalculator.firstReady(points, 5))
        assertEquals(ThrowPoint(1, 1), TimingCalculator.firstReady(points, 1))
        assertNull(TimingCalculator.firstReady(points, 11))
    }

    @Test
    fun rejectsInvalidTurns() {
        assertThrows(IllegalArgumentException::class.java) { TimingCalculator.throwPoints(0, 3) }
        assertThrows(IllegalArgumentException::class.java) { TimingCalculator.throwPoints(2, 0) }
    }
}
