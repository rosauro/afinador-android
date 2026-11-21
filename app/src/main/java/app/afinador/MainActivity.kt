package app.afinador

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView

/** Pantalla del afinador: nota grande, medidor de cents, instrumento y diapasón. Solo escucha en primer plano. */
class MainActivity : Activity() {
    private lateinit var settings: Settings
    private lateinit var names: Array<String>
    private lateinit var tuner: Tuner

    private lateinit var chipsRow: LinearLayout
    private lateinit var noteView: TextView
    private lateinit var centsView: TextView
    private lateinit var hzView: TextView
    private lateinit var meter: MeterView
    private lateinit var stringsRow: LinearLayout
    private lateinit var level: ProgressBar
    private lateinit var statusCard: LinearLayout
    private lateinit var refValue: TextView
    private lateinit var refLabel: TextView
    private var lastFreq: Float? = null

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(Lang.wrap(base))   // LECCIÓN L-I18N-1: toda Activity y servicio
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val c = this
        settings = Settings(c)
        names = NOTE_RES.map { getString(it) }.toTypedArray()
        tuner = Tuner(::render, ::onAudioError)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val root = LinearLayout(c).apply { orientation = LinearLayout.VERTICAL }
        chipsRow = LinearLayout(c).apply { orientation = LinearLayout.HORIZONTAL }
        root.addView(chipsRow, Ui.lp(c, t = 8))

        noteView = Ui.text(c, getString(R.string.main_no_note), 104f, Ui.MUTED, 0, true)
        root.addView(noteView, Ui.lp(c, t = 28))
        centsView = Ui.text(c, "", 24f, Ui.TEXT, 2, true)
        root.addView(centsView, Ui.lp(c, t = 8))
        hzView = Ui.text(c, "", 15f, Ui.MUTED, 1, true)
        root.addView(hzView, Ui.lp(c, t = 4))

        meter = MeterView(c).apply { contentDescription = getString(R.string.main_meter_desc) }
        root.addView(meter, Ui.lp(c, t = 20))

        stringsRow = LinearLayout(c).apply { orientation = LinearLayout.HORIZONTAL }
        root.addView(stringsRow, Ui.lp(c, t = 16))

        level = ProgressBar(c, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progressTintList = android.content.res.ColorStateList.valueOf(Ui.GOLD)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(Ui.SURFACE2)
            contentDescription = getString(R.string.main_level_desc)
        }
        root.addView(level, Ui.lp(c, h = Ui.dp(c, 6), t = 20))

        statusCard = Ui.card(c)
        root.addView(statusCard, Ui.lp(c, t = 16))

        root.addView(buildReferenceRow(), Ui.lp(c, t = 24, b = 8))

        val page = LinearLayout(c).apply { orientation = LinearLayout.VERTICAL }
        page.addView(ScrollView(c).apply { addView(root) }, LinearLayout.LayoutParams(Ui.MATCH, 0, 1f))
        page.addView(Ui.tabBar(c, 0), Ui.lp(c, t = 8))
        Ui.page(c, page)
        setContentView(page)

