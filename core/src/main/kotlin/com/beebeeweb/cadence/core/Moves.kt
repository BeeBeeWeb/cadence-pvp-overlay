package com.beebeeweb.cadence.core

/**
 * A fast move. Every time it completes it gains [energyGain] energy and it takes [turns] turns
 * to complete (1 turn = 0.5 s in trainer battles).
 */
data class FastMove(
    val id: String,
    val name: String,
    val energyGain: Int,
    val turns: Int,
) {
    init {
        require(energyGain > 0) { "Fast move $id must gain energy (got $energyGain)" }
        require(turns > 0) { "Fast move $id must last at least one turn (got $turns)" }
    }
}

/** A charged move that costs [energyCost] energy to throw. */
data class ChargedMove(
    val id: String,
    val name: String,
    val energyCost: Int,
) {
    init {
        require(energyCost > 0) { "Charged move $id must cost energy (got $energyCost)" }
    }
}

/** One fast move plus the charged moves a Pokémon can run alongside it. */
data class Moveset(
    val fast: FastMove,
    val charged: List<ChargedMove>,
)

/**
 * A species and its movesets, ordered from most to least likely.
 * [aliases] are other names the game or data sources use for it (e.g. "Marowak (Alolan)" for "Alolan Marowak").
 */
data class Species(
    val id: String,
    val name: String,
    val movesets: List<Moveset>,
    val aliases: List<String> = emptyList(),
)

/**
 * How long it takes to reach one charged move from zero energy:
 * [fastMoves] fast moves, which take [turns] turns in total.
 */
data class MoveCount(
    val move: ChargedMove,
    val fastMoves: Int,
    val turns: Int,
)

/** All charged-move counts for one moveset, fastest to reach first. */
data class MovesetCounts(
    val fast: FastMove,
    val counts: List<MoveCount>,
)

/** One fast move with every charged move it was seen with across several movesets, quickest first. */
data class FastMoveSection(
    val fast: FastMove,
    val counts: List<MoveCount>,
)
