package com.beebeeweb.cadence.core

/**
 * Test fixtures. Bubble (+11 energy, 3 turns) and Ice Beam (55 energy) are the figures
 * confirmed by the project owner. The "FIXTURE" moves are made up to exercise the maths.
 */
internal object Fixtures {
    val BUBBLE = FastMove("BUBBLE", "Bubble", energyGain = 11, turns = 3)
    val ICE_BEAM = ChargedMove("ICE_BEAM", "Ice Beam", energyCost = 55)

    val TWO_TURN_FAST = FastMove("FIXTURE_FAST_2T", "Fixture Fast 2T", energyGain = 8, turns = 2)
    val THREE_TURN_FAST = FastMove("FIXTURE_FAST_3T", "Fixture Fast 3T", energyGain = 10, turns = 3)

    val CHARGED_32 = ChargedMove("FIXTURE_32", "Fixture 32", energyCost = 32)
    val CHARGED_40 = ChargedMove("FIXTURE_40", "Fixture 40", energyCost = 40)
    val CHARGED_60 = ChargedMove("FIXTURE_60", "Fixture 60", energyCost = 60)
}
