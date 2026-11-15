package com.beebeeweb.cadence

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

/** Start/stop screen and a short checklist; the real work happens in [OverlayService]. */
class MainActivity : ComponentActivity() {

    private lateinit var status: TextView

    private val capturePermission =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (result.resultCode == Activity.RESULT_OK && data != null) {
                val intent = Intent(this, OverlayService::class.java)
                    .setAction(OverlayService.ACTION_START)
                    .putExtra(OverlayService.EXTRA_RESULT_CODE, result.resultCode)
                    .putExtra(OverlayService.EXTRA_RESULT_DATA, data)
                startForegroundService(intent)
                status.text = "Cadence is running, paused. Open the game and tap the floating play button when the battle begins."
            } else {
                status.text = "Screen capture was not allowed, so Cadence can't read the battle."
            }
        }

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { requestCapture() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pad = (24 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
        }

        root.addView(TextView(this).apply {
            text = "Cadence"
            textSize = 28f
        })
        root.addView(TextView(this).apply {
            text = "Shows how many fast moves, and how many turns, the opponent needs to reach each " +
                "charged move. Great League. Reads your screen only; it never taps for you. " +
                "A round play/pause button appears over the game: tap it when a battle begins."
            setPadding(0, pad / 2, 0, pad)
        })
        status = TextView(this).apply { setPadding(0, 0, 0, pad) }
        root.addView(status)

        root.addView(button("1. Allow display over other apps") {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        })
        root.addView(button("Your team") { startActivity(Intent(this, TeamActivity::class.java)) })
        root.addView(button("2. Start Cadence") { start() })
        root.addView(button("Stop") {
            startService(Intent(this, OverlayService::class.java).setAction(OverlayService.ACTION_STOP))
            status.text = "Stopped."
        })
        root.addView(TextView(this).apply {
            text = "When Android asks what to share, choose \"Entire screen\" (not a single app)."
            setPadding(0, pad, 0, 0)
        })

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        if (!Settings.canDrawOverlays(this)) {
            status.text = "Step 1 needed: allow Cadence to display over other apps."
        } else if (status.text.isNullOrEmpty() || status.text.startsWith("Step 1")) {
            status.text = "Ready. Tap \"Start Cadence\"."
        }
    }

    private fun start() {
        if (!Settings.canDrawOverlays(this)) {
            status.text = "Step 1 needed: allow Cadence to display over other apps."
            return
        }
        val needsNotificationPermission = Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsNotificationPermission) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            requestCapture()
        }
    }

    private fun requestCapture() {
        val manager = getSystemService(MediaProjectionManager::class.java)
        capturePermission.launch(manager.createScreenCaptureIntent())
    }

    private fun button(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        setOnClickListener { onClick() }
    }
}
