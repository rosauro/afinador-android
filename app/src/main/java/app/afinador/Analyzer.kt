package app.afinador

/** Resultado del análisis más reciente. [freq] null = no suena nada afinable; [level] es el RMS (0..1). */
data class Reading(val freq: Float?, val confidence: Float, val level: Float)

/**
 * Análisis continuo: ventana deslizante de [WINDOW] muestras, YIN cada [HOP] muestras nuevas,
 * mediana de 5 lecturas y retención de la nota unos instantes para no parpadear en ataques y vibrato.
 */
class Analyzer(private val sampleRate: Float) {
    companion object {
        const val WINDOW = 4096
        const val HOP = 1024
        const val MEDIAN_LEN = 5
        const val HOLD_HOPS = 3
        const val MIN_CONFIDENCE = 0.85f
    }

    private val window = FloatArray(WINDOW)
    private var filled = 0
    private val smoother = MedianSmoother(MEDIAN_LEN)
    private var misses = 0
    private var last: Float? = null

    /** Añade [n] muestras mono (-1..1) y devuelve la lectura actual. */
    fun push(chunk: FloatArray, n: Int): Reading {
        require(n in 0..WINDOW)
        System.arraycopy(window, n, window, 0, WINDOW - n)
        System.arraycopy(chunk, 0, window, WINDOW - n, n)
        filled = minOf(filled + n, WINDOW)
        val level = Yin.rms(chunk, n)
        if (filled < WINDOW) return Reading(null, 0f, level)

        val p = Yin.detect(window, sampleRate)
        if (p != null && p.confidence >= MIN_CONFIDENCE) {
            misses = 0
            last = smoother.push(p.freq)
            return Reading(last, p.confidence, level)
        }
        if (++misses > HOLD_HOPS) { smoother.clear(); last = null }
        return Reading(last, 0f, level)
    }
}
