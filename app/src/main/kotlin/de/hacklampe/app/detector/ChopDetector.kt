package de.hacklampe.app.detector

/**
 * Erkennt das Doppel-Hack-Muster aus einem Strom von Beschleunigungs-Beträgen.
 * Reine Logik ohne Android-Abhängigkeit, damit voll unit-testbar.
 *
 * Modell (Schmitt-Trigger-Peakzähler):
 *  - Ein "Hack" (Schlag) wird gezählt, wenn der Betrag die obere Schwelle
 *    [peakThreshold] übersteigt. Danach ist der Detektor "entschärft", bis der
 *    Betrag wieder unter die untere Schwelle [valleyThreshold] fällt — erst dann
 *    zählt der nächste Schlag. Diese Hysterese trennt die zwei Schläge eines
 *    Doppel-Hacks (deren "Tal" real bei ~20 m/s² liegt), ohne einen einzelnen
 *    kräftigen Schlag doppelt zu zählen.
 *  - Zwei Schläge mit einem Abstand im erlaubten Fenster ergeben einen Doppel-Hack.
 *  - Nach dem Auslösen sperrt ein Cooldown weitere Auslöser kurzzeitig.
 *
 * Schwellwerte aus realen Geräte-Messungen kalibriert: deutliche Hacks erreichen
 * 100–150 m/s², das Tal zwischen zwei Schlägen ~20 m/s², der Rückprall eines
 * Einzel-Hacks bleibt unter ~25 m/s², Gehen/Hinlegen unter ~15 m/s².
 */
class ChopDetector(sensitivity: Int = 5) {

    private var peakThreshold = 0f
    private var valleyThreshold = 0f

    private var armed = true
    private var hasFirstChop = false
    private var firstChopNanos = 0L
    private var cooldownUntilNanos = Long.MIN_VALUE

    init {
        setSensitivity(sensitivity)
    }

    /** Empfindlichkeit 1 (unempfindlich) .. 10 (sehr empfindlich). */
    fun setSensitivity(level: Int) {
        val l = level.coerceIn(1, 10)
        // level 1 -> 60 m/s² (nur sehr kräftige Hacks), level 10 -> 22 m/s²
        peakThreshold = MAX_PEAK_THRESHOLD - (l - 1) * (PEAK_THRESHOLD_SPAN / 9f)
        valleyThreshold = peakThreshold * VALLEY_RATIO
    }

    /** Setzt die Schwellen direkt (aus einer Kalibrierung). */
    fun setThresholds(peak: Float, valley: Float) {
        peakThreshold = peak
        valleyThreshold = valley
    }

    /**
     * Liefert true, sobald ein vollständiger Doppel-Chop erkannt wurde.
     * Erwartet monoton nicht-fallende Zeitstempel (z. B. SensorEvent.timestamp).
     */
    fun onSample(timestampNanos: Long, magnitude: Float): Boolean {
        if (!armed) {
            if (magnitude < valleyThreshold) armed = true
            return false
        }
        if (magnitude < peakThreshold) return false
        // Steigende Flanke über den Schwellwert -> ein Chop
        armed = false
        return registerChop(timestampNanos)
    }

    private fun registerChop(now: Long): Boolean {
        if (now < cooldownUntilNanos) {
            hasFirstChop = false
            return false
        }
        if (!hasFirstChop) {
            hasFirstChop = true
            firstChopNanos = now
            return false
        }
        val gap = now - firstChopNanos
        return when {
            gap in MIN_GAP_NANOS..MAX_GAP_NANOS -> {
                hasFirstChop = false
                cooldownUntilNanos = now + COOLDOWN_NANOS
                true
            }
            gap > MAX_GAP_NANOS -> {
                // zu langsam: dieser Schlag startet ein neues Paar
                firstChopNanos = now
                false
            }
            else -> false // zu schnell: zweiten Schlag ignorieren, ersten behalten
        }
    }

    private companion object {
        /** Peak-Schwellwert bei Empfindlichkeit 1 (unempfindlichste Stufe), m/s². */
        const val MAX_PEAK_THRESHOLD = 60f
        /** Spanne über Level 1..10: 60 - 38 = 22 m/s² bei höchster Empfindlichkeit. */
        const val PEAK_THRESHOLD_SPAN = 38f
        /** Wieder-Scharf-Schranke (Tal zwischen zwei Schlägen) als Anteil des Peaks. */
        const val VALLEY_RATIO = 0.6f

        const val MIN_GAP_NANOS = 100_000_000L    // 100 ms: schneller wäre ein Schlag
        const val MAX_GAP_NANOS = 800_000_000L    // 800 ms: langsamer zählt als getrennt
        const val COOLDOWN_NANOS = 1_000_000_000L // 1 s Sperre nach Auslösen
    }
}
