package com.beebeeweb.cadence.core

/** Looks up species by name and returns their most likely movesets with counts. */
class MovesetRepository(species: List<Species>) {

    private val byName: Map<String, Species> = buildMap {
        for (s in species) putIfAbsent(Names.normalize(s.name), s) // first entry wins on duplicates
    }

    /** Display names of every known species, for [SpeciesMatcher]. */
    val speciesNames: List<String> = species.map { it.name }

    fun find(name: String): Species? = byName[Names.normalize(name)]

    /** Up to [limit] movesets for [name], most likely first. Empty if the species is unknown. */
    fun topMovesets(name: String, limit: Int = 3): List<MovesetCounts> {
        require(limit >= 0) { "limit must not be negative" }
        val species = find(name) ?: return emptyList()
        return species.movesets.take(limit).map(CountCalculator::countsFor)
    }
}
