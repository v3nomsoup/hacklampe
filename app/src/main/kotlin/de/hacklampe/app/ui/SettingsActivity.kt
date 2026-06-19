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
import de.hacklampe.app.R
import de.hacklampe.app.data.Prefs
import de.hacklampe.app.service.GestureService

class SettingsActivity : AppCompatActivity() {

    private lateinit var toggleButton: Button
    private lateinit var statusText: TextView

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
        val seekBar = findViewById<SeekBar>(R.id.sensitivitySeekBar)
        val sensitivityValue = findViewById<TextView>(R.id.sensitivityValue)
        val autostart = findViewById<CheckBox>(R.id.autostartCheckBox)

        val initial = Prefs.getSensitivity(this)
        seekBar.progress = initial - 1
        sensitivityValue.text = getString(R.string.sensitivity_value, initial)
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val level = progress + 1
                Prefs.setSensitivity(this@SettingsActivity, level)
                sensitivityValue.text = getString(R.string.sensitivity_value, level)
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}

            override fun onStopTrackingTouch(sb: SeekBar?) {
                if (GestureService.isRunning) {
                    val refresh = Intent(this@SettingsActivity, GestureService::class.java)
                        .apply { action = GestureService.ACTION_REFRESH }
                    ContextCompat.startForegroundService(this@SettingsActivity, refresh)
                }
            }
        })

        autostart.isChecked = Prefs.isAutoStart(this)
        autostart.setOnCheckedChangeListener { _, checked ->
            Prefs.setAutoStart(this, checked)
        }

        toggleButton.setOnClickListener { onToggleClicked() }
    }

    override fun onResume() {
        super.onResume()
        updateRunningUi(GestureService.isRunning)
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

    private fun updateRunningUi(running: Boolean) {
        toggleButton.setText(if (running) R.string.settings_stop else R.string.settings_start)
        statusText.setText(if (running) R.string.status_on else R.string.status_off)
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
