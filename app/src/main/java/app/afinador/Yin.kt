package app.afinador

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sqrt

/** Lectura de tono: frecuencia y confianza (1 = señal perfectamente periódica). */
data class Pitch(val freq: Float, val confidence: Float)

/**
 * Detección de tono con YIN (de Cheveigné y Kawahara, 2002), implementación propia:
 * función de diferencia, diferencia normalizada (CMNDF), umbral absoluto e interpolación parabólica.
 * Portado del afinador de escritorio (pitch.rs).
 */
object Yin {
    /** Rango de búsqueda: del Sol3 del violín a los agudos de la trompeta. */
    const val MIN_HZ = 150f
    const val MAX_HZ = 2800f
    const val THRESHOLD = 0.15f
    /** Por debajo de este RMS se considera silencio. */
    const val SILENCE_RMS = 0.01f

    fun rms(samples: FloatArray, n: Int = samples.size): Float {
        if (n <= 0) return 0f
        var s = 0.0
        for (i in 0 until n) s += samples[i] * samples[i]
        return sqrt(s / n).toFloat()
    }

    /** Tono fundamental de [samples], o null si hay silencio o no hay un mínimo con confianza. */
    fun detect(samples: FloatArray, sampleRate: Float): Pitch? {
        if (rms(samples) < SILENCE_RMS) return null
        val tauMin = max(floor(sampleRate / MAX_HZ).toInt(), 2)
        val tauMax = ceil(sampleRate / MIN_HZ).toInt()
        val w = samples.size - (tauMax + 1)
        if (w < tauMax || tauMin >= tauMax) return null

        val cmndf = FloatArray(tauMax + 1) { 1f }
        var running = 0f
        for (tau in 1..tauMax) {
            var d = 0f
            for (j in 0 until w) {
                val diff = samples[j] - samples[j + tau]
                d += diff * diff
            }
            running += d
            cmndf[tau] = if (running > 0f) d * tau / running else 1f
        }

        var tau = tauMin
        var found = -1
        while (tau < tauMax) {
            if (cmndf[tau] < THRESHOLD) {
                while (tau + 1 < tauMax && cmndf[tau + 1] < cmndf[tau]) tau++
                found = tau
                break
            }
            tau++
        }
        if (found < 0) return null
        tau = found

        val better = if (tau in 2 until tauMax) {
            val a = cmndf[tau - 1]; val b = cmndf[tau]; val c = cmndf[tau + 1]
            val denom = a - 2f * b + c
            if (abs(denom) > 1e-12f) tau + 0.5f * (a - c) / denom else tau.toFloat()
        } else tau.toFloat()

        val freq = sampleRate / better
        if (freq < MIN_HZ || freq > MAX_HZ) return null
        return Pitch(freq, (1f - cmndf[tau]).coerceIn(0f, 1f))
    }
}

/** Mediana de las últimas [capacity] lecturas: la aguja no tiembla y se descartan saltos de octava aislados. */
class MedianSmoother(capacity: Int) {
    private val cap = max(capacity, 1)
    private val buf = ArrayList<Float>(cap)

    fun push(freq: Float): Float {
        if (buf.size == cap) buf.removeAt(0)
        buf.add(freq)
        return buf.sorted()[buf.size / 2]
    }

    fun clear() = buf.clear()
}
