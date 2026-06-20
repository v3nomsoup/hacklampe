package de.hacklampe.app.detector

import kotlin.math.sqrt

/**
 * Rechnet aus rohen Beschleunigungswerten (inkl. Schwerkraft) die lineare
 * Beschleunigung (ohne Schwerkraft) und liefert deren Betrag.
 *
 * Ein Tiefpassfilter schätzt die langsam veränderliche Schwerkraft; diese wird
 * vom Rohwert abgezogen, sodass nur die schnellen Bewegungsanteile (Hacks)
 * übrig bleiben. Das ersetzt den virtuellen TYPE_LINEAR_ACCELERATION-Sensor,
 * der auf manchen (Budget-/MTK-)Geräten ohne Sensor-Fusion unzuverlässig ist.
 *
 * Reine Logik ohne Android-Abhängigkeit, damit unit-testbar.
 */
class GravityFilter(private val alpha: Float = 0.8f) {

    private var gx = 0f
    private var gy = 0f
    private var gz = 0f
    private var initialized = false

    /** Betrag der linearen Beschleunigung (Schwerkraft entfernt) für ein Sample. */
    fun linearMagnitude(x: Float, y: Float, z: Float): Float {
        if (!initialized) {
            gx = x; gy = y; gz = z
            initialized = true
        }
        gx = alpha * gx + (1 - alpha) * x
        gy = alpha * gy + (1 - alpha) * y
        gz = alpha * gz + (1 - alpha) * z
        val lx = x - gx
        val ly = y - gy
        val lz = z - gz
        return sqrt(lx * lx + ly * ly + lz * lz)
    }
}
