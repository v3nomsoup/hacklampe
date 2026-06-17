package de.hacklampe.app.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import de.hacklampe.app.data.Prefs
import de.hacklampe.app.service.GestureService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && Prefs.isAutoStart(context)) {
            ContextCompat.startForegroundService(
                context, Intent(context, GestureService::class.java)
            )
        }
    }
}
