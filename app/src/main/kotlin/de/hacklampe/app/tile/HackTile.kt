package de.hacklampe.app.tile

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import de.hacklampe.app.service.GestureService

class HackTile : TileService() {

    override fun onStartListening() {
        updateTile()
    }

    override fun onClick() {
        val intent = Intent(this, GestureService::class.java)
        if (GestureService.isRunning) {
            stopService(intent)
        } else {
            ContextCompat.startForegroundService(this, intent)
        }
        updateTile()
    }

    private fun updateTile() {
        qsTile?.apply {
            state = if (GestureService.isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            updateTile()
        }
    }
}
