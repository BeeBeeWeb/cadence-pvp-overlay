package com.beebeeweb.cadence.core

/** Type effectiveness (the standard chart; Pokémon GO uses 1.6x, 0.625x and 0.39x for the same cases). */
object TypeChart {

    val TYPES = listOf(
        "normal", "fighting", "flying", "poison", "ground", "rock", "bug", "ghost", "steel",
        "fire", "water", "grass", "electric", "psychic", "ice", "dragon", "dark", "fairy",
    )

    private class Row(val superEffective: List<String>, val notVery: List<String>, val immune: List<String> = emptyList())

    private val chart: Map<String, Row> = mapOf(
        "normal" to Row(emptyList(), listOf("rock", "steel"), listOf("ghost")),
        "fighting" to Row(listOf("normal", "ice", "rock", "dark", "steel"), listOf("flying", "poison", "bug", "psychic", "fairy"), listOf("ghost")),
        "flying" to Row(listOf("fighting", "bug", "grass"), listOf("rock", "steel", "electric")),
        "poison" to Row(listOf("grass", "fairy"), listOf("poison", "ground", "rock", "ghost"), listOf("steel")),
        "ground" to Row(listOf("poison", "rock", "steel", "fire", "electric"), listOf("bug", "grass"), listOf("flying")),
        "rock" to Row(listOf("flying", "bug", "fire", "ice"), listOf("fighting", "ground", "steel")),
        "bug" to Row(listOf("grass", "psychic", "dark"), listOf("fighting", "flying", "poison", "ghost", "steel", "fire", "fairy")),
        "ghost" to Row(listOf("ghost", "psychic"), listOf("dark"), listOf("normal")),
        "steel" to Row(listOf("rock", "ice", "fairy"), listOf("steel", "fire", "water", "electric")),
        "fire" to Row(listOf("bug", "steel", "grass", "ice"), listOf("rock", "fire", "water", "dragon")),
        "water" to Row(listOf("ground", "rock", "fire"), listOf("water", "grass", "dragon")),
        "grass" to Row(listOf("ground", "rock", "water"), listOf("flying", "poison", "bug", "steel", "fire", "grass", "dragon")),
        "electric" to Row(listOf("flying", "water"), listOf("grass", "electric", "dragon"), listOf("ground")),
        "psychic" to Row(listOf("fighting", "poison"), listOf("steel", "psychic"), listOf("dark")),
        "ice" to Row(listOf("flying", "ground", "grass", "dragon"), listOf("steel", "fire", "water", "ice")),
        "dragon" to Row(listOf("dragon"), listOf("steel"), listOf("fairy")),
        "dark" to Row(listOf("ghost", "psychic"), listOf("fighting", "dark", "fairy")),
        "fairy" to Row(listOf("fighting", "dragon", "dark"), listOf("poison", "steel", "fire")),
    )

    /** Damage multiplier of one attacking type against a Pokémon with [defenderTypes]. */
    fun multiplier(attacker: String, defenderTypes: List<String>): Double {
        val row = chart.getValue(attacker)
        return defenderTypes.fold(1.0) { acc, defender ->
            acc * when (defender) {
                in row.immune -> IMMUNE
                in row.superEffective -> SUPER_EFFECTIVE
                in row.notVery -> NOT_VERY_EFFECTIVE
                else -> 1.0
            }
        }
    }

    /** The attacking types that hit a Pokémon with [defenderTypes] super effectively. */
    fun weaknesses(defenderTypes: List<String>): List<String> =
        TYPES.filter { multiplier(it, defenderTypes) > 1.0 }

    const val SUPER_EFFECTIVE = 1.6
    const val NOT_VERY_EFFECTIVE = 0.625
    const val IMMUNE = 0.390625
}
