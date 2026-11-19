package de.lernen.feature.werkzeuge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import de.lernen.core.data.model.Formel
import de.lernen.core.data.model.MitrechnenDaten
import de.lernen.core.data.repo.PaketRepository
import de.lernen.core.design.komponenten.AbhakZeile
import de.lernen.core.design.komponenten.BoxArt
import de.lernen.core.design.komponenten.Grau
import de.lernen.core.design.komponenten.HinweisBox
import de.lernen.core.design.komponenten.Karte
import de.lernen.core.design.komponenten.KartenTitel
import de.lernen.core.design.theme.Lernen
import de.lernen.core.design.theme.LeseSchrift
import de.lernen.core.math.Mitrechnen
import de.lernen.core.math.SchrittPruefung
import de.lernen.core.units.Einheiten
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MitrechnenVorgabe(val daten: MitrechnenDaten?, val formel: Formel?, val titel: String?)

@HiltViewModel
class MitrechnenViewModel @Inject constructor(savedStateHandle: SavedStateHandle, pakete: PaketRepository) : ViewModel() {
    private val route = runCatching { savedStateHandle.toRoute<WerkzeugRoute>() }.getOrNull()
    val vorgabe = MutableStateFlow<MitrechnenVorgabe?>(null)
    val eingabe: String? = route?.eingabe

    init {
        viewModelScope.launch {
            val id = route?.paketId
            if (id == null) { vorgabe.value = MitrechnenVorgabe(null, null, null); return@launch }
            val p = pakete.paket(id).first()
            val s = p?.begleiter?.firstOrNull { it.id == route.schrittId }
            val d = s?.mitrechnen
            val fs = pakete.formelsammlung()
            val alle = fs?.gebiete?.flatMap { it.formeln }.orEmpty()
            val formel = d?.formel?.let { fid -> alle.firstOrNull { it.id == fid } }
                ?: d?.gesucht?.let { g -> alle.firstOrNull { f -> f.umgestelltNach(g.zeichen) != null && d.gegeben.all { gg -> f.formel.contains(gg.zeichen) || f.umstellungen.any { it.contains(gg.zeichen) } } } }
            vorgabe.value = MitrechnenVorgabe(d, formel, s?.titel)
        }
    }
}

@Composable
internal fun MitrechnenWerkzeug(vm: MitrechnenViewModel = hiltViewModel()) {
    val v by vm.vorgabe.collectAsStateWithLifecycle()
    val vv = v ?: return
    if (vv.daten?.art == "physik") PhysikMitrechnen(vv) else MatheMitrechnen(vm.eingabe?.lines() ?: vv.daten?.aufgabe.orEmpty(), vv.daten?.tipps.orEmpty())
}

private data class Zeile(val text: String, val pruefung: SchrittPruefung)

