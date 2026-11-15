package com.beebeeweb.cadence.core

/** Turns counts into the short text shown in the overlay. */
object CountFormatter {

    /** One line per charged move, e.g. "Ice Beam  5x  15t" (fast-move count, then turns). */
    fun lines(counts: MovesetCounts): List<String> =
        counts.counts.map { "${it.move.name}  ${it.fastMoves}x  ${it.turns}t" }

    /** Header plus lines, e.g. "Bubble (+11 / 3t)" followed by the charged moves. */
    fun format(counts: MovesetCounts): String =
        buildString {
            append(counts.fast.name)
            append(" (+").append(counts.fast.energyGain)
            append(" / ").append(counts.fast.turns).append("t)")
            for (line in lines(counts)) append('\n').append(line)
        }
}
