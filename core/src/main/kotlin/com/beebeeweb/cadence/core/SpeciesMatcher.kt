package com.beebeeweb.cadence.core

/**
 * Maps noisy OCR text from the opponent's name label to a known species name.
 *
 * An exact match (ignoring case, accents and punctuation) always wins. Otherwise the closest
 * name is accepted only if it is within a small edit distance and no other name is equally
 * close, so a bad read returns null instead of a wrong species.
 */
class SpeciesMatcher(names: Collection<String>) {

    // One candidate per distinct normalised name, so identical names never count as an ambiguous tie.
    private val candidates: List<Pair<String, String>> =
        names.map { it to Names.normalize(it) }.filter { it.second.isNotEmpty() }.distinctBy { it.second }

    fun match(ocrText: String): String? {
        val query = Names.normalize(ocrText)
        if (query.isEmpty()) return null

        candidates.firstOrNull { it.second == query }?.let { return it.first }

        val allowed = maxDistance(query.length)
        if (allowed == 0) return null

        var best: String? = null
        var bestDistance = Int.MAX_VALUE
        var tied = false
        for ((name, normalized) in candidates) {
            val d = editDistance(query, normalized)
            if (d < bestDistance) {
                best = name
                bestDistance = d
                tied = false
            } else if (d == bestDistance) {
                tied = true
            }
        }
        return if (best != null && bestDistance <= allowed && !tied) best else null
    }

    /** Short names must match exactly; longer names tolerate one or two misread characters. */
    private fun maxDistance(length: Int): Int = when {
        length <= 4 -> 0
        length <= 8 -> 1
        else -> 2
    }

    private fun editDistance(a: String, b: String): Int {
        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)
        for (i in 1..a.length) {
            current[0] = i
            for (j in 1..b.length) {
                val substitution = previous[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(substitution, previous[j] + 1, current[j - 1] + 1)
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[b.length]
    }
}
