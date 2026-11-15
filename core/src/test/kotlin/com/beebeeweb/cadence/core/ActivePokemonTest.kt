package com.beebeeweb.cadence.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActivePokemonTest {

    private val names = mapOf(
        "ACROBATICS" to "Acrobatics",
        "ENERGY_BALL" to "Energy Ball",
        "ICE_BEAM" to "Ice Beam",
        "PLAY_ROUGH" to "Play Rough",
        "HURRICANE" to "Hurricane",
    )

    private val team = listOf(
        TeamMember("Jumpluff", "BULLET_SEED", listOf("ACROBATICS", "ENERGY_BALL")),
        TeamMember("Azumarill", "BUBBLE", listOf("ICE_BEAM", "PLAY_ROUGH")),
        null,
    )

    @Test
    fun matchesBothButtonLabels() {
        assertEquals(0, ActivePokemon.pick(listOf("ACROBATICS", "SUPER EFFECTIVE", "ENERGY BALL"), team, names))
    }

    @Test
    fun oneReadableLabelIsEnough() {
        assertEquals(1, ActivePokemon.pick(listOf("ICE BEAM NOT VERY EFFECTIVE"), team, names))
    }

    @Test
    fun noMatchGivesNull() {
        assertNull(ActivePokemon.pick(listOf("Go, Flareon!"), team, names))
        assertNull(ActivePokemon.pick(emptyList(), team, names))
    }

    @Test
    fun tiesGiveNull() {
        val shared = listOf(
            TeamMember("A", "X", listOf("ICE_BEAM")),
            TeamMember("B", "X", listOf("ICE_BEAM")),
            null,
        )
        assertNull(ActivePokemon.pick(listOf("ICE BEAM"), shared, names))
    }
}
