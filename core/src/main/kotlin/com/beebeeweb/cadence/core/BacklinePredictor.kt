package com.beebeeweb.cadence.core

/**
 * Guesses which types the opponent's other Pokémon are likely to have, from the lead's weaknesses:
 * a team builder covers those weaknesses with types that beat or resist what hits the lead.
 */
object BacklinePredictor {

    /**
     * The [limit] best single types against the types that hit [leadTypes] super effectively.
     *
     * A type scores +1 for every of those attacking types it hits super effectively, +1 for every one
     * it resists (+1.5 if immune), and -1 for every one it is weak to. Ties go to the earlier type in
     * [TypeChart.TYPES]. This is a rough guess, not a prediction of the actual team.
     */
    fun likelyTypes(leadTypes: List<String>, limit: Int = 3): List<String> {
        if (leadTypes.isEmpty()) return emptyList()
        val attackers = TypeChart.weaknesses(leadTypes)
        if (attackers.isEmpty()) return emptyList()
        return TypeChart.TYPES
            .map { candidate -> candidate to score(candidate, attackers) }
            .filter { it.second > 0.0 }
            .sortedByDescending { it.second } // stable: ties keep chart order
            .take(limit)
            .map { it.first }
    }

    private fun score(candidate: String, attackers: List<String>): Double =
        attackers.sumOf { attacker ->
            var points = 0.0
            if (TypeChart.multiplier(candidate, listOf(attacker)) > 1.0) points += 1.0
            when {
                TypeChart.multiplier(attacker, listOf(candidate)) >= TypeChart.SUPER_EFFECTIVE -> points -= 1.0
                TypeChart.multiplier(attacker, listOf(candidate)) <= TypeChart.IMMUNE -> points += 1.5
                TypeChart.multiplier(attacker, listOf(candidate)) < 1.0 -> points += 1.0
            }
            points
        }

    /** "Back: Rock Fire Steel", or null when there is nothing to show. */
    fun label(types: List<String>): String? =
        if (types.isEmpty()) null else "Back: " + types.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
}
