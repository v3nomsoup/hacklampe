package de.hacklampe.app.detector

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalibrationAnalyzerTest {

    private val gate = 25f

    /** Speist einen vollständigen Schlag ein: über dem Gate, dann der Peak, dann darunter. */
    private fun feedStroke(a: CalibrationAnalyzer, peak: Float) {
        a.onSample(gate + 5f)
        a.onSample(peak)
        a.onSample(0f)
    }

    @Test
    fun noDataReturnsNull() {
        val a = CalibrationAnalyzer()
        assertNull(a.result())
        assertFalse(a.hasEnoughData())
        assertEquals(0, a.strokeCount)
    }

    @Test
    fun countsCompletedStrokes() {
        val a = CalibrationAnalyzer()
        feedStroke(a, 100f)
        feedStroke(a, 120f)
        feedStroke(a, 110f)
        feedStroke(a, 130f)
        assertEquals(4, a.strokeCount)
        assertTrue(a.hasEnoughData())
    }

    @Test
    fun strokeInProgressCountsButNeedsCompletion() {
        val a = CalibrationAnalyzer()
        feedStroke(a, 100f)
        feedStroke(a, 120f)
        feedStroke(a, 110f)
        // Vierter Schlag beginnt, fällt aber nie unter das Gate -> nicht abgeschlossen.
        a.onSample(gate + 5f)
        a.onSample(130f)
        assertEquals(4, a.strokeCount)
        assertFalse(a.hasEnoughData())
        assertNull(a.result())
    }

    @Test
    fun computesMedianAndThresholds() {
        val a = CalibrationAnalyzer()
        feedStroke(a, 100f)
        feedStroke(a, 200f)
        feedStroke(a, 110f)
        feedStroke(a, 130f)
        val r = a.result()
        assertNotNull(r)
        r!!
        assertEquals(130f, r.medianPeak, 0.01f)
        assertEquals(52f, r.peakThreshold, 0.01f)
        assertEquals(31.2f, r.valleyThreshold, 0.01f)
        assertEquals(4, r.strokeCount)
    }

    @Test
    fun clampsLowPeaks() {
        val a = CalibrationAnalyzer()
        feedStroke(a, 30f)
        feedStroke(a, 30f)
        feedStroke(a, 30f)
        feedStroke(a, 30f)
        val r = a.result()
        assertNotNull(r)
        r!!
        assertEquals(30f, r.medianPeak, 0.01f)
        assertEquals(18f, r.peakThreshold, 0.01f)
        assertEquals(10.8f, r.valleyThreshold, 0.01f)
    }

    @Test
    fun clampsHighPeaks() {
        val a = CalibrationAnalyzer()
        feedStroke(a, 300f)
        feedStroke(a, 300f)
        feedStroke(a, 300f)
        feedStroke(a, 300f)
        val r = a.result()
        assertNotNull(r)
        r!!
        assertEquals(300f, r.medianPeak, 0.01f)
        assertEquals(75f, r.peakThreshold, 0.01f)
        assertEquals(45f, r.valleyThreshold, 0.01f)
    }

    @Test
    fun gateSeparatesStrokes() {
        val a = CalibrationAnalyzer()
        // Ein einziger zusammenhängender Lauf über dem Gate (steigend/fallend),
        // nie unter das Gate, dann darunter -> EIN Schlag.
        a.onSample(gate + 1f)
        a.onSample(100f)
        a.onSample(60f)
        a.onSample(120f)
        a.onSample(gate + 2f)
        a.onSample(0f)
        assertEquals(1, a.strokeCount)
    }
}
