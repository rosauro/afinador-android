package app.afinador

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.abs
import kotlin.math.pow

class NotesTest {
    private val names = arrayOf("Do", "Do♯", "Re", "Re♯", "Mi", "Fa", "Fa♯", "Sol", "Sol♯", "La", "La♯", "Si")
    private fun label(f: Float, a4: Float = 440f, t: Int = 0) = Notes.freqToNote(f, a4, t).label(names)

    @Test fun la4EsCeroCents() {
        val n = Notes.freqToNote(440f, 440f, 0)
        assertEquals("La4", n.label(names)); assertEquals(0f, n.cents, 0.01f)
    }

    @Test fun do4() = assertEquals("Do4", label(261.6256f))
    @Test fun sostenidos() { assertEquals("Do♯4", label(277.18f)); assertEquals("Fa♯4", label(369.99f)) }

    @Test fun signoYRangoDeCents() {
        assertEquals(25f, Notes.freqToNote(440f * 2f.pow(25f / 1200f), 440f, 0).cents, 0.1f)
        assertEquals(-30f, Notes.freqToNote(440f * 2f.pow(-30f / 1200f), 440f, 0).cents, 0.1f)
        val n = Notes.freqToNote(440f * 2f.pow(60f / 1200f), 440f, 0)
        assertEquals("La♯4", n.label(names)); assertEquals(-40f, n.cents, 0.1f)
    }

    @Test fun diapasonPersonalizado() {
        assertEquals("La4", label(415f, 415f))
        assertEquals("Sol♯4", label(415f, 440f))
    }

    @Test fun trompetaEscribeDoCuandoSuenaSib() {
        val n = Notes.freqToNote(Notes.midiToFreq(58, 440f), 440f, Instrument.TRUMPET_BB.transposition)
        assertEquals("Do4", n.label(names)); assertEquals(0f, n.cents, 0.01f)
    }

    @Test fun violinYCromaticoNoTranspone() {
        assertEquals(0, Instrument.VIOLIN.transposition); assertEquals(0, Instrument.CHROMATIC.transposition)
        assertEquals("La♯3", label(Notes.midiToFreq(58, 440f)))
    }

    @Test fun cuerdaMasCercana() {
        assertEquals(0, Notes.nearestViolinString(196f, 440f))
        assertEquals(1, Notes.nearestViolinString(300f, 440f))
        assertEquals(2, Notes.nearestViolinString(440f, 440f))
        assertEquals(3, Notes.nearestViolinString(660f, 440f))
    }

    @Test fun zonas() {
        assertEquals(0, Notes.zone(-5f)); assertEquals(1, Notes.zone(12f)); assertEquals(2, Notes.zone(-30f))
    }

    @Test fun idaYVuelta() {
        assertEquals(440f, Notes.midiToFreq(69, 440f), 1e-3f)
        assertEquals(220f, Notes.midiToFreq(57, 440f), 1e-2f)
        assertEquals(true, abs(Notes.clampA4(100) - 415) == 0 && Notes.clampA4(900) == 466)
    }
}