@Composable
internal fun MatheMitrechnen(vorbelegt: List<String>, tipps: List<String>) {
    val f = Lernen.farben
    var aufgabe by rememberSaveable { mutableStateOf(vorbelegt.map(String::trim).filter { it.isNotBlank() }.joinToString("\n")) }
    var gestartet by rememberSaveable { mutableStateOf(false) }
    var eingabe by rememberSaveable { mutableStateOf("") }
    var tippStufe by rememberSaveable { mutableStateOf(0) }
    var fehler by rememberSaveable { mutableStateOf<String?>(null) }
    val schritte = rememberSaveableListe()
    var engine by androidx.compose.runtime.remember { mutableStateOf<Mitrechnen?>(null) }

    fun starten() {
        val z = aufgabe.lines().filter { it.isNotBlank() }
        engine = runCatching { Mitrechnen(z) }.onFailure { fehler = it.message ?: "Die Aufgabe kann ich nicht lesen." }.getOrNull()
        if (engine != null) { gestartet = true; fehler = null; schritte.clear() }
    }
    // Nach Drehen des Bildschirms die Engine aus den gespeicherten Schritten wiederherstellen
    if (gestartet && engine == null) {
        engine = runCatching { Mitrechnen(aufgabe.lines().filter { it.isNotBlank() }) }.getOrNull()
        engine?.let { e -> schritte.filter { it.pruefung.ok }.forEach { e.pruefe(it.text) } }
    }

    val alleTipps = tipps.ifEmpty { allgemeineTipps(engine?.beschreibung) }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Karte {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    KartenTitel("Deine Aufgabe", Modifier.weight(1f))
                    if (gestartet) TextButton(onClick = { gestartet = false; engine = null; schritte.clear(); tippStufe = 0 }) { Text("Ändern") }
                }
                if (gestartet) {
                    Text(aufgabe, fontFamily = LeseSchrift, fontSize = 19.sp, lineHeight = 28.sp)
                    engine?.let { Grau(it.beschreibung, Modifier.padding(top = 4.dp)) }
                } else {
                    Grau("Tippe die Aufgabe ein. Mehrere Gleichungen: mit ⏎ in eine neue Zeile.", Modifier.padding(bottom = 8.dp))
                    aufgabe.lines().forEachIndexed { i, z ->
                        Eingabezeile(z, if (i == 0) "z. B. y = 2x − 1" else "", Modifier.padding(bottom = 6.dp), aktiv = i == aufgabe.lines().lastIndex)
                    }
                    fehler?.let { Text(it, color = f.schlecht, style = MaterialTheme.typography.bodySmall) }
                    Button(onClick = ::starten, enabled = aufgabe.isNotBlank(), modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) { Text("Los geht's") }
                }
            }
            if (gestartet) {
                Spacer(Modifier.height(10.dp))
                schritte.forEachIndexed { i, z ->
                    val ok = z.pruefung.ok
                    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp).clip(RoundedCornerShape(12.dp)).background(if (ok) f.okTon else f.schlechtTon).padding(10.dp)) {
                        Text(if (ok) "✓" else "✗", color = if (ok) f.ok else f.schlecht, fontWeight = FontWeight.Bold, modifier = Modifier.width(22.dp))
                        Column {
                            Text(z.text, fontFamily = LeseSchrift, fontSize = 18.sp)
                            Text(z.pruefung.meldung, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                alleTipps.take(tippStufe).forEachIndexed { i, t ->
                    HinweisBox(BoxArt.TIPP, "Tipp ${i + 1}") { Text(t, style = MaterialTheme.typography.bodyMedium) }
                }
                Grau("Dein Schritt ${schritte.count { it.pruefung.ok } + 1}", Modifier.padding(top = 4.dp, bottom = 4.dp))
                Eingabezeile(eingabe, "nächste Umformung …")
            }
        }
        Spacer(Modifier.height(8.dp))
        if (gestartet) Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { tippStufe++ }, enabled = tippStufe < alleTipps.size, modifier = Modifier.weight(1f)) {
                Text(if (tippStufe < alleTipps.size) "Tipp ${tippStufe + 1} von ${alleTipps.size}" else "Keine Tipps mehr")
            }
        }
        Tastatur(MATHE_TASTEN, onTaste = { t ->
            if (t.text == "⏎") {
                if (!gestartet) aufgabe += "\n"
                else engine?.let { e -> if (eingabe.isNotBlank()) { schritte.add(Zeile(eingabe, e.pruefe(eingabe))); if (schritte.last().pruefung.ok) eingabe = "" } }
            } else if (!gestartet) {
                val zeilen = aufgabe.lines().toMutableList()
                if (t == ENTF && zeilen.last().isEmpty() && zeilen.size > 1) zeilen.removeAt(zeilen.lastIndex)
                else zeilen[zeilen.lastIndex] = tippe(zeilen.last(), t)
                aufgabe = zeilen.joinToString("\n")
            } else eingabe = tippe(eingabe, t)
        }, modifier = Modifier.padding(bottom = 8.dp))
    }
}

@Composable
private fun rememberSaveableListe() = androidx.compose.runtime.remember { mutableStateListOf<Zeile>() }

private fun allgemeineTipps(beschreibung: String?): List<String> = when {
    beschreibung == null -> emptyList()
    beschreibung.startsWith("Lineares Gleichungssystem") -> listOf(
        "Löse eine Gleichung nach x oder y auf.",
        "Setze den Term in die andere Gleichung ein – in Klammern.",
        "Jetzt hat die Gleichung nur noch eine Unbekannte: zusammenfassen und auflösen.",
    )
    beschreibung.startsWith("Gleichung") -> listOf(
        "Löse zuerst Klammern auf und fasse jede Seite zusammen.",
        "Bring alle Glieder mit der Unbekannten auf eine Seite, alle Zahlen auf die andere.",
        "Teile zum Schluss durch die Zahl vor der Unbekannten.",
    )
    else -> listOf("Multipliziere Klammern aus.", "Fasse gleichartige Glieder zusammen (x mit x, Zahlen mit Zahlen).", "Denk an die binomischen Formeln.")
}

