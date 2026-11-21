package app.afinador

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sin

class YinTest {
    private val sr = 44100f
    private fun sine(freq: Float, n: Int, amp: Float) = FloatArray(n) { amp * sin(2.0 * PI * freq * it / sr).toFloat() }

    private fun assertDetects(freq: Float) {
        val p = Yin.detect(sine(freq, 4096, 0.5f), sr)
        assertNotNull("sin tono a $freq Hz", p)
        val cents = 1200.0 * ln(p!!.freq / freq.toDouble()) / ln(2.0)
        assertTrue("$freq detectado como ${p.freq} ($cents cents)", abs(cents) < 2.0)
        assertTrue(p.confidence > 0.9f)
    }

    @Test fun detecta220() = assertDetects(220f)
    @Test fun detecta440() = assertDetects(440f)
    @Test fun detecta932() = assertDetects(932f)

    @Test fun detectaConArmonicos() {
        val s = FloatArray(4096) {
            val t = it / sr
            (0.3 * sin(2 * PI * 330 * t) + 0.4 * sin(2 * PI * 660 * t) + 0.3 * sin(2 * PI * 990 * t)).toFloat()
        }
        assertEquals(330f, Yin.detect(s, sr)!!.freq, 1.5f)
    }

    @Test fun silencioDaNull() {
        assertNull(Yin.detect(FloatArray(4096), sr))
        assertNull(Yin.detect(sine(440f, 4096, 0.001f), sr))
    }

    @Test fun ruidoDaNull() {
        var x = 12345
        val s = FloatArray(4096) { x = x * 1664525 + 1013904223; ((x ushr 16) / 32768f - 1f) * 0.5f }
        assertNull(Yin.detect(s, sr))
    }

    @Test fun bufferCortoDaNull() = assertNull(Yin.detect(sine(440f, 256, 0.5f), sr))

    @Test fun medianaIgnoraAtipicos() {
        val m = MedianSmoother(5)
        for (f in listOf(440f, 440f, 880f, 440f)) m.push(f)
        assertEquals(440f, m.push(441f), 0f)
    }

    @Test fun analizadorSigueUnTonoYLoSueltaTrasSilencio() {
        val a = Analyzer(sr)
        val tone = sine(440f, Analyzer.HOP * 20, 0.5f)
        var r = Reading(null, 0f, 0f)
        for (i in 0 until 20) r = a.push(tone.copyOfRange(i * Analyzer.HOP, (i + 1) * Analyzer.HOP), Analyzer.HOP)
        assertEquals(440f, r.freq!!, 2f)
        val quiet = FloatArray(Analyzer.HOP)
        repeat(Analyzer.WINDOW / Analyzer.HOP + Analyzer.HOLD_HOPS + 1) { r = a.push(quiet, Analyzer.HOP) }
        assertNull(r.freq)
    }
}
