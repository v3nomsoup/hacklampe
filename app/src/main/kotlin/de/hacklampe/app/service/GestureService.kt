package de.hacklampe.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ServiceInfo
import android.service.quicksettings.TileService
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import de.hacklampe.app.BuildConfig
import de.hacklampe.app.R
import de.hacklampe.app.data.Prefs
import de.hacklampe.app.detector.ChopDetector
import de.hacklampe.app.detector.GravityFilter
import de.hacklampe.app.tile.HackTile
import de.hacklampe.app.torch.TorchController

class GestureService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var sensor: Sensor? = null
    private lateinit var detector: ChopDetector
    private lateinit var torch: TorchController
    private val gravityFilter = GravityFilter()

    private var firstSampleNanos = 0L

    override fun onCreate() {
        super.onCreate()
        isRunning = true

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        detector = ChopDetector()
        torch = TorchController(this)

        applyConfig()

        if (BuildConfig.DEBUG) {
            android.util.Log.i(TAG, "Sensor ACCELEROMETER: ${sensor?.name ?: "NULL"}")
        }

        createChannel()
        sensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }

        broadcastStateChange()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        // Pflicht-Promotion zum Vordergrund-Service MIT deklariertem Typ.
        // Ab Android 14 (API 34) zwingend, sonst stürzt der Start ab.
        ServiceCompat.startForeground(
            this,
            NOTIF_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
        if (intent?.action == ACTION_REFRESH) {
            applyConfig()
        }
        return START_STICKY
    }

    private fun applyConfig() {
        if (Prefs.isCalibrated(this)) {
            // Empfindlichkeitsregler wirkt relativ zur Kalibrierung (Stufe 5 = 1.0).
            val factor = Prefs.sensitivityFactor(Prefs.getSensitivity(this))
            detector.setThresholds(
                Prefs.getCalibratedPeak(this) * factor,
                Prefs.getCalibratedValley(this) * factor,
            )
        } else {
            detector.setSensitivity(Prefs.getSensitivity(this))
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val magnitude = gravityFilter.linearMagnitude(
            event.values[0], event.values[1], event.values[2]
        )

        if (BuildConfig.DEBUG && magnitude > 6f) {
            if (firstSampleNanos == 0L) firstSampleNanos = event.timestamp
            val tMs = (event.timestamp - firstSampleNanos) / 1_000_000L
            android.util.Log.i(TAG, "WAVE t=$tMs mag=%.1f".format(magnitude))
        }

        if (detector.onSample(event.timestamp, magnitude)) {
            torch.toggle()
            if (BuildConfig.DEBUG) android.util.Log.i(TAG, "DOPPEL-HACK -> Taschenlampe umgeschaltet")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        isRunning = false
        broadcastStateChange()
        super.onDestroy()
    }

    /** Meldet Zustandsänderungen an Kachel (requestListeningState) und App (Broadcast). */
    private fun broadcastStateChange() {
        TileService.requestListeningState(this, ComponentName(this, HackTile::class.java))
        sendBroadcast(Intent(ACTION_STATE_CHANGED).setPackage(packageName))
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, GestureService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_tile_flashlight)
            .setOngoing(true)
            .addAction(0, getString(R.string.notification_stop), stopPending)
            .build()
    }

    companion object {
        @Volatile
        var isRunning = false
            private set

        const val ACTION_STOP = "de.hacklampe.app.action.STOP"
        const val ACTION_REFRESH = "de.hacklampe.app.action.REFRESH"
        const val ACTION_STATE_CHANGED = "de.hacklampe.app.action.STATE_CHANGED"
        private const val CHANNEL_ID = "hacklampe_gestures"
        private const val NOTIF_ID = 1
        private const val TAG = "HackLampe"
    }
}
