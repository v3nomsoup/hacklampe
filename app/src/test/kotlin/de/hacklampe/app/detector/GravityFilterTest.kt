package de.hacklampe.app.detector

import org.junit.Assert.assertTrue
import org.junit.Test

class GravityFilterTest {

    @Test
    fun steadyGravityYieldsNearZero() {
        val f = GravityFilter()
        var m = 1f
        repeat(200) { m = f.linearMagnitude(0f, 0f, 9.81f) }
        assertTrue("Bei konstanter Schwerkraft sollte ~0 herauskommen, war $m", m < 0.5f)
    }

    @Test
    fun suddenAccelerationProducesSignal() {
        val f = GravityFilter()
        repeat(100) { f.linearMagnitude(0f, 0f, 9.81f) } // einschwingen
        val m = f.linearMagnitude(0f, 0f, 40f) // plötzlicher Schlag
        assertTrue("Plötzliche Beschleunigung sollte ein Signal liefern, war $m", m > 10f)
    }

    @Test
    fun orientationChangeAloneDoesNotProduceLargeSignal() {
        // Telefon liegt erst flach (Schwerkraft auf z), dann aufrecht (auf y).
        val f = GravityFilter()
        repeat(200) { f.linearMagnitude(0f, 0f, 9.81f) }
        // Langsames Kippen: Schwerkraft wandert über viele Samples nach y.
        var m = 0f
        repeat(200) { i ->
            val frac = i / 200f
            m = f.linearMagnitude(0f, 9.81f * frac, 9.81f * (1 - frac))
        }
        assertTrue("Langsames Kippen sollte keinen großen Ausschlag erzeugen, war $m", m < 2f)
    }
}
