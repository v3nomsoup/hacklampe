package de.hacklampe.app.detector

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChopDetectorTest {

    private val ms = 1_000_000L // Nanosekunden pro Millisekunde

    // Bei Empfindlichkeit 5 liegt der Peak-Schwellwert ~18.3 m/s², die
    // Wieder-Scharf-Schranke ~7.3 m/s². 30f gilt als Peak, 0f als Ruhe.
    private val peak = 30f
    private val rest = 0f

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
}
