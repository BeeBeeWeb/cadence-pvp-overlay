package com.beebeeweb.cadence.core

/**
 * Move-count maths.
 *
 * Counts assume the Pokémon starts at zero energy. Energy left over after a charged move
 * is not tracked yet.
 */
object CountCalculator {

    /** Fast moves needed to afford [charged], and the turns those fast moves take. */
    fun count(fast: FastMove, charged: ChargedMove): MoveCount {
        val fastMoves = (charged.energyCost + fast.energyGain - 1) / fast.energyGain // round up
        return MoveCount(charged, fastMoves, fastMoves * fast.turns)
    }

    /** Counts for every charged move in [moveset], quickest to reach first (ties keep input order). */
    fun countsFor(moveset: Moveset): MovesetCounts =
        MovesetCounts(
            fast = moveset.fast,
            counts = moveset.charged
                .map { count(moveset.fast, it) }
                .sortedBy { it.fastMoves },
        )
}
