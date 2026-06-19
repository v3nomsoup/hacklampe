package de.hacklampe.app.detector

/** Ergebnis einer Kalibrierung: berechnete Schwellen + Diagnose. */
data class CalibrationResult(
    val peakThreshold: Float,
    val valleyThreshold: Float,
    val medianPeak: Float,
    val strokeCount: Int,
)

/**
 * Sammelt während einer Kalibrierung die Spitzenstärken der Hackbewegungen und
 * berechnet daraus passende Erkennungsschwellen. Reine Logik, kein Android.
 *
 * Ein "Schlag" (Stroke) ist eine zusammenhängende Phase, in der die Magnitude
 * über [gate] liegt; sein Maximum wird als Spitze erfasst.
 */
class CalibrationAnalyzer(private val gate: Float = 25f) {

    private val peaks = mutableListOf<Float>()
    private var inStroke = false
    private var currentPeak = 0f

    /** Speist einen Sensor-Magnitudenwert ein. */
    fun onSample(magnitude: Float) {
        if (magnitude >= gate) {
            if (!inStroke) {
                inStroke = true
                currentPeak = magnitude
            } else if (magnitude > currentPeak) {
                currentPeak = magnitude
            }
        } else if (inStroke) {
            // Schlag ist abgeschlossen.
            peaks.add(currentPeak)
            inStroke = false
            currentPeak = 0f
        }
    }

    /** Anzahl der bisher gesehenen Schläge (inkl. eines gerade laufenden). */
    val strokeCount: Int
        get() = peaks.size + if (inStroke) 1 else 0

    /** True, sobald genug abgeschlossene Schläge für ein verlässliches Ergebnis vorliegen. */
    fun hasEnoughData(): Boolean = peaks.size >= MIN_STROKES

    /** Berechnete Schwellen oder null, falls noch nicht genug Daten. */
    fun result(): CalibrationResult? {
        if (!hasEnoughData()) return null
        val sorted = peaks.sorted()
        val medianPeak = sorted[sorted.size / 2]
        val peakThreshold = (medianPeak * PEAK_FACTOR).coerceIn(MIN_PEAK, MAX_PEAK)
        val valleyThreshold = peakThreshold * VALLEY_RATIO
        return CalibrationResult(
            peakThreshold = peakThreshold,
            valleyThreshold = valleyThreshold,
            medianPeak = medianPeak,
            strokeCount = peaks.size,
        )
    }

    companion object {
        const val MIN_STROKES = 4
        const val PEAK_FACTOR = 0.4f
        const val VALLEY_RATIO = 0.6f
        const val MIN_PEAK = 18f
        const val MAX_PEAK = 75f
    }
}
