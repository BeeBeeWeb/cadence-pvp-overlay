package com.beebeeweb.cadence.core

/**
 * One line of overlay text.
 *
 * [highlight] marks the characters (the fast-move count, e.g. "12x") the UI should colour, and
 * [isFastMove] marks a fast-move header line, which the UI colours as a whole. Plain lines have neither.
 */
data class FormattedLine(
    val text: String,
    val highlight: IntRange? = null,
    val isFastMove: Boolean = false,
)

/** Turns merged sections into the text shown in the overlay (meant for a monospace font). */
object CountFormatter {

    /**
     * Each fast move is listed once as a header, followed by its charged moves with their energy cost
     * in brackets, the number of fast moves needed ("x") and the turns ("t"):
     *
     *     Water Gun (+3 / 1t)
     *     Aqua Tail(35)      12x  12t
     *     Last Resort(55)    19x  19t
     *
     * [extraRow], when it returns text for a section, adds one plain line right under the header
     * (used for the throw-timing row).
     *
     * Sections are separated by an empty line. The count ("12x") carries the highlight; turns do not.
     */
    fun lines(
        sections: List<FastMoveSection>,
        extraRow: (FastMoveSection) -> String? = { null },
    ): List<FormattedLine> = buildList {
        sections.forEachIndexed { index, section ->
            if (index > 0) add(FormattedLine(""))
            add(FormattedLine("${section.fast.name} (+${section.fast.energyGain} / ${section.fast.turns}t)", isFastMove = true))
            extraRow(section)?.let { add(FormattedLine(it)) }
            val labels = section.counts.map { "${it.move.name}(${it.move.energyCost})" }
            val labelWidth = labels.maxOfOrNull { it.length } ?: 0
            section.counts.forEachIndexed { i, count ->
                val countText = "${count.fastMoves}x"
                val paddedCount = countText.padStart(3)
                val text = "${labels[i].padEnd(labelWidth)}  $paddedCount  ${count.turns}t"
                val start = labelWidth + 2 + (paddedCount.length - countText.length)
                add(FormattedLine(text, highlight = start until start + countText.length))
            }
        }
    }
}
