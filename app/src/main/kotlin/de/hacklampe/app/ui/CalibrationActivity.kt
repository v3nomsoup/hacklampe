package de.hacklampe.app.ui

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import de.hacklampe.app.BuildConfig
import de.hacklampe.app.R
import de.hacklampe.app.data.Prefs
import de.hacklampe.app.detector.CalibrationAnalyzer
import de.hacklampe.app.detector.CalibrationResult
import de.hacklampe.app.detector.GravityFilter
import de.hacklampe.app.service.GestureService

class CalibrationActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var sensor: Sensor? = null

    private lateinit var instruction: TextView
    private lateinit var countText: TextView
    private lateinit var resultText: TextView
    private lateinit var primary: Button
    private lateinit var secondary: Button

    private val gravityFilter = GravityFilter()
    private var analyzer = CalibrationAnalyzer()
    private var measuring = true
    private var pendingResult: CalibrationResult? = null
    private var maxMagnitude = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calibration)
        applyInsets()

        // Dienst während der Messung pausieren, damit die Taschenlampe nicht schaltet.
        stopService(Intent(this, GestureService::class.java))

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (BuildConfig.DEBUG) {
            android.util.Log.i(TAG, "Kalibrierung Sensor ACCELEROMETER: ${sensor?.name ?: "NULL"}")
        }

        instruction = findViewById(R.id.calInstruction)
        countText = findViewById(R.id.calCount)
        resultText = findViewById(R.id.calResult)
        primary = findViewById(R.id.calPrimary)
        secondary = findViewById(R.id.calSecondary)

        primary.setOnClickListener { onPrimary() }
        secondary.setOnClickListener { onSecondary() }

        enterMeasuring()
    }

    override fun onResume() {
        super.onResume()
        sensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!measuring) return
        val magnitude = gravityFilter.linearMagnitude(
            event.values[0], event.values[1], event.values[2]
        )
        if (BuildConfig.DEBUG && magnitude > 3f) {
            android.util.Log.i(TAG, "CAL mag=%.1f n=%d".format(magnitude, analyzer.strokeCount))
        }
        analyzer.onSample(magnitude)

        // Live-Feedback der stärksten Bewegung (Debug + Release).
        if (magnitude > maxMagnitude) {
            maxMagnitude = magnitude
            resultText.text = "Stärkster Hack: %.0f m/s²".format(maxMagnitude)
        }

        updateMeasuringUi()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun enterMeasuring() {
        measuring = true
        maxMagnitude = 0f
        countText.visibility = View.VISIBLE
        resultText.visibility = View.VISIBLE
        resultText.text = "Stärkster Hack: – m/s²"
        primary.setText(R.string.cal_done)
        secondary.setText(R.string.cal_cancel)
        updateMeasuringUi()
    }

    private fun updateMeasuringUi() {
        val count = analyzer.strokeCount
        countText.text = getString(R.string.cal_count, count, CalibrationAnalyzer.MIN_STROKES)
        val enough = analyzer.hasEnoughData()
        primary.isEnabled = enough
        primary.alpha = if (enough) 1f else 0.4f
        instruction.setText(
            if (enough) R.string.cal_instruction_ready else R.string.cal_instruction
        )
    }

    private fun onPrimary() {
        if (measuring) {
            val r = analyzer.result() ?: return
            pendingResult = r
            measuring = false
            countText.visibility = View.GONE
            resultText.visibility = View.VISIBLE
            resultText.text = "Gemessene Stärke ~%.0f m/s²\nAuslöseschwelle: %.0f m/s²"
                .format(r.medianPeak, r.peakThreshold)
            instruction.setText(R.string.cal_instruction_result)
            primary.isEnabled = true
            primary.alpha = 1f
            primary.setText(R.string.cal_apply)
            secondary.setText(R.string.cal_retry)
        } else {
            val r = pendingResult ?: return
            Prefs.setCalibration(this, r.peakThreshold, r.valleyThreshold)
            finish()
        }
    }

    private fun onSecondary() {
        if (measuring) {
            finish()
        } else {
            analyzer = CalibrationAnalyzer()
            pendingResult = null
            enterMeasuring()
        }
    }

    private companion object {
        private const val TAG = "HackLampe"
    }

    private fun applyInsets() {
        val root = findViewById<View>(R.id.root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }
    }
}
