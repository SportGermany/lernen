package de.lernen.feature.werkzeuge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import de.lernen.core.design.komponenten.Grau
import de.lernen.core.design.komponenten.Karte
import de.lernen.core.design.theme.Lernen
import de.lernen.core.design.theme.LeseSchrift
import kotlinx.serialization.Serializable

@Serializable
data object WerkzeugeRoute

/** Ein einzelnes Werkzeug. [art] siehe [Werkzeug]. [eingabe]: optional vorbelegte Aufgabe (Mitrechnen). */
@Serializable
data class WerkzeugRoute(val art: String, val eingabe: String? = null)

enum class Werkzeug(val schluessel: String, val titel: String, val untertitel: String, val symbol: String, val bereit: Boolean = true) {
    WORTKISTE("wortkiste", "Wortkiste", "Verben, Gefühle, Bilder", "Aa"),
    KORREKTUR("korrektur", "Korrekturzeichen", "Was bedeutet was?", "T"),
    MITRECHNEN("mitrechnen", "Mitrechnen", "Schritte prüfen lassen", "="),
    TASCHENRECHNER("rechner", "Taschenrechner", "wissenschaftlich", "÷×"),
    FORMELN("formeln", "Formelsammlung", "Mathe & Physik", "∑"),
    EINHEITEN("einheiten", "Einheiten", "umrechnen", "⇄"),
    PLOTTER("plotter", "Funktionsplotter", "Graphen zeichnen", "ƒ(x)");

    companion object {
        fun von(s: String) = entries.firstOrNull { it.schluessel == s }
    }
}

fun NavController.oeffneWerkzeug(w: Werkzeug, eingabe: String? = null) = navigate(WerkzeugRoute(w.schluessel, eingabe))

fun NavGraphBuilder.werkzeugeBildschirm(onWerkzeug: (Werkzeug) -> Unit, onZurueck: () -> Unit) {
    composable<WerkzeugeRoute> { WerkzeugeScreen(onWerkzeug) }
    composable<WerkzeugRoute> { e ->
        val r = e.toRoute<WerkzeugRoute>()
        WerkzeugScreen(Werkzeug.von(r.art) ?: Werkzeug.WORTKISTE, r.eingabe, onZurueck)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WerkzeugeScreen(onWerkzeug: (Werkzeug) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Werkzeuge") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 720.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp).padding(bottom = 20.dp)) {
                Grau("Immer verfügbar, unabhängig vom Thema", Modifier.padding(bottom = 10.dp))
                Werkzeug.entries.chunked(2).forEach { paar ->
                    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        paar.forEach { w -> Kachel(w, Modifier.weight(1f)) { onWerkzeug(w) } }
                        if (paar.size == 1) Box(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun Kachel(w: Werkzeug, modifier: Modifier, onClick: () -> Unit) {
    Karte(modifier, onClick = onClick) {
        Text(
            w.symbol, fontFamily = LeseSchrift, fontSize = 24.sp,
            color = if (w == Werkzeug.KORREKTUR) Lernen.farben.schlecht else MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        Text(w.titel, fontWeight = FontWeight.Medium)
        Grau(w.untertitel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WerkzeugScreen(w: Werkzeug, eingabe: String?, onZurueck: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(w.titel) },
            navigationIcon = { IconButton(onClick = onZurueck) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        )
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.widthIn(max = 900.dp).fillMaxWidth().padding(horizontal = 14.dp)) {
                when (w) {
                    Werkzeug.WORTKISTE -> WortkisteWerkzeug()
                    Werkzeug.KORREKTUR -> KorrekturWerkzeug()
                    else -> WerkzeugM5(w, eingabe)
                }
            }
        }
    }
}

