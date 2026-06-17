package de.hacklampe.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import de.hacklampe.app.R
import de.hacklampe.app.data.Prefs
import de.hacklampe.app.service.GestureService

class SettingsActivity : AppCompatActivity() {

    private lateinit var toggleButton: Button

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* egal */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        toggleButton = findViewById(R.id.toggleButton)
        val seekBar = findViewById<SeekBar>(R.id.sensitivitySeekBar)
        val autostart = findViewById<CheckBox>(R.id.autostartCheckBox)

        // Empfindlichkeit 1..10 auf SeekBar 0..9 abbilden
        seekBar.progress = Prefs.getSensitivity(this) - 1
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                Prefs.setSensitivity(this@SettingsActivity, progress + 1)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {
                // Laufenden Service über die neue Empfindlichkeit informieren
                if (GestureService.isRunning) {
                    val refresh = Intent(this@SettingsActivity, GestureService::class.java).apply {
                        action = GestureService.ACTION_REFRESH
                    }
                    ContextCompat.startForegroundService(this@SettingsActivity, refresh)
                }
            }
        })

        autostart.isChecked = Prefs.isAutoStart(this)
        autostart.setOnCheckedChangeListener { _, checked ->
            Prefs.setAutoStart(this, checked)
        }

        toggleButton.setOnClickListener { onToggleClicked() }

        requestNotificationPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        updateToggleLabel()
    }

    private fun onToggleClicked() {
        val intent = Intent(this, GestureService::class.java)
        if (GestureService.isRunning) {
            stopService(intent)
        } else {
            ContextCompat.startForegroundService(this, intent)
        }
        updateToggleLabel()
    }

    private fun updateToggleLabel() {
        toggleButton.setText(
            if (GestureService.isRunning) R.string.settings_stop else R.string.settings_start
        )
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
