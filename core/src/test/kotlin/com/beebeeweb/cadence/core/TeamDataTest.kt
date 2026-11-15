package com.beebeeweb.cadence.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TeamDataTest {

    @Test
    fun roundTripsAFilledAndEmptySlots() {
        val team = listOf(
            TeamMember("Azumarill", "BUBBLE", listOf("ICE_BEAM", "PLAY_ROUGH")),
            null,
            TeamMember("Medicham", "COUNTER", listOf("ICE_PUNCH")),
        )
        assertEquals(team, TeamData.fromJson(TeamData.toJson(team)))
    }

    @Test
    fun missingOrBrokenDataGivesEmptySlots() {
        val empty = List<TeamMember?>(TeamData.SLOTS) { null }
        assertEquals(empty, TeamData.fromJson(null))
        assertEquals(empty, TeamData.fromJson(""))
        assertEquals(empty, TeamData.fromJson("not json"))
        assertNull(TeamData.fromJson("[{\"species\":\"X\"}]")[0])
    }
}
