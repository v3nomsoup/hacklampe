package de.hacklampe.app.detector

/**
 * Erkennt das Doppel-Hack-Muster aus einem Strom von Beschleunigungs-Beträgen.
 * Reine Logik ohne Android-Abhängigkeit, damit voll unit-testbar.
 *
 * Ablauf:
 *  - Ein "Chop" wird erkannt, wenn der Betrag den Peak-Schwellwert übersteigt,
 *    nachdem er zuvor unter die Ruhe-Schranke gefallen war (steigende Flanke).
 *  - Zwei Chops mit einem Abstand im erlaubten Fenster ergeben einen Doppel-Chop.
 *  - Nach dem Auslösen sperrt ein Cooldown weitere Auslöser kurzzeitig.
 */
class ChopDetector(sensitivity: Int = 5) {

    private var peakThreshold = 0f
    private var restThreshold = 0f

    private val minGapNanos = 120_000_000L  // 120 ms: schneller wäre eine Bewegung
    private val maxGapNanos = 700_000_000L  // 700 ms: langsamer zählt als getrennt
    private val cooldownNanos = 800_000_000L // 800 ms Sperre nach Auslösen

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
        // level 1 -> 25 m/s², level 10 -> 10 m/s² (linear interpoliert)
        peakThreshold = 25f - (l - 1) * (15f / 9f)
        restThreshold = peakThreshold * 0.4f
    }

    /** Liefert true, sobald ein vollständiger Doppel-Chop erkannt wurde. */
    fun onSample(timestampNanos: Long, magnitude: Float): Boolean {
        if (!armed) {
            if (magnitude < restThreshold) armed = true
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
            gap in minGapNanos..maxGapNanos -> {
                hasFirstChop = false
                cooldownUntilNanos = now + cooldownNanos
                true
            }
            gap > maxGapNanos -> {
                // zu langsam: dieser Chop startet ein neues Paar
                firstChopNanos = now
                false
            }
            else -> false // zu schnell: zweiten Peak ignorieren, ersten behalten
        }
    }
}
