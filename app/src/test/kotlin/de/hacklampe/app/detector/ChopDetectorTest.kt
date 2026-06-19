package de.hacklampe.app.detector

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChopDetectorTest {

    private val ms = 1_000_000L // Nanosekunden pro Millisekunde

    // Bei Empfindlichkeit 5 liegt die Peak-Schwelle ~43.1 m/s², die
    // Wieder-Scharf-Schranke (Tal) ~25.9 m/s². 80f gilt als Schlag,
    // 10f als Tal/Ruhe (unter der Wieder-Scharf-Schranke).
    private val peak = 80f
    private val rest = 10f

    @Test
    fun singleChopDoesNotTrigger() {
        val d = ChopDetector(sensitivity = 5)
        assertFalse(d.onSample(0, peak))
    }

    @Test
    fun doubleChopWithinWindowTriggers() {
        val d = ChopDetector(sensitivity = 5)
        assertFalse(d.onSample(0, peak))          // Chop 1
        assertFalse(d.onSample(50 * ms, rest))    // wieder scharf
        assertTrue(d.onSample(300 * ms, peak))    // Chop 2 -> auslösen
    }

    @Test
    fun twoChopsTooSlowDoNotTrigger() {
        val d = ChopDetector(sensitivity = 5)
        assertFalse(d.onSample(0, peak))
        assertFalse(d.onSample(50 * ms, rest))
        assertFalse(d.onSample(2000 * ms, peak))  // 2 s Abstand > Maximum
    }

    @Test
    fun secondPeakWithoutRearmIsIgnored() {
        val d = ChopDetector(sensitivity = 5)
        assertFalse(d.onSample(0, peak))          // Chop 1, jetzt entschärft
        assertFalse(d.onSample(300 * ms, peak))   // bleibt hoch -> kein Chop 2
    }

    @Test
    fun cooldownSuppressesImmediateRetrigger() {
        val d = ChopDetector(sensitivity = 5)
        assertFalse(d.onSample(0, peak))
        assertFalse(d.onSample(50 * ms, rest))
        assertTrue(d.onSample(300 * ms, peak))    // erster Doppel-Chop
        // Sofort danach: weiterer Doppel-Chop im Cooldown wird unterdrückt
        assertFalse(d.onSample(350 * ms, rest))
        assertFalse(d.onSample(400 * ms, peak))
        assertFalse(d.onSample(450 * ms, rest))
        assertFalse(d.onSample(500 * ms, peak))
    }

    @Test
    fun twoChopsTooFastDoNotTrigger() {
        val d = ChopDetector(sensitivity = 5)
        assertFalse(d.onSample(0, peak))          // Chop 1
        assertFalse(d.onSample(20 * ms, rest))    // wieder scharf
        assertFalse(d.onSample(60 * ms, peak))    // Gap 60 ms < Minimum (100 ms) -> ignorieren
    }

    @Test
    fun gapAtMinBoundaryTriggers() {
        val d = ChopDetector(sensitivity = 5)
        assertFalse(d.onSample(0, peak))
        assertFalse(d.onSample(50 * ms, rest))
        assertTrue(d.onSample(100 * ms, peak))    // genau am Minimum -> auslösen (inklusiv)
    }

    @Test
    fun gapAtMaxBoundaryTriggers() {
        val d = ChopDetector(sensitivity = 5)
        assertFalse(d.onSample(0, peak))
        assertFalse(d.onSample(50 * ms, rest))
        assertTrue(d.onSample(800 * ms, peak))    // genau am Maximum -> auslösen (inklusiv)
    }
}
