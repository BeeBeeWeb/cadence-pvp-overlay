package com.beebeeweb.cadence

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView

/**
 * The floating text panel drawn over the game. It never takes touches or focus, so taps go
 * straight through to the game.
 */
class OverlayPanel(private val context: Context) {

    private val windowManager = context.getSystemService(WindowManager::class.java)
    private var added = false

    private val view = TextView(context).apply {
        typeface = Typeface.MONOSPACE
        textSize = 13f
        setTextColor(Color.WHITE)
        val pad = dp(8)
        setPadding(pad, pad, pad, pad)
        background = GradientDrawable().apply {
            setColor(Color.argb(200, 0, 0, 0))
            cornerRadius = dp(10).toFloat()
        }
    }

    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.END
        x = dp(8)
        y = (context.resources.displayMetrics.heightPixels * 0.13f).toInt() // just below the opponent card
        // Android blocks touches that pass through an overlay more opaque than 0.8.
        alpha = 0.8f
    }

    /** Show the panel with [text], adding it to the screen if needed. Call on the main thread. */
    fun show(text: String) {
        view.text = text
        if (added) return
        try {
            windowManager.addView(view, params)
            added = true
        } catch (e: Exception) { // e.g. "display over other apps" was revoked
            Log.w(TAG, "Could not show overlay", e)
        }
    }

    /** Remove the panel from the screen. Call on the main thread. */
    fun hide() {
        if (!added) return
        try {
            windowManager.removeView(view)
        } catch (e: Exception) {
            Log.w(TAG, "Could not remove overlay", e)
        }
        added = false
    }

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

    private companion object {
        const val TAG = "CadenceOverlay"
    }
}
