package com.beebeeweb.cadence

import android.graphics.RectF

/**
 * Where things are on the battle screen, as fractions of the screen (0..1), portrait orientation.
 * Measured from the owner's screenshots and video of the current (2026) battle layout; tune here if
 * the game's layout changes.
 */
object ScreenRegions {

    /** The opponent's card in the top-right corner: CP and species name, shields and remaining Pokémon. */
    val OPPONENT_CARD = RectF(0.58f, 0.055f, 1.0f, 0.12f)
}
