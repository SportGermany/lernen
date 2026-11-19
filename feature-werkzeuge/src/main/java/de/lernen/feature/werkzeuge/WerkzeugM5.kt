package de.lernen.feature.werkzeuge

import androidx.compose.runtime.Composable
import de.lernen.core.design.komponenten.KommtSpaeter

/** Platzhalter bis Meilenstein 5. */
@Composable
internal fun WerkzeugM5(w: Werkzeug, eingabe: String?) {
    KommtSpaeter("${w.titel} kommt in Meilenstein 5", "Dieses Werkzeug wird mit dem Mathe- und Physik-Update freigeschaltet.")
}