        renderChips()
        renderStrings(null)
        renderStatus(null)
    }

    override fun onResume() {
        super.onResume()
        if (hasMic()) startTuner() else if (!settings.micAsked) explainMic() else renderStatus(Status.NO_PERMISSION)
    }

    override fun onPause() {
        tuner.stop()
        showNothing()
        super.onPause()
    }

    private fun hasMic() = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun startTuner() { renderStatus(null); tuner.start() }

    /** Explica para qué se usa el micrófono antes de pedir el permiso del sistema. */
    private fun explainMic() {
        renderStatus(Status.NO_PERMISSION)
        Ui.dialog(this) {
            title(getString(R.string.main_mic_title))
            message(getString(R.string.main_mic_message))
            primary(getString(R.string.main_mic_continue)) { askMic() }
            cancel(getString(R.string.main_mic_cancel))
        }
    }

    private fun askMic() {
        settings.micAsked = true
        requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQ_MIC)
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, perms, results)
        if (code == REQ_MIC) { if (hasMic()) startTuner() else renderStatus(Status.NO_PERMISSION) }
    }

    private fun onAudioError() = renderStatus(Status.AUDIO_ERROR)

    private enum class Status { NO_PERMISSION, AUDIO_ERROR }

    /** Aviso en una tarjeta solo cuando algo falla; con todo bien se oculta. */
    private fun renderStatus(s: Status?) {
        val c = this
        statusCard.removeAllViews()
        if (s == null) { statusCard.visibility = android.view.View.GONE; return }
        statusCard.visibility = android.view.View.VISIBLE
        statusCard.addView(Ui.text(c, getString(if (s == Status.NO_PERMISSION) R.string.main_mic_denied else R.string.main_audio_error), 15f))
        val label = if (s == Status.NO_PERMISSION) R.string.main_mic_allow else R.string.main_retry
        statusCard.addView(Ui.secondaryButton(c, getString(label)) {
            when {
                s == Status.AUDIO_ERROR -> if (hasMic()) startTuner() else askMic()
                shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) || !settings.micAsked -> askMic()
                else -> startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            }
        }, Ui.lp(c, Ui.WRAP, Ui.WRAP, t = 12))
    }

    private fun renderChips() {
        val c = this
        chipsRow.removeAllViews()
        for (inst in Instrument.values()) {
            chipsRow.addView(Ui.chip(c, getString(inst.labelRes), settings.instrument == inst) {
                settings.instrument = inst
                renderChips(); showReading(lastFreq)
            }.apply { maxLines = 1; minimumHeight = Ui.dp(c, 40); gravity = Gravity.CENTER },
                LinearLayout.LayoutParams(0, Ui.WRAP, 1f).apply { setMargins(Ui.dp(c, 3), 0, Ui.dp(c, 3), 0) })
        }
    }

    /** Cuerdas al aire del violín; resalta la más cercana a la nota que suena. */
    private fun renderStrings(freq: Float?) {
        val c = this
        stringsRow.removeAllViews()
        if (settings.instrument != Instrument.VIOLIN) { stringsRow.visibility = android.view.View.GONE; return }
        stringsRow.visibility = android.view.View.VISIBLE
        val near = freq?.let { Notes.nearestViolinString(it, settings.a4.toFloat()) }
        Notes.VIOLIN_STRINGS.forEachIndexed { i, midi ->
            val on = i == near
            val label = NoteInfo(midi, 0f).label(names)
            stringsRow.addView(Ui.text(c, label, 16f, if (on) Ui.BG else Ui.MUTED, 2, true).apply {
                background = Ui.shape(c, if (on) Ui.GOLD else Ui.SURFACE2, 20)
                minHeight = Ui.dp(c, 40); gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(0, Ui.WRAP, 1f).apply { setMargins(Ui.dp(c, 3), 0, Ui.dp(c, 3), 0) })
        }
    }

    private fun buildReferenceRow(): LinearLayout {
        val c = this
        refLabel = Ui.text(c, "", 16f, Ui.TEXT, 2)
        refValue = Ui.text(c, "", 20f, Ui.GOLD, 2, true)
        fun step(label: String, desc: String, d: Int) = Ui.chip(c, label, false) { settings.a4 += d; renderRef(); showReading(lastFreq) }
            .apply { gravity = Gravity.CENTER; contentDescription = desc; minimumWidth = Ui.dp(c, 56) }
        val row = LinearLayout(c).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        row.addView(refLabel, LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
        row.addView(step(getString(R.string.main_minus), getString(R.string.main_minus_desc), -1))
        row.addView(refValue, LinearLayout.LayoutParams(Ui.dp(c, 84), Ui.WRAP))
        row.addView(step(getString(R.string.main_plus), getString(R.string.main_plus_desc), 1))
        renderRef()
        return row
    }

    private fun renderRef() {
        refLabel.text = getString(R.string.main_reference, names[9] + "4")
        refValue.text = getString(R.string.main_hz_value, settings.a4)
    }

    private fun render(r: Reading) {
        level.progress = (minOf(1f, r.level * 8f) * 100).toInt()
        lastFreq = r.freq
        showReading(r.freq)
    }

    private fun showNothing() { lastFreq = null; level.progress = 0; showReading(null) }

    private fun showReading(freq: Float?) {
        renderStrings(freq)
        if (freq == null) {
            noteView.text = getString(R.string.main_no_note); noteView.setTextColor(Ui.MUTED)
            centsView.text = ""; hzView.text = ""; meter.cents = null
            return
        }
        val info = Notes.freqToNote(freq, settings.a4.toFloat(), settings.instrument.transposition)
        noteView.text = info.label(names)
        noteView.setTextColor(when (Notes.zone(info.cents)) { 0 -> GREEN; 1 -> AMBER; else -> Ui.TEXT })
        centsView.text = getString(R.string.main_cents, Math.round(info.cents))
        hzView.text = getString(R.string.main_hz_measured, String.format(Lang.locale(this), "%.1f", freq))
        meter.cents = info.cents
    }

    companion object {
        private const val REQ_MIC = 1
        private const val GREEN = 0xFF7FB77E.toInt()
        private const val AMBER = 0xFFE0A458.toInt()
        private val NOTE_RES = intArrayOf(R.string.note_0, R.string.note_1, R.string.note_2, R.string.note_3,
            R.string.note_4, R.string.note_5, R.string.note_6, R.string.note_7, R.string.note_8, R.string.note_9,
            R.string.note_10, R.string.note_11)
    }
}
