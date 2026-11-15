package com.beebeeweb.cadence.core

/** The compact timing row shown under an opponent fast move. */
object TimingFormatter {

    const val CLOCK = "🕒" // 🕒

    /** "🕒 1 4 7 10" (your fast-move numbers to throw after), or "🕒 –" when the moves never line up. */
    fun row(myTurns: Int, theirTurns: Int, limit: Int = 4): String {
        val points = TimingCalculator.throwPoints(myTurns, theirTurns, limit)
        return if (points.isEmpty()) "$CLOCK –" else "$CLOCK " + points.joinToString(" ") { it.myFastMoves.toString() }
    }
}
