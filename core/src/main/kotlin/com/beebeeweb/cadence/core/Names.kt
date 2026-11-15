package com.beebeeweb.cadence.core

import java.text.Normalizer
import java.util.Locale

/** Name normalisation shared by the repository and the OCR matcher. */
object Names {

    private val combiningMarks = Regex("\\p{M}+")

    /** Lower-case letters and digits only, without accents: "Flabébé" -> "flabebe", "Mr. Mime" -> "mrmime". */
    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(combiningMarks, "")
            .lowercase(Locale.ROOT)
            .filter { it.isLetterOrDigit() }
}
