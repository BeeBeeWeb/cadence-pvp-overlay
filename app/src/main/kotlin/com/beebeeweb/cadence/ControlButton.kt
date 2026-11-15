package com.beebeeweb.cadence

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.TextView
import kotlin.math.abs

/**
 * A small round floating play/pause button that sits over the game. Tap it to toggle; drag it to move it
 * (the position is remembered). Unlike the count panel it takes touches, but only on its own area.
 */
class ControlButton(
    private val context: Context,
    private val onToggle: () -> Unit,
) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var added = false

    private val background = GradientDrawable().apply { shape = GradientDrawable.OVAL }

    private val view = TextView(context).apply {
        textSize = 16f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        this.background = this@ControlButton.background
    }

    private val params = WindowManager.LayoutParams(
        dp(44),
        dp(44),
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = prefs.getInt(KEY_X, dp(8))
        y = prefs.getInt(KEY_Y, (context.resources.displayMetrics.heightPixels * 0.45f).toInt())
    }

    init {
        attachTouchHandling()
        setPaused(true)
    }

    /** Update the icon (play when paused, pause when running) and colour. Call on the main thread. */
    fun setPaused(paused: Boolean) {
        view.text = if (paused) "\u25B6\uFE0E" else "\u275A\u275A" // play / pause
        background.setColor(if (paused) Color.argb(225, 51, 51, 51) else Color.argb(225, 27, 94, 32))
    }

    fun show() {
        if (added) return
        try {
            windowManager.addView(view, params)
            added = true
        } catch (e: Exception) { // e.g. "display over other apps" was revoked
            Log.w(TAG, "Could not show control button", e)
        }
    }

    fun hide() {
        if (!added) return
        try {
            windowManager.removeView(view)
        } catch (e: Exception) {
            Log.w(TAG, "Could not remove control button", e)
        }
        added = false
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachTouchHandling() {
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var dragging = false

        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = params.x
                    startY = params.y
                    dragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (!dragging && (abs(dx) > touchSlop || abs(dy) > touchSlop)) dragging = true
                    if (dragging) {
                        params.x = startX + dx.toInt()
                        params.y = startY + dy.toInt()
                        windowManager.updateViewLayout(view, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (dragging) {
                        prefs.edit().putInt(KEY_X, params.x).putInt(KEY_Y, params.y).apply()
                    } else {
                        onToggle()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

    private companion object {
        const val TAG = "CadenceControl"
        const val PREFS = "cadence_ui"
        const val KEY_X = "button_x"
        const val KEY_Y = "button_y"
    }
}
