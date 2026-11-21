package app.afinador

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView

/** Pantalla de inicio. SUSTITÚYELA por la pantalla principal de la app (ver docs en CLAUDE.md). */
class MainActivity : Activity() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(Lang.wrap(base))   // LECCIÓN L-I18N-1: toda Activity y servicio
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val c = this
        val root = LinearLayout(c).apply { orientation = LinearLayout.VERTICAL }
        root.addView(Ui.text(c, getString(R.string.main_title), 28f, Ui.TEXT, 0), Ui.lp(c, t = 8, b = 8))
        root.addView(Ui.card(c).apply {
            addView(Ui.text(c, getString(R.string.main_hello), 16f, Ui.TEXT))
        }, Ui.lp(c, t = 16))

        val page = LinearLayout(c).apply { orientation = LinearLayout.VERTICAL }
        page.addView(ScrollView(c).apply { addView(root) }, LinearLayout.LayoutParams(Ui.MATCH, 0, 1f))
        page.addView(Ui.tabBar(c, 0), Ui.lp(c, t = 8))
        Ui.page(c, page)
        setContentView(page)
    }
}
