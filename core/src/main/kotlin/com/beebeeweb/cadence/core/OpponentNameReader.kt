package com.beebeeweb.cadence.core

/** Picks the opponent's species out of the text lines OCR found on the top-right battle card. */
object OpponentNameReader {

    // The card shows "CP 1465" next to the name; OCR may return them as separate lines or one line.
    private val cpLabel = Regex("(?i)\\bCP\\s*\\S*")

    /** First line that [matcher] recognises as a species once any "CP ..." text is removed, or null. */
    fun pickSpecies(lines: List<String>, matcher: SpeciesMatcher): String? {
        for (line in lines) {
            val cleaned = line.replace(cpLabel, " ").trim()
            if (cleaned.isEmpty()) continue
            matcher.match(cleaned)?.let { return it }
        }
        return null
    }
}
