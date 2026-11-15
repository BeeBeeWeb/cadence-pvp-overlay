package com.beebeeweb.cadence.core

/**
 * Works out which of your Pokémon is out, from the charged-move names on the bottom buttons
 * (for example "ACROBATICS" and "ENERGY BALL").
 */
object ActivePokemon {

    /** Shortest move name we trust inside a longer text line, to avoid matching stray letters. */
    private const val MIN_NAME_LENGTH = 4

    /**
     * Index in [team] of the Pokémon whose charged moves best match [lines], or null when nothing
     * matches or two Pokémon match equally well. [chargedNames] maps a charged move id to its display name.
     */
    fun pick(lines: List<String>, team: List<TeamMember?>, chargedNames: Map<String, String>): Int? {
        val seen = lines.map { Names.normalize(it) }.filter { it.isNotEmpty() }
        if (seen.isEmpty()) return null

        val scores = team.map { member ->
            member?.chargedIds.orEmpty().count { id ->
                val name = chargedNames[id]?.let { Names.normalize(it) }
                name != null && name.length >= MIN_NAME_LENGTH && seen.any { it.contains(name) }
            }
        }
        val best = scores.maxOrNull() ?: return null
        if (best == 0 || scores.count { it == best } != 1) return null
        return scores.indexOf(best)
    }
}
