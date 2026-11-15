package com.beebeeweb.cadence.core

/**
 * Move-count maths.
 *
 * Counts assume the Pokémon starts at zero energy. [MoveCount.repeatFastMoves] also counts the
 * energy left over after throwing the same move once.
 */
object CountCalculator {

    /** Fast moves needed to afford [charged], and the turns those fast moves take. */
    fun count(fast: FastMove, charged: ChargedMove): MoveCount {
        val fastMoves = (charged.energyCost + fast.energyGain - 1) / fast.energyGain // round up
        // Energy left after the first throw gives a head start on the next one.
        val leftover = fastMoves * fast.energyGain - charged.energyCost
        val repeat = (charged.energyCost - leftover + fast.energyGain - 1) / fast.energyGain
        return MoveCount(charged, fastMoves, fastMoves * fast.turns, repeat)
    }

    /** Counts for every charged move in [moveset], quickest to reach first (ties keep input order). */
    fun countsFor(moveset: Moveset): MovesetCounts =
        MovesetCounts(
            fast = moveset.fast,
            counts = moveset.charged
                .map { count(moveset.fast, it) }
                .sortedBy { it.fastMoves },
        )

    /**
     * Merges movesets that share a fast move into one section, so the fast move is listed once with
     * every charged move seen alongside it. Sections keep the order of their first appearance (most
     * likely moveset first); charged moves are listed quickest to reach first, without duplicates.
     */
    fun sectionsFor(movesets: List<MovesetCounts>): List<FastMoveSection> {
        val byFast = LinkedHashMap<String, Pair<FastMove, LinkedHashMap<String, MoveCount>>>()
        for (moveset in movesets) {
            val (_, charged) = byFast.getOrPut(moveset.fast.id) { moveset.fast to LinkedHashMap() }
            for (count in moveset.counts) charged.putIfAbsent(count.move.id, count)
        }
        return byFast.values.map { (fast, charged) ->
            FastMoveSection(fast, charged.values.sortedBy { it.fastMoves })
        }
    }
}
