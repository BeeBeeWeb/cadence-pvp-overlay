package com.beebeeweb.cadence.core

import org.junit.Assert.assertEquals
import org.junit.Test

class TimingFormatterTest {

    @Test
    fun listsThrowPoints() {
        assertEquals("🕒 1 4 7 10", TimingFormatter.row(2, 3))
        assertEquals("🕒 2 7 12 17", TimingFormatter.row(2, 5))
    }

    @Test
    fun showsDashWhenMovesAlign() {
        assertEquals("🕒 –", TimingFormatter.row(2, 4))
    }
}
