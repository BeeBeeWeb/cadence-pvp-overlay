package com.beebeeweb.cadence

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.RectF
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.WindowManager
import androidx.core.app.ServiceCompat
import androidx.core.content.IntentCompat
import com.beebeeweb.cadence.core.CountCalculator
import com.beebeeweb.cadence.core.CountFormatter
import com.beebeeweb.cadence.core.ActivePokemon
import com.beebeeweb.cadence.core.BacklinePredictor
import com.beebeeweb.cadence.core.CmpEstimator
import com.beebeeweb.cadence.core.FastMove
import com.beebeeweb.cadence.core.FormattedLine
import com.beebeeweb.cadence.core.MovesetData
import com.beebeeweb.cadence.core.TeamMember
import com.beebeeweb.cadence.core.TimingFormatter
import com.beebeeweb.cadence.core.MovesetRepository
import com.beebeeweb.cadence.core.NameStabilizer
import com.beebeeweb.cadence.core.OpponentNameReader
import com.beebeeweb.cadence.core.SpeciesMatcher
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * Captures the screen (with the user's permission), reads the opponent's name from the top-right
 * battle card a few times per second and shows the move counts in an overlay.
 */
class OverlayService : Service() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val worker = HandlerThread("cadence-capture").also { it.start() }
    private val workerHandler = Handler(worker.looper)

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val stabilizer = NameStabilizer(hitsToConfirm = 2, missesToClear = 15)
    private lateinit var panel: OverlayPanel
    private lateinit var control: ControlButton

    private var repository: MovesetRepository? = null
    private var matcher: SpeciesMatcher? = null

    // Your own team (set up in "Your team"); reloaded whenever you press play.
    @Volatile private var team: List<TeamMember?> = emptyList()
    private var activeIndex: Int? = null
    private var chargedNames: Map<String, String> = emptyMap()
    private var fastById: Map<String, FastMove> = emptyMap()

    private var projection: MediaProjection? = null
    private var projectionCallback: MediaProjection.Callback? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    /** Starts paused: the user taps the floating button when a battle begins. */
    @Volatile private var paused = true

    // The panel is hidden while the opponent's name can't be read (e.g. during a charged-move animation),
    // so you can see the move that was thrown.
    private var misses = 0
    private var leadTypes: List<String> = emptyList() // types of the first opponent seen this battle
    private var nameVisible = false

    @Volatile private var busy = false
    private var lastFrameAt = 0L
    private var running = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        panel = OverlayPanel(this)
        control = ControlButton(this) { setPaused(!paused) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startCapture(intent)
            else -> stopSelf() // ACTION_STOP, or a restart without data
        }
        return START_NOT_STICKY
    }

    private fun startCapture(intent: Intent) {
        if (running) return
        // Android 14+ requires the foreground service to be running before the projection is created.
        startAsForeground()

        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
        val data = IntentCompat.getParcelableExtra(intent, EXTRA_RESULT_DATA, Intent::class.java)
        if (resultCode != Activity.RESULT_OK || data == null) {
            stopSelf()
            return
        }

        try {
            val json = assets.open(DATA_ASSET).bufferedReader().use { it.readText() }
            val species = MovesetData.parse(json)
            val catalog = MovesetData.parseCatalog(json)
            chargedNames = catalog.charged.associate { it.id to it.name }
            fastById = catalog.fast.associateBy { it.id }
            repository = MovesetRepository(species).also { matcher = SpeciesMatcher(it.speciesNames) }
        } catch (e: Exception) {
            Log.e(TAG, "Could not load $DATA_ASSET", e)
            stopSelf()
            return
        }

        val manager = getSystemService(MediaProjectionManager::class.java)
        val newProjection = manager.getMediaProjection(resultCode, data)
        val callback = object : MediaProjection.Callback() {
            override fun onStop() {
                stopSelf()
            }
        }
        newProjection.registerCallback(callback, mainHandler) // required before creating a virtual display
        projection = newProjection
        projectionCallback = callback

        val bounds = getSystemService(WindowManager::class.java).maximumWindowMetrics.bounds
        val width = (bounds.width() * CAPTURE_SCALE).toInt()
        val height = (bounds.height() * CAPTURE_SCALE).toInt()
        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        reader.setOnImageAvailableListener({ onFrame(it) }, workerHandler)
        imageReader = reader
        virtualDisplay = newProjection.createVirtualDisplay(
            "cadence-capture",
            width,
            height,
            resources.displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            workerHandler,
        )
        running = true
        loadTeam()
        setPaused(true)
        control.show()
        Log.i(TAG, "Capturing ${width}x$height")
    }

    /** Main thread. Pausing hides the panel and forgets the current opponent. */
    private fun loadTeam() {
        team = TeamStore(this).load()
        activeIndex = team.indexOfFirst { it != null }.takeIf { it >= 0 }
    }

    private fun setPaused(value: Boolean) {
        if (!value) loadTeam() // pick up any change made in "Your team"
        paused = value
        misses = 0
        nameVisible = false
        leadTypes = emptyList()
        stabilizer.reset()
        panel.hide()
        control.setPaused(value)
    }

    /** Runs on the worker thread for every captured frame; processes about three per second. */
    private fun onFrame(reader: ImageReader) {
        val image = reader.acquireLatestImage() ?: return
        try {
            val now = SystemClock.elapsedRealtime()
            if (paused || busy || now - lastFrameAt < MIN_FRAME_INTERVAL_MS) return
            val wantButtons = team.any { it != null }
            val (card, buttons) = cropRegions(image, wantButtons)
            lastFrameAt = now
            busy = true
            recognizer.process(InputImage.fromBitmap(card, 0))
                .addOnSuccessListener { text -> onText(text.textBlocks.flatMap { block -> block.lines.map { it.text } }) }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Text recognition failed", e)
                    onText(emptyList())
                }
                .addOnCompleteListener { readButtons(buttons) }
        } finally {
            image.close()
        }
    }

    /** Second step of a frame: read your charged-move labels to see which of your Pokémon is out. */
    private fun readButtons(buttons: Bitmap?) {
        if (buttons == null) {
            busy = false
            return
        }
        recognizer.process(InputImage.fromBitmap(buttons, 0))
            .addOnSuccessListener { text -> onButtonText(text.textBlocks.flatMap { block -> block.lines.map { it.text } }) }
            .addOnFailureListener { e -> Log.w(TAG, "Button text recognition failed", e) }
            .addOnCompleteListener { busy = false }
    }

    /** Main thread. Keeps the last known Pokémon when the labels can't be read. */
    private fun onButtonText(lines: List<String>) {
        if (paused) return
        val index = ActivePokemon.pick(lines, team, chargedNames) ?: return
        if (index != activeIndex) {
            activeIndex = index
            render()
        }
    }

    private fun cropRegions(image: Image, wantButtons: Boolean): Pair<Bitmap, Bitmap?> {
        val plane = image.planes[0]
        val pixelStride = plane.pixelStride
        val rowPadding = plane.rowStride - pixelStride * image.width
        val full = Bitmap.createBitmap(image.width + rowPadding / pixelStride, image.height, Bitmap.Config.ARGB_8888)
        full.copyPixelsFromBuffer(plane.buffer)
        val card = crop(full, image, ScreenRegions.OPPONENT_CARD)
        val buttons = if (wantButtons) crop(full, image, ScreenRegions.CHARGED_BUTTONS) else null
        full.recycle()
        return card to buttons
    }

    private fun crop(full: Bitmap, image: Image, region: RectF): Bitmap {
        val x = (image.width * region.left).toInt()
        val y = (image.height * region.top).toInt()
        val w = (image.width * (region.right - region.left)).toInt()
        val h = (image.height * (region.bottom - region.top)).toInt()
        return Bitmap.createBitmap(full, x, y, w, h)
    }

    /** Main thread (ML Kit delivers its results here). */
    private fun onText(lines: List<String>) {
        if (paused) return
        val matcher = matcher ?: return
        val species = OpponentNameReader.pickSpecies(lines, matcher)
        misses = if (species == null) misses + 1 else 0
        val changed = stabilizer.update(species)
        if (leadTypes.isEmpty()) {
            stabilizer.current?.let { leadTypes = repository?.find(it)?.types.orEmpty() }
        }
        if (misses >= HIDE_AFTER_MISSES) {
            nameVisible = false
            panel.hide()
            return
        }
        if (changed || !nameVisible) {
            nameVisible = true
            render()
        }
    }

    private fun render() {
        if (!nameVisible) {
            panel.hide()
            return
        }
        val name = stabilizer.current
        val movesets = if (name != null) repository?.topMovesets(name).orEmpty() else emptyList()
        if (movesets.isEmpty()) {
            panel.hide()
            return
        }
        val mine = team.getOrNull(activeIndex ?: -1)?.let { fastById[it.fastId] }
        val sections = CountCalculator.sectionsFor(movesets)
        val cmp = team.getOrNull(activeIndex ?: -1)?.let { member ->
            val mineAttack = repository?.find(member.speciesName)?.baseAttack ?: 0
            val theirAttack = repository?.find(name!!)?.baseAttack ?: 0
            CmpEstimator.label(CmpEstimator.compare(mineAttack, theirAttack))
        }
        val lines = CountFormatter.lines(sections) { section -> mine?.let { TimingFormatter.row(it.turns, section.fast.turns) } }
        val back = BacklinePredictor.label(BacklinePredictor.likelyTypes(leadTypes))
        val header = listOfNotNull(cmp, back).map { FormattedLine(it) }
        panel.show(if (header.isNotEmpty()) header + FormattedLine("") + lines else lines)
    }

    private fun startAsForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Cadence", NotificationManager.IMPORTANCE_LOW),
        )
        val stop = PendingIntent.getService(
            this,
            0,
            Intent(this, OverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        @Suppress("DEPRECATION")
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("Cadence is reading the battle screen")
            .setOngoing(true)
            .addAction(android.R.drawable.ic_delete, "Stop", stop)
            .build()
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
    }

    override fun onDestroy() {
        running = false
        imageReader?.setOnImageAvailableListener(null, null)
        virtualDisplay?.release()
        imageReader?.close()
        projection?.let { p ->
            projectionCallback?.let { p.unregisterCallback(it) }
            p.stop()
        }
        panel.hide()
        control.hide()
        recognizer.close()
        worker.quitSafely()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.beebeeweb.cadence.START"
        const val ACTION_STOP = "com.beebeeweb.cadence.STOP"
        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_RESULT_DATA = "resultData"

        private const val TAG = "CadenceService"
        private const val CHANNEL_ID = "cadence_capture"
        private const val NOTIFICATION_ID = 1
        const val DATA_ASSET = "greatleague.json"

        /** Frames in a row without a readable opponent name before the overlay hides (about 0.6 s). */
        private const val HIDE_AFTER_MISSES = 2

        /** Fraction of the real screen size to capture. Lower is lighter, higher reads small text better. */
        private const val CAPTURE_SCALE = 0.75f
        private const val MIN_FRAME_INTERVAL_MS = 300L
    }
}
