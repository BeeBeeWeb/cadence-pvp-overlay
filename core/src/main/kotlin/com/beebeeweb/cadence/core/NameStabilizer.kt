package com.beebeeweb.cadence.core

/**
 * Turns a noisy stream of per-frame readings into a steady "current opponent".
 *
 * A reading must repeat [hitsToConfirm] times in a row before it replaces the current species, so a
 * single misread frame never flickers the overlay. After [missesToClear] consecutive frames with no
 * reading (battle over, card faded for a long time) the current species is cleared.
 *
 * Not thread-safe: feed it from one thread.
 */
class NameStabilizer(
    private val hitsToConfirm: Int = 2,
    private val missesToClear: Int = 15,
) {
    init {
        require(hitsToConfirm >= 1) { "hitsToConfirm must be at least 1" }
        require(missesToClear >= 1) { "missesToClear must be at least 1" }
    }

    /** The confirmed species, or null if there is none. */
    var current: String? = null
        private set

    private var candidate: String? = null
    private var candidateHits = 0
    private var misses = 0

    /** Feed one frame's reading (null = nothing recognised). Returns true if [current] changed. */
    fun update(reading: String?): Boolean {
        if (reading == null) {
            candidate = null
            candidateHits = 0
            misses++
            if (misses >= missesToClear && current != null) {
                current = null
                return true
            }
            return false
        }

        misses = 0
        if (reading == current) {
            candidate = null
            candidateHits = 0
            return false
        }
        if (reading == candidate) candidateHits++ else {
            candidate = reading
            candidateHits = 1
        }
        if (candidateHits >= hitsToConfirm) {
            current = reading
            candidate = null
            candidateHits = 0
            return true
        }
        return false
    }
}
