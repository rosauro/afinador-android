package app.afinador

import android.content.Context

/** Ajustes globales. Añade aquí los de la app; idioma y actualizaciones ya vienen resueltos. */
class Settings(ctx: Context) {
    private val p = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** Idioma de la interfaz: «es» (por defecto) o «en». No sigue al idioma del móvil. */
    var lang: String
        get() = p.getString("lang", Lang.ES) ?: Lang.ES
        set(v) { p.edit().putString("lang", v).apply() }

    /** Buscar actualizaciones en GitHub automáticamente. */
    var autoUpdate: Boolean
        get() = p.getBoolean("autoUpdate", true)
        set(v) { p.edit().putBoolean("autoUpdate", v).apply() }

    var lastCheck: Long
        get() = p.getLong("lastCheck", 0L)
        set(v) { p.edit().putLong("lastCheck", v).apply() }
}
