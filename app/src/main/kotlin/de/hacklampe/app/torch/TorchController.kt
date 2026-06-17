package de.hacklampe.app.torch

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

/** Schaltet die LED-Taschenlampe und merkt sich den Zustand. */
class TorchController(context: Context) {

    private val cameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private val torchCameraId: String? = cameraManager.cameraIdList.firstOrNull { id ->
        cameraManager.getCameraCharacteristics(id)
            .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
    }

    @Volatile
    private var isOn = false

    fun toggle() = setTorch(!isOn)

    fun setTorch(enabled: Boolean) {
        val id = torchCameraId ?: return
        try {
            cameraManager.setTorchMode(id, enabled)
            isOn = enabled
        } catch (e: CameraAccessException) {
            // Taschenlampe gerade nicht verfügbar (z.B. Kamera in Benutzung)
        }
    }
}
