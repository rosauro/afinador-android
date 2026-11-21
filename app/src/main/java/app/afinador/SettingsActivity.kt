package app.afinador

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.Toast

/** Ajustes: idioma, fiabilidad (batería, notificaciones), actualizaciones y acerca de. */
class SettingsActivity : Activity() {
    private lateinit var settings: Settings
    private lateinit var langCard: LinearLayout
    private lateinit var reliabilityCard: LinearLayout
    private lateinit var updatesCard: LinearLayout
    private var updateStatus: String? = null

    override fun attachBaseContext(base: Context) { super.attachBaseContext(Lang.wrap(base)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val c = this
        settings = Settings(c)
        val root = LinearLayout(c).apply { orientation = LinearLayout.VERTICAL }
        root.addView(Ui.text(c, getString(R.string.settings_title), 28f, Ui.TEXT, 0), Ui.lp(c, t = 8, b = 8))

        fun section(label: Int, card: LinearLayout) {
            root.addView(Ui.text(c, getString(label), 12f, Ui.MUTED), Ui.lp(c, t = 28, b = 10, l = 4))
            root.addView(card, Ui.lp(c))
        }
        langCard = Ui.card(c); section(R.string.settings_section_language, langCard); renderLanguage()
        reliabilityCard = Ui.card(c); section(R.string.settings_section_reliability, reliabilityCard)
        updatesCard = Ui.card(c); section(R.string.settings_section_updates, updatesCard)

        val version = runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull() ?: "?"
        section(R.string.settings_section_about, Ui.card(c).apply {
            addView(Ui.text(c, getString(R.string.app_name), 22f, Ui.TEXT, 0))
            addView(Ui.text(c, getString(R.string.settings_version, version), 14f, Ui.MUTED), Ui.lp(c, t = 4))
            addView(Ui.text(c, getString(R.string.settings_about_text), 15f).apply { setLineSpacing(0f, 1.3f) }, Ui.lp(c, t = 16))
        })

        val page = LinearLayout(c).apply { orientation = LinearLayout.VERTICAL }
        page.addView(ScrollView(c).apply { addView(root) }, LinearLayout.LayoutParams(Ui.MATCH, 0, 1f))
        page.addView(Ui.tabBar(c, 1), Ui.lp(c, t = 8))
        Ui.page(c, page)
        setContentView(page)
    }

    override fun onResume() {
        super.onResume()
        Updater.userInitiated = true
        renderReliability()
        renderUpdates()
    }

    override fun onPause() { Updater.userInitiated = false; super.onPause() }

    /** Dos pastillas; al cambiar de idioma guarda y recrea la Activity (L-I18N-2: recrear TODAS las pantallas abiertas). */
    private fun renderLanguage() {
        val c = this
        langCard.removeAllViews()
        val row = LinearLayout(c).apply { orientation = LinearLayout.HORIZONTAL }
        for ((name, code) in listOf("Español" to Lang.ES, "English" to Lang.EN)) {
            row.addView(Ui.chip(c, name, settings.lang == code) {
                if (settings.lang != code) { settings.lang = code; recreate() }
            }.apply { maxLines = 1; minimumHeight = Ui.dp(c, 40) },
                LinearLayout.LayoutParams(0, Ui.WRAP, 1f).apply { setMargins(Ui.dp(c, 4), 0, Ui.dp(c, 4), 0) })
        }
        langCard.addView(row, Ui.lp(c))
    }

    private fun renderReliability() {
        val c = this
        reliabilityCard.removeAllViews()
        val batteryOk = getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)
        reliabilityCard.addView(row(batteryOk, getString(R.string.settings_battery_title),
            getString(if (batteryOk) R.string.settings_battery_ok else R.string.settings_battery_missing),
            getString(R.string.settings_btn_allow)) {
            launch(Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")),
                Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }, Ui.lp(c))
        val notifOk = getSystemService(NotificationManager::class.java).areNotificationsEnabled()
        reliabilityCard.addView(row(notifOk, getString(R.string.settings_notif_title),
            getString(if (notifOk) R.string.settings_notif_ok else R.string.settings_notif_missing),
            getString(R.string.settings_btn_allow)) {
            startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName))
        }, Ui.lp(c, t = 20))
        reliabilityCard.addView(Ui.text(c, getString(R.string.settings_oem_note), 13f, Ui.MUTED)
            .apply { setLineSpacing(0f, 1.3f) }, Ui.lp(c, t = 20))
    }

    private fun renderUpdates() {
        val c = this
        updatesCard.removeAllViews()
        val rel = Updater.available
        val installed = Updater.currentVersion(c)
        updatesCard.addView(LinearLayout(c).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(Ui.text(c, getString(R.string.settings_auto_update), 16f, Ui.TEXT, 2), LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
            addView(Switch(c).apply {
                isChecked = settings.autoUpdate
                val st = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked))
                thumbTintList = ColorStateList(st, intArrayOf(Ui.GOLD, Ui.MUTED))
                trackTintList = ColorStateList(st, intArrayOf(Ui.GOLD, Ui.SURFACE2))
                setOnCheckedChangeListener { _, on -> settings.autoUpdate = on; if (on) UpdateJob.schedule(c) else UpdateJob.cancel(c) }
            })
        }, Ui.lp(c))
        updatesCard.addView(Ui.text(c, getString(R.string.settings_auto_update_hint), 13f, Ui.MUTED)
            .apply { setLineSpacing(0f, 1.3f) }, Ui.lp(c, t = 6))
        val state = updateStatus ?: when {
            rel != null -> getString(R.string.settings_update_available, rel.version)
            settings.lastCheck > 0 -> getString(R.string.settings_update_latest)
            else -> null
        }
        updatesCard.addView(Ui.text(c, getString(R.string.settings_installed_version, installed), 14f, Ui.MUTED), Ui.lp(c, t = 20))
        if (state != null) updatesCard.addView(Ui.text(c, state, 15f), Ui.lp(c, t = 4))
        updatesCard.addView(Ui.secondaryButton(c, getString(R.string.settings_btn_check)) { checkNow() }, Ui.lp(c, Ui.WRAP, Ui.WRAP, t = 16))
        if (rel != null) updatesCard.addView(Ui.primaryButton(c, getString(R.string.settings_btn_update, rel.version)) { installNow(rel) }, Ui.lp(c, t = 12))
        val canInstall = Updater.canInstall(c)
        updatesCard.addView(row(canInstall, getString(R.string.settings_unknown_title),
            getString(if (canInstall) R.string.settings_unknown_ok else R.string.settings_unknown_missing),
            getString(R.string.settings_btn_allow)) {
            startActivity(Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
        }, Ui.lp(c, t = 20))
    }

    private fun checkNow() {
        val c = this
        if (Updater.busy) return
        Updater.busy = true
        updateStatus = getString(R.string.settings_update_checking)
        renderUpdates()
        Thread {
            val error = try { Updater.check(c); null } catch (e: Exception) { e.message ?: "?" }
            Updater.busy = false
            runOnUiThread { updateStatus = error?.let { getString(R.string.settings_update_error, it) }; renderUpdates() }
        }.start()
    }

    private fun installNow(rel: Updater.Release) {
        val c = this
        if (Updater.busy) return
        if (!Updater.canInstall(c)) {
            startActivity(Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
            Toast.makeText(c, R.string.settings_toast_allow_unknown, Toast.LENGTH_LONG).show()
            return
        }
        if (Updater.sessionActive(c)) { Toast.makeText(c, R.string.settings_toast_busy, Toast.LENGTH_LONG).show(); return }
        Updater.busy = true
        updateStatus = getString(R.string.settings_update_downloading)
        renderUpdates()
        Thread {
            var error: String? = null
            try {
                val file = Updater.download(c, rel)
                runOnUiThread { updateStatus = getString(R.string.settings_update_installing); renderUpdates() }
                Updater.install(c, file)
            } catch (e: Exception) { error = e.message ?: "?" }
            Updater.busy = false
            runOnUiThread { updateStatus = error?.let { getString(R.string.settings_update_error, it) }; renderUpdates() }
        }.start()
    }

    /** Fila con punto de estado (verde = bien, ámbar = falta), explicación y botón solo si falta. */
    private fun row(ok: Boolean, title: String, detail: String, button: String, onAction: () -> Unit): LinearLayout {
        val c = this
        val dot = View(c).apply { background = Ui.shape(c, if (ok) 0xFF7FB77E.toInt() else 0xFFE0A458.toInt(), 5) }
        val col = LinearLayout(c).apply { orientation = LinearLayout.VERTICAL }
        col.addView(Ui.text(c, title, 16f, Ui.TEXT, 2))
        col.addView(Ui.text(c, detail, 13f, Ui.MUTED), Ui.lp(c, t = 4))
        if (!ok) col.addView(Ui.secondaryButton(c, button, onAction).apply { textSize = 14f; minHeight = Ui.dp(c, 40) }, Ui.lp(c, Ui.WRAP, Ui.WRAP, t = 10))
        return LinearLayout(c).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(dot, LinearLayout.LayoutParams(Ui.dp(c, 10), Ui.dp(c, 10)).apply { setMargins(0, Ui.dp(c, 6), Ui.dp(c, 12), 0) })
            addView(col, LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
        }
    }

    private fun launch(primary: Intent, fallback: Intent) { try { startActivity(primary) } catch (e: Exception) { startActivity(fallback) } }
}
