package com.beebeeweb.cadence.core

/**
 * One line of overlay text. [highlight] marks the characters (the fast-move count, e.g. "12x")
 * that the UI should colour; null means no highlight.
 */
data class FormattedLine(val text: String, val highlight: IntRange? = null)

/** Turns merged sections into the text shown in the overlay (meant for a monospace font). */
object CountFormatter {

    /**
     * Each fast move is listed once as a header, followed by its charged moves:
     *
     *     Water Gun (+3 / 1t)
     *     Aqua Tail     12x  12t
     *     Scald         17x  17t
     *
     * Sections are separated by an empty line. The count ("12x") carries the highlight; turns do not.
     */
    fun lines(sections: List<FastMoveSection>): List<FormattedLine> = buildList {
        sections.forEachIndexed { index, section ->
            if (index > 0) add(FormattedLine(""))
            add(FormattedLine("${section.fast.name} (+${section.fast.energyGain} / ${section.fast.turns}t)"))
            val nameWidth = section.counts.maxOfOrNull { it.move.name.length } ?: 0
            for (count in section.counts) {
                val countText = "${count.fastMoves}x"
                val paddedCount = countText.padStart(3)
                val text = "${count.move.name.padEnd(nameWidth)}  $paddedCount  ${count.turns}t"
                val start = nameWidth + 2 + (paddedCount.length - countText.length)
                add(FormattedLine(text, start until start + countText.length))
            }
        }
    }
}
