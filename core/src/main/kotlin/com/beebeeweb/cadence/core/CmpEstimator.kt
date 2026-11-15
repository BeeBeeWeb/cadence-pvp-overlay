package com.beebeeweb.cadence.core

/** Who is expected to go first when both sides throw a charged move on the same turn. */
enum class CmpResult { YOU, THEM, CLOSE, UNKNOWN }

/**
 * First, rough charged move priority (CMP) estimate from base Attack stats only.
 *
 * The game compares the full Attack stat (base stat plus IVs, times the level multiplier), which we
 * cannot see for the opponent. Base Attack differences of up to [CLOSE_WITHIN] points can be flipped
 * by IVs and level, so those are reported as [CmpResult.CLOSE] instead of a winner.
 */
object CmpEstimator {

    const val CLOSE_WITHIN = 10

    fun compare(myBaseAttack: Int, theirBaseAttack: Int, closeWithin: Int = CLOSE_WITHIN): CmpResult {
        if (myBaseAttack <= 0 || theirBaseAttack <= 0) return CmpResult.UNKNOWN
        val difference = myBaseAttack - theirBaseAttack
        return when {
            difference > closeWithin -> CmpResult.YOU
            difference < -closeWithin -> CmpResult.THEM
            else -> CmpResult.CLOSE
        }
    }

    /** Short overlay text, or null when nothing is known. */
    fun label(result: CmpResult): String? = when (result) {
        CmpResult.YOU -> "CMP: you"
        CmpResult.THEM -> "CMP: them"
        CmpResult.CLOSE -> "CMP: close"
        CmpResult.UNKNOWN -> null
    }
}
