package de.lernen.feature.werkzeuge

import androidx.compose.runtime.Composable

@Composable
internal fun WerkzeugM5(w: Werkzeug, eingabe: String?) {
    when (w) {
        Werkzeug.MITRECHNEN -> MitrechnenWerkzeug()
        Werkzeug.TASCHENRECHNER -> Taschenrechner()
        Werkzeug.FORMELN -> Formelsammlung()
        Werkzeug.EINHEITEN -> Umrechner()
        Werkzeug.PLOTTER -> Plotter(eingabe)
        else -> Unit
    }
}
