package com.beebeeweb.cadence.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpeciesMatcherTest {

    private val matcher = SpeciesMatcher(
        listOf("Sneasel", "Combusken", "Magcargo", "Flareon", "Azumarill", "Mr. Mime", "Flabébé", "Mew", "Meowth"),
    )

    @Test
    fun `exact name matches ignoring case and spacing`() {
        assertEquals("Combusken", matcher.match("  combusken "))
    }

    @Test
    fun `punctuation and accents are ignored`() {
        assertEquals("Mr. Mime", matcher.match("Mr Mime"))
        assertEquals("Flabébé", matcher.match("Flabebe"))
    }

    @Test
    fun `one misread character is corrected`() {
        assertEquals("Sneasel", matcher.match("Sneasei")) // l read as i
        assertEquals("Magcargo", matcher.match("Magcarqo")) // g read as q
    }

    @Test
    fun `two misread characters are tolerated on long names only`() {
        assertEquals("Azumarill", matcher.match("Azumarii1")) // both l's misread (i and 1)
        assertNull(matcher.match("Sneasxi")) // two misreads on a 7-letter name is too many
    }

    @Test
    fun `short names must match exactly`() {
        assertEquals("Mew", matcher.match("Mew"))
        assertNull(matcher.match("Mow"))
    }

    @Test
    fun `text that is too far from every name returns null`() {
        assertNull(matcher.match("Charizard"))
        assertNull(matcher.match("CP 1471"))
    }

    @Test
    fun `empty or symbol-only text returns null`() {
        assertNull(matcher.match(""))
        assertNull(matcher.match(" -- "))
    }

    @Test
    fun `ambiguous reads return null instead of guessing`() {
        val ambiguous = SpeciesMatcher(listOf("Cradily", "Cradilx"))
        assertNull(ambiguous.match("Cradilz"))
    }
}