@Composable
internal fun PhysikMitrechnen(v: MitrechnenVorgabe) {
    val d = v.daten ?: return
    val f = Lernen.farben
    var umstellen by rememberSaveable { mutableStateOf(false) }
    var ergebnis by rememberSaveable { mutableStateOf("") }
    var meldung by rememberSaveable { mutableStateOf<Pair<Boolean, String>?>(null) }
    var tippStufe by rememberSaveable { mutableStateOf(0) }
    var haken by rememberSaveable { mutableStateOf(listOf<Int>()) }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        v.titel?.let { Grau(it, Modifier.padding(bottom = 8.dp)) }
        Karte {
            KartenTitel("Gegeben & gesucht")
            Row {
                Column(Modifier.weight(1f)) {
                    Grau("Gegeben")
                    d.gegeben.forEach { g -> Text("${g.zeichen} = ${g.wert} ${g.einheit}", fontFamily = LeseSchrift, fontSize = 18.sp) }
                }
                d.gesucht?.let { g ->
                    Column(Modifier.weight(1f)) {
                        Grau("Gesucht")
                        Text("${g.zeichen} = ? ${g.einheit}".trim(), fontFamily = LeseSchrift, fontSize = 18.sp)
                        if (g.name.isNotBlank()) Grau(g.name)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        v.formel?.let { fo ->
            Karte {
                KartenTitel("Passende Formel")
                Text(fo.formel, fontFamily = LeseSchrift, fontSize = 20.sp)
                Grau(fo.name)
                val ziel = d.gesucht?.zeichen
                val um = ziel?.let { fo.umgestelltNach(it) }
                if (um != null && um != fo.formel) {
                    if (!umstellen) OutlinedButton(onClick = { umstellen = true }, modifier = Modifier.padding(top = 8.dp)) { Text("Umstellen zeigen") }
                    else {
                        Text(um, fontFamily = LeseSchrift, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
                        Grau(umstellHinweis(fo.formel, um))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Karte {
            KartenTitel("Dein Ergebnis mit Einheit")
            OutlinedTextField(
                value = ergebnis, onValueChange = { ergebnis = it; meldung = null }, singleLine = true,
                placeholder = { Text("z. B. 8,7 A") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = {
                val e = d.ergebnis ?: return@Button
                val soll = e.wert.replace(',', '.').toDoubleOrNull() ?: return@Button
                val p = Einheiten.pruefe(ergebnis, soll, e.einheit, e.toleranz)
                meldung = (p.bewertung == Einheiten.Bewertung.RICHTIG) to p.meldung
            }, enabled = ergebnis.isNotBlank() && d.ergebnis != null, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Prüfen") }
            meldung?.let { (ok, t) ->
                Spacer(Modifier.height(8.dp))
                HinweisBox(if (ok) BoxArt.OK else if (t.startsWith("Halb")) BoxArt.WARNUNG else BoxArt.SCHLECHT) { Text(t, style = MaterialTheme.typography.bodyMedium) }
            }
        }
        Spacer(Modifier.height(10.dp))
        d.tipps.take(tippStufe).forEachIndexed { i, t -> HinweisBox(BoxArt.TIPP, "Tipp ${i + 1}") { Text(t, style = MaterialTheme.typography.bodyMedium) } }
        if (tippStufe < d.tipps.size) FilledTonalButton(onClick = { tippStufe++ }, modifier = Modifier.fillMaxWidth()) { Text("Tipp ${tippStufe + 1} von ${d.tipps.size} zeigen") }
        Spacer(Modifier.height(10.dp))
        Karte(innen = 0.dp) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                listOf("Gegeben/Gesucht notiert", "Formel umgestellt", "Antwortsatz geschrieben").forEachIndexed { i, t ->
                    AbhakZeile(t, i in haken, { haken = if (i in haken) haken - i else haken + i }, letzte = i == 2)
                }
            }
        }
        if (f.dunkel) Spacer(Modifier.height(0.dp))
    }
}

private fun umstellHinweis(formel: String, umgestellt: String): String {
    val ziel = umgestellt.substringBefore("=").trim()
    val rechts = formel.substringAfter("=")
    return when {
        rechts.contains("·") -> "Beide Seiten durch die anderen Faktoren teilen, dann steht $ziel allein."
        rechts.contains(":") -> "Beide Seiten mit dem Nenner malnehmen und dann nach $ziel auflösen."
        else -> "So umstellen, dass $ziel allein auf einer Seite steht."
    }
}
