package app.afinador

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

/** Instrumento: fija la transposición de la nota mostrada. */
enum class Instrument(val id: String, val labelRes: Int, val transposition: Int) {
    /** Trompeta en Si♭: la nota escrita suena un tono (2 semitonos) más grave, así que se suma 2. */
    TRUMPET_BB("trumpet", R.string.main_instrument_trumpet, 2),
    VIOLIN("violin", R.string.main_instrument_violin, 0),
    CHROMATIC("chromatic", R.string.main_instrument_chromatic, 0);

    companion object {
        fun fromId(id: String?) = values().firstOrNull { it.id == id } ?: TRUMPET_BB
    }
}

/** Nota mostrada ([midi] ya transpuesto) y desviación en cents, en [-50, 50]. */
data class NoteInfo(val midi: Int, val cents: Float) {
    /** Índice 0–11 del nombre (Do = 0). */
    val nameIndex: Int get() = Math.floorMod(midi, 12)
    /** Octava en notación científica (La4 = MIDI 69). */
    val octave: Int get() = Math.floorDiv(midi, 12) - 1

    fun label(names: Array<String>) = names[nameIndex] + octave
}

object Notes {
    const val DEFAULT_A4 = 440
    const val MIN_A4 = 415
    const val MAX_A4 = 466

    /** Cuerdas al aire del violín (Sol3, Re4, La4, Mi5) como número MIDI. */
    val VIOLIN_STRINGS = intArrayOf(55, 62, 69, 76)

    fun clampA4(v: Int) = v.coerceIn(MIN_A4, MAX_A4)

    private fun midiFloat(freq: Float, a4: Float): Double = 69.0 + 12.0 * ln(freq.toDouble() / a4) / ln(2.0)

    /** Frecuencia sonora → nota a mostrar (con [transposition] semitonos) y cents. */
    fun freqToNote(freq: Float, a4: Float, transposition: Int): NoteInfo {
        val m = midiFloat(freq, a4)
        val nearest = Math.round(m).toDouble()
        return NoteInfo(nearest.toInt() + transposition, ((m - nearest) * 100.0).toFloat())
    }

    fun midiToFreq(midi: Int, a4: Float): Float = a4 * 2f.pow((midi - 69) / 12f)

    /** Índice de la cuerda del violín más cercana a [freq]. */
    fun nearestViolinString(freq: Float, a4: Float): Int {
        val m = midiFloat(freq, a4)
        return VIOLIN_STRINGS.indices.minByOrNull { abs(m - VIOLIN_STRINGS[it]) } ?: 0
    }

    /** Zona de color de la aguja: 0 = afinado (≤5 cents), 1 = cerca (≤15), 2 = desafinado. */
    fun zone(cents: Float): Int = when {
        abs(cents) <= 5f -> 0
        abs(cents) <= 15f -> 1
        else -> 2
    }
}
