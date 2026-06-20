package de.hacklampe.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import de.hacklampe.app.BuildConfig
import de.hacklampe.app.R
import de.hacklampe.app.data.Prefs
import de.hacklampe.app.service.GestureService

class SettingsActivity : AppCompatActivity() {

    private lateinit var toggleButton: Button
    private lateinit var statusText: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var sensitivityValue: TextView
    private lateinit var calibrationStatus: TextView

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // Egal ob erlaubt oder verweigert: Dienst trotzdem starten (er läuft auch
            // ohne sichtbare Benachrichtigung). Der Nutzer wurde vorher aufgeklärt.
            startGestureService()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        applyInsets()

        toggleButton = findViewById(R.id.toggleButton)
        statusText = findViewById(R.id.statusText)
        seekBar = findViewById(R.id.sensitivitySeekBar)
        sensitivityValue = findViewById(R.id.sensitivityValue)
        calibrationStatus = findViewById(R.id.calibrationStatus)
        val autostart = findViewById<CheckBox>(R.id.autostartCheckBox)
        val calibrateButton = findViewById<Button>(R.id.calibrateButton)

        seekBar.progress = Prefs.getSensitivity(this) - 1
        sensitivityValue.text = getString(R.string.sensitivity_value, Prefs.getSensitivity(this))
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                val level = progress + 1
                // Bei bestehender Kalibrierung wirkt der Regler relativ dazu
                // (Stufe 5 = Kalibrierungswert), sonst absolut.
                Prefs.setSensitivity(this@SettingsActivity, level)
                sensitivityValue.text = getString(R.string.sensitivity_value, level)
                updateCalibrationStatus()
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}

            override fun onStopTrackingTouch(sb: SeekBar?) {
                refreshRunningService()
            }
        })

        autostart.isChecked = Prefs.isAutoStart(this)
        autostart.setOnCheckedChangeListener { _, checked ->
            Prefs.setAutoStart(this, checked)
        }

        calibrateButton.setOnClickListener {
            startActivity(Intent(this, CalibrationActivity::class.java))
        }

        toggleButton.setOnClickListener { onToggleClicked() }

        findViewById<TextView>(R.id.versionText).text =
            "v${BuildConfig.VERSION_NAME}" + if (BuildConfig.DEBUG) " · debug" else ""
    }

    override fun onResume() {
        super.onResume()
        updateRunningUi(GestureService.isRunning)
        updateCalibrationStatus()
        // Eventuell geänderte Kalibrierung an einen laufenden Dienst übernehmen.
        refreshRunningService()
    }

    private fun onToggleClicked() {
        if (GestureService.isRunning) {
            stopService(Intent(this, GestureService::class.java))
            updateRunningUi(false)
            return
        }
        if (needsNotificationPermission()) {
            showNotificationRationale()
        } else {
            startGestureService()
        }
    }

    private fun startGestureService() {
        ContextCompat.startForegroundService(this, Intent(this, GestureService::class.java))
        updateRunningUi(true)
    }

    private fun refreshRunningService() {
        if (GestureService.isRunning) {
            val refresh = Intent(this, GestureService::class.java)
                .apply { action = GestureService.ACTION_REFRESH }
            ContextCompat.startForegroundService(this, refresh)
        }
    }

    private fun updateRunningUi(running: Boolean) {
        toggleButton.setText(if (running) R.string.settings_stop else R.string.settings_start)
        statusText.setText(if (running) R.string.status_on else R.string.status_off)
    }

    private fun updateCalibrationStatus() {
        if (Prefs.isCalibrated(this)) {
            val effective = Prefs.getCalibratedPeak(this) *
                Prefs.sensitivityFactor(Prefs.getSensitivity(this))
            calibrationStatus.text = getString(R.string.settings_calibrated, effective)
        } else {
            calibrationStatus.setText(R.string.cal_status_manual)
        }
    }

    private fun needsNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED

    private fun showNotificationRationale() {
        AlertDialog.Builder(this)
            .setTitle(R.string.perm_dialog_title)
            .setMessage(R.string.perm_dialog_message)
            .setPositiveButton(R.string.perm_dialog_ok) { _, _ ->
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .setNegativeButton(R.string.perm_dialog_cancel, null)
            .show()
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
