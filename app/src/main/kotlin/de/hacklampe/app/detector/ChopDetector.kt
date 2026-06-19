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

    private var armed = true
    private var hasFirstChop = false
    private var firstChopNanos = 0L
    private var cooldownUntilNanos = Long.MIN_VALUE

    /** Diagnose-Ereignisse für Kalibrierung/Logging. */
    enum class Event { FIRST_CHOP, DOUBLE_CHOP, TOO_FAST, TOO_SLOW_RESTART, IN_COOLDOWN }

    /**
     * Optionaler Beobachter für Diagnose. Wird an jedem Entscheidungspunkt aufgerufen.
     * Standard null -> kein Einfluss auf bestehendes Verhalten/Tests.
     */
    var observer: ((event: Event, timestampNanos: Long, magnitude: Float, gapNanos: Long) -> Unit)? = null

    init {
        setSensitivity(sensitivity)
    }

    /** Empfindlichkeit 1 (unempfindlich) .. 10 (sehr empfindlich). */
    fun setSensitivity(level: Int) {
        val l = level.coerceIn(1, 10)
        // level 1 -> 25 m/s², level 10 -> 10 m/s² (linear interpoliert)
        peakThreshold = MAX_PEAK_THRESHOLD - (l - 1) * (PEAK_THRESHOLD_SPAN / 9f)
        restThreshold = peakThreshold * REST_RATIO
    }

    /**
     * Liefert true, sobald ein vollständiger Doppel-Chop erkannt wurde.
     * Erwartet monoton nicht-fallende Zeitstempel (z. B. SensorEvent.timestamp).
     */
    fun onSample(timestampNanos: Long, magnitude: Float): Boolean {
        if (!armed) {
            if (magnitude < restThreshold) armed = true
            return false
        }
        if (magnitude < peakThreshold) return false
        // Steigende Flanke über den Schwellwert -> ein Chop
        armed = false
        return registerChop(timestampNanos, magnitude)
    }

    private fun registerChop(now: Long, magnitude: Float): Boolean {
        if (now < cooldownUntilNanos) {
            hasFirstChop = false
            observer?.invoke(Event.IN_COOLDOWN, now, magnitude, 0L)
            return false
        }
        if (!hasFirstChop) {
            hasFirstChop = true
            firstChopNanos = now
            observer?.invoke(Event.FIRST_CHOP, now, magnitude, 0L)
            return false
        }
        val gap = now - firstChopNanos
        return when {
            gap in MIN_GAP_NANOS..MAX_GAP_NANOS -> {
                hasFirstChop = false
                cooldownUntilNanos = now + COOLDOWN_NANOS
                observer?.invoke(Event.DOUBLE_CHOP, now, magnitude, gap)
                true
            }
            gap > MAX_GAP_NANOS -> {
                // zu langsam: dieser Chop startet ein neues Paar
                firstChopNanos = now
                observer?.invoke(Event.TOO_SLOW_RESTART, now, magnitude, gap)
                false
            }
            else -> {
                // zu schnell: zweiten Peak ignorieren, ersten behalten
                observer?.invoke(Event.TOO_FAST, now, magnitude, gap)
                false
            }
        }
    }

    private companion object {
        /** Peak-Schwellwert bei Empfindlichkeit 1 (unempfindlichste Stufe), m/s². */
        const val MAX_PEAK_THRESHOLD = 25f
        /** Spanne über Level 1..10: 25 - 15 = 10 m/s² bei höchster Empfindlichkeit. */
        const val PEAK_THRESHOLD_SPAN = 15f
        /** Ruhe-/Wieder-Scharf-Schranke als Anteil des Peak-Schwellwerts. */
        const val REST_RATIO = 0.4f

        const val MIN_GAP_NANOS = 120_000_000L   // 120 ms: schneller wäre eine Bewegung
        const val MAX_GAP_NANOS = 700_000_000L   // 700 ms: langsamer zählt als getrennt
        const val COOLDOWN_NANOS = 800_000_000L  // 800 ms Sperre nach Auslösen
    }
}
