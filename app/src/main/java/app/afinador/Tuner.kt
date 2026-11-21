package app.afinador

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.os.Process

/**
 * Captura del micrófono y análisis en un hilo propio. Solo funciona con la app en primer plano:
 * la Activity llama a [start] en onResume y a [stop] en onPause. Los callbacks llegan al hilo principal.
 */
class Tuner(private val onReading: (Reading) -> Unit, private val onError: () -> Unit) {
    companion object { const val SAMPLE_RATE = 44100 }

    private val main = Handler(Looper.getMainLooper())
    @Volatile private var running = false
    private var thread: Thread? = null

    @SuppressLint("MissingPermission") // el permiso lo comprueba la Activity antes de llamar a start()
    fun start() {
        if (running) return
        running = true
        thread = Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            val min = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val rec = try {
                AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, maxOf(min, Analyzer.WINDOW * 2))
            } catch (e: Exception) { null }
            if (rec == null || rec.state != AudioRecord.STATE_INITIALIZED) {
                rec?.release(); running = false; main.post(onError); return@Thread
            }
            try {
                rec.startRecording()
                val analyzer = Analyzer(SAMPLE_RATE.toFloat())
                val shorts = ShortArray(Analyzer.HOP)
                val floats = FloatArray(Analyzer.HOP)
                while (running) {
                    val n = rec.read(shorts, 0, shorts.size)
                    if (n <= 0) { if (running) { running = false; main.post(onError) }; break }
                    for (i in 0 until n) floats[i] = shorts[i] / 32768f
                    val r = analyzer.push(floats, n)
                    if (running) main.post { if (running) onReading(r) }
                }
            } catch (e: Exception) {
                if (running) { running = false; main.post(onError) }
            } finally {
                runCatching { rec.stop() }
                rec.release()
            }
        }, "afinador-audio").also { it.start() }
    }

    fun stop() {
        running = false
        thread?.join(500)
        thread = null
    }
}
