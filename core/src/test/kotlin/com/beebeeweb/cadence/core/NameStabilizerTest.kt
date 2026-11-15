package com.beebeeweb.cadence.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class NameStabilizerTest {

    @Test
    fun `a reading is confirmed after it repeats`() {
        val s = NameStabilizer(hitsToConfirm = 2)
        assertFalse(s.update("Sneasel"))
        assertNull(s.current)
        assertTrue(s.update("Sneasel"))
        assertEquals("Sneasel", s.current)
    }

    @Test
    fun `a single misread frame does not change the current species`() {
        val s = NameStabilizer(hitsToConfirm = 2)
        s.update("Sneasel"); s.update("Sneasel")
        assertFalse(s.update("Flareon"))
        assertFalse(s.update("Sneasel"))
        assertEquals("Sneasel", s.current)
    }

    @Test
    fun `a new species replaces the old one after enough hits`() {
        val s = NameStabilizer(hitsToConfirm = 2)
        s.update("Sneasel"); s.update("Sneasel")
        assertFalse(s.update("Combusken"))
        assertTrue(s.update("Combusken"))
        assertEquals("Combusken", s.current)
    }

    @Test
    fun `alternating readings never confirm`() {
        val s = NameStabilizer(hitsToConfirm = 2)
        repeat(5) { s.update("Sneasel"); s.update("Flareon") }
        assertNull(s.current)
    }

    @Test
    fun `a gap resets progress towards a new species`() {
        val s = NameStabilizer(hitsToConfirm = 2, missesToClear = 10)
        s.update("Sneasel"); s.update("Sneasel")
        s.update("Combusken"); s.update(null); s.update("Combusken")
        assertEquals("Sneasel", s.current)
    }

    @Test
    fun `current is cleared after enough consecutive misses`() {
        val s = NameStabilizer(hitsToConfirm = 2, missesToClear = 3)
        s.update("Sneasel"); s.update("Sneasel")
        assertFalse(s.update(null))
        assertFalse(s.update(null))
        assertTrue(s.update(null))
        assertNull(s.current)
        assertFalse(s.update(null)) // already clear, nothing changes
    }

    @Test
    fun `a reading in between resets the miss counter`() {
        val s = NameStabilizer(hitsToConfirm = 2, missesToClear = 3)
        s.update("Sneasel"); s.update("Sneasel")
        s.update(null); s.update(null)
        s.update("Sneasel")
        s.update(null); s.update(null)
        assertEquals("Sneasel", s.current)
    }

    @Test
    fun `settings must be at least one`() {
        assertThrows(IllegalArgumentException::class.java) { NameStabilizer(hitsToConfirm = 0) }
        assertThrows(IllegalArgumentException::class.java) { NameStabilizer(missesToClear = 0) }
    }
}
