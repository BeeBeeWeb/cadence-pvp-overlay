package com.beebeeweb.cadence

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import com.beebeeweb.cadence.core.FormattedLine

/**
 * The floating text panel drawn over the game. It never takes touches or focus, so taps go
 * straight through to the game.
 */
class OverlayPanel(private val context: Context) {

    private val windowManager = context.getSystemService(WindowManager::class.java)
    private var added = false

    private val view = TextView(context).apply {
        typeface = Typeface.MONOSPACE
        textSize = 12f
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

    /** Show the panel with [lines], adding it to the screen if needed. Call on the main thread. */
    fun show(lines: List<FormattedLine>) {
        view.text = buildText(lines)
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

    /** White text; fast-move headers in green and each row's fast-move count in red. */
    private fun buildText(lines: List<FormattedLine>): CharSequence {
        val builder = SpannableStringBuilder()
        lines.forEachIndexed { index, line ->
            val start = builder.length
            builder.append(line.text)
            if (line.isFastMove) {
                builder.setSpan(
                    ForegroundColorSpan(FAST_MOVE_COLOR),
                    start,
                    start + line.text.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
            line.highlight?.let { range ->
                builder.setSpan(
                    ForegroundColorSpan(COUNT_COLOR),
                    start + range.first,
                    start + range.last + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
            if (index < lines.lastIndex) builder.append('\n')
        }
        return builder
    }

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

    private companion object {
        const val TAG = "CadenceOverlay"
        val COUNT_COLOR: Int = Color.rgb(255, 82, 82) // a red that stays readable on the dark panel
        val FAST_MOVE_COLOR: Int = Color.rgb(105, 240, 174) // a light green, readable on the dark panel
    }
}
