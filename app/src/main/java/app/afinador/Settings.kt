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

    private val a = ctx.getSharedPreferences("afinador", Context.MODE_PRIVATE)

    /** Instrumento elegido (por defecto trompeta en Si♭). */
    var instrument: Instrument
        get() = Instrument.fromId(a.getString("instrument", null))
        set(v) { a.edit().putString("instrument", v.id).apply() }

    /** Frecuencia del La4 en Hz (415–466), por defecto 440. */
    var a4: Int
        get() = Notes.clampA4(a.getInt("a4", Notes.DEFAULT_A4))
        set(v) { a.edit().putInt("a4", Notes.clampA4(v)).apply() }

    /** Ya se explicó el permiso de micrófono (la petición del sistema ya se hizo al menos una vez). */
    var micAsked: Boolean
        get() = a.getBoolean("micAsked", false)
        set(v) { a.edit().putBoolean("micAsked", v).apply() }
}
