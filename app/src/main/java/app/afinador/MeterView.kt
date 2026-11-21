package app.afinador

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

/** Medidor horizontal de ±50 cents con marcas cada 10 y una aguja que cambia de color según la desviación. */
class MeterView(c: Context) : View(c) {
    /** Desviación en cents, o null si no hay nota (la aguja se oculta). */
    var cents: Float? = null
        set(v) { field = v; invalidate() }

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val track = RectF()

    private fun zoneColor(z: Int) = when (z) { 0 -> 0xFF7FB77E.toInt(); 1 -> 0xFFE0A458.toInt(); else -> 0xFFD96A5E.toInt() }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val pad = Ui.dpf(context, 12f)
        val left = pad; val right = w - pad
        val midY = h * 0.55f
        fun x(c: Float) = left + (c + 50f) / 100f * (right - left)

        p.style = Paint.Style.FILL
        p.color = Ui.SURFACE2
        track.set(left, midY - Ui.dpf(context, 3f), right, midY + Ui.dpf(context, 3f))
        canvas.drawRoundRect(track, 6f, 6f, p)
        // zona de afinado (±5 cents)
        p.color = 0x667FB77E
        track.set(x(-5f), midY - Ui.dpf(context, 3f), x(5f), midY + Ui.dpf(context, 3f))
        canvas.drawRect(track, p)

        p.strokeWidth = Ui.dpf(context, 2f)
        for (c in -50..50 step 10) {
            val big = c == 0
            p.color = if (big) Ui.GOLD else Ui.MUTED
            val len = Ui.dpf(context, if (big) 22f else 12f)
            canvas.drawLine(x(c.toFloat()), midY - len, x(c.toFloat()), midY + len, p)
        }
        val v = cents ?: return
        val cc = v.coerceIn(-50f, 50f)
        p.color = zoneColor(Notes.zone(v))
        p.strokeWidth = Ui.dpf(context, 5f)
        p.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(x(cc), midY - Ui.dpf(context, 30f), x(cc), midY + Ui.dpf(context, 30f), p)
    }

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        setMeasuredDimension(getDefaultSize(0, wSpec), Ui.dp(context, 90))
    }
}
