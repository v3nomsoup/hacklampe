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
            try {
                ContextCompat.startForegroundService(
                    context, Intent(context, GestureService::class.java)
                )
            } catch (e: Exception) {
                // Ab Android 12+ darf ein specialUse-Foreground-Service evtl. nicht
                // direkt nach dem Boot gestartet werden
                // (ForegroundServiceStartNotAllowedException). Autostart ist daher
                // Best-Effort; der Nutzer kann den Dienst per Kachel/App starten.
            }
        }
    }
}
