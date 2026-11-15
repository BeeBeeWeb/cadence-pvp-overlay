package com.beebeeweb.cadence.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BacklinePredictorTest {

    @Test
    fun grassIsWeakToFireFlyingBugPoisonIce() {
        assertEquals(setOf("fire", "flying", "bug", "poison", "ice"), TypeChart.weaknesses(listOf("grass")).toSet())
    }

    @Test
    fun dualTypesCombineMultipliers() {
        assertTrue(TypeChart.multiplier("ice", listOf("grass", "flying")) > 2.0) // 1.6 * 1.6
        assertEquals(TypeChart.IMMUNE, TypeChart.multiplier("ground", listOf("flying")), 1e-9)
        assertTrue(TypeChart.weaknesses(listOf("water", "fairy")).containsAll(listOf("grass", "electric", "poison")))
    }

    @Test
    fun grassLeadPointsAtRock() {
        val types = BacklinePredictor.likelyTypes(listOf("grass"))
        assertEquals(3, types.size)
        assertEquals("rock", types.first()) // hits fire/flying/bug/ice and resists fire/flying/poison
    }

    @Test
    fun noTypesGivesNothing() {
        assertEquals(emptyList<String>(), BacklinePredictor.likelyTypes(emptyList()))
        assertNull(BacklinePredictor.label(emptyList()))
    }

    @Test
    fun labelCapitalisesTypes() {
        assertEquals("Back: Rock Fire", BacklinePredictor.label(listOf("rock", "fire")))
    }
}
