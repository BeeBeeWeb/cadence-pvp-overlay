package com.beebeeweb.cadence.core

/**
 * Optimal charged-move throw timing.
 *
 * A fast move registers on the last turn of its animation. Throwing a charged move interrupts
 * whatever the opponent is doing, so the best moment is the last turn of the *opponent's* fast move:
 * they have already finished it and get no free turns.
 *
 * If your fast move lasts [a] turns and theirs [b], your n-th fast move completes on turn n*a and you
 * throw on turn n*a + 1. That is optimal when n*a + 1 is a multiple of [b]. The pattern only holds
 * from the last reset (battle start, a charged move or a switch), because those reset both cooldowns.
 */
object TimingCalculator {

    /** One optimal throw point: throw after your [myFastMoves]-th fast move, when they have done [theirFastMoves]. */
    data class ThrowPoint(val myFastMoves: Int, val theirFastMoves: Int)

    /**
     * The first [limit] optimal throw points, in order. Empty when the move lengths never line up
     * (for example 2 vs 4 turns), because then every throw gives the opponent a free turn.
     */
    fun throwPoints(myTurns: Int, theirTurns: Int, limit: Int = 4): List<ThrowPoint> {
        require(myTurns > 0 && theirTurns > 0) { "Fast moves last at least one turn" }
        require(limit >= 0) { "limit must not be negative" }
        val points = ArrayList<ThrowPoint>()
        // n*a+1 mod b repeats every b moves, so b*limit moves are enough to find `limit` points (if any exist)
        for (n in 1..theirTurns * limit) {
            val turn = n * myTurns + 1
            if (turn % theirTurns == 0) {
                points += ThrowPoint(n, turn / theirTurns)
                if (points.size == limit) break
            }
        }
        return points
    }

    /** True if no throw point exists for these move lengths. */
    fun isAligned(myTurns: Int, theirTurns: Int): Boolean = throwPoints(myTurns, theirTurns, 1).isEmpty()

    /** The first throw point at which you also have the energy for a move that needs [count] fast moves. */
    fun firstReady(points: List<ThrowPoint>, count: Int): ThrowPoint? = points.firstOrNull { it.myFastMoves >= count }
}
