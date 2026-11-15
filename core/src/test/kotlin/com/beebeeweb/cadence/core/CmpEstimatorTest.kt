package com.beebeeweb.cadence.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CmpEstimatorTest {

    @Test
    fun clearDifferencesPickAWinner() {
        assertEquals(CmpResult.YOU, CmpEstimator.compare(150, 120))
        assertEquals(CmpResult.THEM, CmpEstimator.compare(100, 130))
    }

    @Test
    fun smallDifferencesAreClose() {
        assertEquals(CmpResult.CLOSE, CmpEstimator.compare(112, 118))
        assertEquals(CmpResult.CLOSE, CmpEstimator.compare(120, 110)) // exactly at the limit
        assertEquals(CmpResult.CLOSE, CmpEstimator.compare(120, 120))
    }

    @Test
    fun missingStatsAreUnknown() {
        assertEquals(CmpResult.UNKNOWN, CmpEstimator.compare(0, 120))
        assertEquals(CmpResult.UNKNOWN, CmpEstimator.compare(120, 0))
        assertNull(CmpEstimator.label(CmpResult.UNKNOWN))
    }

    @Test
    fun labelsAreShort() {
        assertEquals("CMP: you", CmpEstimator.label(CmpResult.YOU))
        assertEquals("CMP: them", CmpEstimator.label(CmpResult.THEM))
        assertEquals("CMP: close", CmpEstimator.label(CmpResult.CLOSE))
    }
}
