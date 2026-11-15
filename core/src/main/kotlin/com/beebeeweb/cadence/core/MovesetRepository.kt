package com.beebeeweb.cadence.core

/** Looks up species by name or alias and returns their most likely movesets with counts. */
class MovesetRepository(species: List<Species>) {

    private val byName: Map<String, Species> = buildMap {
        // Real names are indexed before aliases so an alias never shadows another species' name.
        // On duplicates the first entry wins.
        for (s in species) putIfAbsent(Names.normalize(s.name), s)
        for (s in species) for (alias in s.aliases) putIfAbsent(Names.normalize(alias), s)
    }

    /** Every name and alias we can resolve, for [SpeciesMatcher]. */
    val speciesNames: List<String> = species.flatMap { listOf(it.name) + it.aliases }

    fun find(name: String): Species? = byName[Names.normalize(name)]

    /** Up to [limit] movesets for [name], most likely first. Empty if the species is unknown. */
    fun topMovesets(name: String, limit: Int = 3): List<MovesetCounts> {
        require(limit >= 0) { "limit must not be negative" }
        val species = find(name) ?: return emptyList()
        return species.movesets.take(limit).map(CountCalculator::countsFor)
    }
}
