package com.beebeeweb.cadence.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpponentNameReaderTest {

    private val matcher = SpeciesMatcher(listOf("Sneasel", "Combusken", "Alolan Marowak", "Marowak (Alolan)"))

    @Test
    fun `cp and name on separate lines`() {
        assertEquals("Sneasel", OpponentNameReader.pickSpecies(listOf("CP 1465", "Sneasel"), matcher))
    }

    @Test
    fun `cp and name merged into one line, either order`() {
        assertEquals("Sneasel", OpponentNameReader.pickSpecies(listOf("CP 1465 Sneasel"), matcher))
        assertEquals("Combusken", OpponentNameReader.pickSpecies(listOf("Combusken CP 1487"), matcher))
    }

    @Test
    fun `unrelated text lines are ignored`() {
        assertEquals("Sneasel", OpponentNameReader.pickSpecies(listOf("12:46", "20", "Sneasel"), matcher))
    }

    @Test
    fun `regional form names are matched`() {
        assertEquals("Alolan Marowak", OpponentNameReader.pickSpecies(listOf("CP 1490", "Alolan Marowak"), matcher))
    }

    @Test
    fun `nothing recognisable gives null`() {
        assertNull(OpponentNameReader.pickSpecies(listOf("CP 1465"), matcher))
        assertNull(OpponentNameReader.pickSpecies(emptyList(), matcher))
    }

    @Test
    fun `a misread letter is still matched`() {
        assertEquals("Sneasel", OpponentNameReader.pickSpecies(listOf("CP 1465", "Sneasei"), matcher))
    }
}
