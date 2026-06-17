package de.hacklampe.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import de.hacklampe.app.R
import de.hacklampe.app.data.Prefs
import de.hacklampe.app.detector.ChopDetector
import de.hacklampe.app.torch.TorchController
import kotlin.math.sqrt

class GestureService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var sensor: Sensor? = null
    private lateinit var detector: ChopDetector
    private lateinit var torch: TorchController

    override fun onCreate() {
        super.onCreate()
        isRunning = true

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        detector = ChopDetector(Prefs.getSensitivity(this))
        torch = TorchController(this)

        createChannel()
        sensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
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
            detector.setSensitivity(Prefs.getSensitivity(this))
        }
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt(x * x + y * y + z * z)
        if (detector.onSample(event.timestamp, magnitude)) {
            torch.toggle()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        isRunning = false
        super.onDestroy()
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
            .setSmallIcon(android.R.drawable.ic_menu_view)
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
        private const val CHANNEL_ID = "hacklampe_gestures"
        private const val NOTIF_ID = 1
    }
}
