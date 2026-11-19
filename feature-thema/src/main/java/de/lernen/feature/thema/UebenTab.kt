package de.lernen.feature.thema

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.lernen.core.data.model.Uebung
import de.lernen.core.data.paket.Aufgabe
import de.lernen.core.data.paket.ErgebnisStatus
import de.lernen.core.data.paket.Stufe
import de.lernen.core.data.paket.Ueben
import de.lernen.core.design.komponenten.AbhakZeile
import de.lernen.core.design.komponenten.Abzeichen
import de.lernen.core.design.komponenten.Aufklappbar
import de.lernen.core.design.komponenten.BoxArt
import de.lernen.core.design.komponenten.Grau
import de.lernen.core.design.komponenten.HinweisBox
import de.lernen.core.design.komponenten.HtmlText
import de.lernen.core.design.komponenten.Karte
import de.lernen.core.design.komponenten.KartenTitel
import de.lernen.core.design.komponenten.ZeileZwischen
import de.lernen.core.design.theme.Lernen
import de.lernen.core.design.theme.LeseStil

@Composable
internal fun UebenTab(
    z: ThemaZustand,
    breit: Boolean,
    onStufe: (String) -> Unit,
    onIndex: (Int) -> Unit,
    onErgebnis: (Aufgabe, ErgebnisStatus) -> Unit,
    onWortkiste: () -> Unit,
    onMitrechnen: ((String) -> Unit)? = null,
) {
    val wdh = Ueben.wiederholen(z.paket, z.ergebnisse)
    val stufe = z.uebenStufe
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Stufe.entries.forEach { s -> FilterChip(selected = stufe == s.schluessel, onClick = { onStufe(s.schluessel) }, label = { Text(s.titel) }) }
        FilterChip(selected = stufe == STUFE_WIEDERHOLEN, onClick = { onStufe(STUFE_WIEDERHOLEN) },
            label = { Text("Wiederholen" + if (wdh.isNotEmpty()) " (${wdh.size})" else "") })
    }
    val liste = if (stufe == STUFE_WIEDERHOLEN) wdh else Ueben.aufgaben(z.paket, Stufe.von(stufe) ?: Stufe.EINSTIEG)
    if (liste.isEmpty()) {
        Karte {
            Text(if (stufe == STUFE_WIEDERHOLEN) "Nichts zu wiederholen" else "Keine Aufgaben in dieser Stufe", fontWeight = FontWeight.Medium)
            if (stufe == STUFE_WIEDERHOLEN) Grau("Alles, was du falsch beantwortest oder als „noch unsicher“ markierst, landet hier. Richtig gelöste Aufgaben fallen wieder heraus.")
        }
        return
    }
    val i = (z.uebenIndex[stufe] ?: 0).coerceIn(0, liste.lastIndex)
    val a = liste[i]
    val st = z.ergebnisse[a.id]
    ZeileZwischen(Modifier.padding(bottom = 8.dp)) {
        Grau("Aufgabe ${i + 1} von ${liste.size}" + if (stufe == STUFE_WIEDERHOLEN) " · aus ${a.stufe.titel}" else "")
        when (st) {
            ErgebnisStatus.RICHTIG -> Abzeichen("schon richtig", Lernen.farben.ok)
            ErgebnisStatus.FALSCH -> Abzeichen("zum Wiederholen", Lernen.farben.schlecht)
            ErgebnisStatus.UNSICHER -> Abzeichen("noch unsicher", Lernen.farben.warnung)
            null -> Unit
        }
    }
    when (val u = a.uebung) {
        is Uebung.Auswahl -> AuswahlAufgabe(a.id, u) { onErgebnis(a, if (it) ErgebnisStatus.RICHTIG else ErgebnisStatus.FALSCH) }
        is Uebung.Tippen -> TippenAufgabe(a.id, u) { onErgebnis(a, if (it) ErgebnisStatus.RICHTIG else ErgebnisStatus.FALSCH) }
        is Uebung.Umschreiben -> UmschreibenAufgabe(a.id, u, onWortkiste.takeIf { z.paket.wortkiste.isNotEmpty() }) { onErgebnis(a, it) }
        is Uebung.Text -> TextAufgabe(a.id, u, z.paket.raster, breit) { onErgebnis(a, it) }
        is Uebung.Rechnen -> RechenAufgabe(a.id, u, onMitrechnen) { onErgebnis(a, it) }
        is Uebung.Unbekannt -> Grau("Diese Aufgabe braucht eine neuere App-Version.")
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { onIndex(i - 1) }, enabled = i > 0, modifier = Modifier.weight(1f).height(48.dp)) { Text("‹ Zurück") }
        Button(onClick = { onIndex(i + 1) }, enabled = i < liste.lastIndex, modifier = Modifier.weight(1f).height(48.dp)) { Text("Weiter ›") }
    }
}

@Composable
private fun Selbsteinschaetzung(onWahl: (ErgebnisStatus) -> Unit) {
    var gewaehlt by rememberSaveable { mutableStateOf<String?>(null) }
    Text("Wie lief's?", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp, bottom = 6.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val f = Lernen.farben
        OutlinedButton(
            onClick = { gewaehlt = "unsicher"; onWahl(ErgebnisStatus.UNSICHER) }, modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = if (gewaehlt == "unsicher") f.warnungTon else Color.Transparent),
        ) { Text("Noch unsicher") }
        OutlinedButton(
            onClick = { gewaehlt = "passt"; onWahl(ErgebnisStatus.RICHTIG) }, modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = if (gewaehlt == "passt") f.okTon else Color.Transparent),
        ) { Text("Passt ✓") }
    }
}

@Composable
private fun AuswahlAufgabe(id: String, u: Uebung.Auswahl, onAntwort: (Boolean) -> Unit) {
    var gewaehlt by rememberSaveable(id) { mutableStateOf<Int?>(null) }
    val f = Lernen.farben
    Karte {
        Text(u.frage, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 10.dp))
        u.optionen.forEachIndexed { k, o ->
            val g = gewaehlt
            val (rand, hg) = when {
                g == null -> f.rand to f.karte
                k == u.richtig -> f.ok to f.okTon
                k == g -> f.schlecht to f.schlechtTon
                else -> f.rand to f.karte
            }
            OutlinedButton(
                onClick = { gewaehlt = k; onAntwort(k == u.richtig) }, enabled = g == null,
                shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, rand),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = hg, contentColor = f.text, disabledContainerColor = hg, disabledContentColor = f.text),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            ) { Text(o, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start, style = MaterialTheme.typography.bodyLarge) }
        }
        gewaehlt?.let { g ->
            val ok = g == u.richtig
            HinweisBox(if (ok) BoxArt.OK else BoxArt.SCHLECHT) {
                Text(if (ok) "Richtig." else "Leider falsch.", style = MaterialTheme.typography.titleSmall, color = if (ok) f.ok else f.schlecht)
                if (u.warum.isNotBlank()) Text(u.warum, style = MaterialTheme.typography.bodyMedium)
            }
            OutlinedButton(onClick = { gewaehlt = null }, modifier = Modifier.fillMaxWidth()) { Text("Nochmal") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TippenAufgabe(id: String, u: Uebung.Tippen, onErgebnis: (Boolean) -> Unit) {
    var gewaehlt by rememberSaveable(id) { mutableStateOf(listOf<Int>()) }
    var geprueft by rememberSaveable(id) { mutableStateOf(false) }
    val f = Lernen.farben
    val primaer = MaterialTheme.colorScheme.primary
    val bewertung = if (geprueft) Ueben.bewerte(u.tokens, gewaehlt.toSet()) else null
    Karte {
        Text(u.frage, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            u.tokens.forEachIndexed { k, t ->
                val (hg, rand) = when (bewertung?.get(k)) {
                    Ueben.TokenBewertung.GEFUNDEN -> f.okTon to f.ok
                    Ueben.TokenBewertung.UEBERSEHEN -> f.warnungTon to f.warnung
                    Ueben.TokenBewertung.FALSCH_MARKIERT -> f.schlechtTon to f.schlecht
                    Ueben.TokenBewertung.NEUTRAL -> Color.Transparent to Color.Transparent
                    null -> if (k in gewaehlt) f.ton to primaer else Color.Transparent to f.rand
                }
                Text(
                    t.text, style = LeseStil,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(hg).border(1.dp, rand, RoundedCornerShape(6.dp))
                        .clickable(enabled = !geprueft) { gewaehlt = if (k in gewaehlt) gewaehlt - k else gewaehlt + k }
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        if (!geprueft) {
            Button(onClick = { geprueft = true; onErgebnis(Ueben.tippenRichtig(u.tokens, gewaehlt.toSet())) }, modifier = Modifier.fillMaxWidth()) { Text("Prüfen") }
        } else {
            val ok = Ueben.tippenRichtig(u.tokens, gewaehlt.toSet())
            Grau("Grün = gefunden · Gelb = übersehen · Rot = falsch markiert", Modifier.padding(bottom = 8.dp))
            HinweisBox(if (ok) BoxArt.OK else BoxArt.SCHLECHT) {
                Text(if (ok) "Alles gefunden." else "Noch nicht ganz.", style = MaterialTheme.typography.titleSmall, color = if (ok) f.ok else f.schlecht)
                if (u.warum.isNotBlank()) Text(u.warum, style = MaterialTheme.typography.bodyMedium)
            }
            OutlinedButton(onClick = { geprueft = false; gewaehlt = emptyList() }, modifier = Modifier.fillMaxWidth()) { Text("Nochmal") }
        }
    }
}

@Composable
private fun UmschreibenAufgabe(id: String, u: Uebung.Umschreiben, onWortkiste: (() -> Unit)?, onWahl: (ErgebnisStatus) -> Unit) {
    var loesung by rememberSaveable(id) { mutableStateOf(false) }
    Karte {
        Text(u.frage, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))
        Text(u.satz, style = LeseStil.copy(fontSize = 17.sp), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Lernen.farben.ton).padding(12.dp))
        Spacer(Modifier.height(10.dp))
        Grau("Schreib deine Fassung auf dein Blatt.", Modifier.padding(bottom = 10.dp))
        u.tipp?.let { Aufklappbar("Tipp") { Text(it, style = MaterialTheme.typography.bodyMedium) } }
        if (onWortkiste != null) OutlinedButton(onClick = onWortkiste, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) { Text("Wortkiste öffnen") }
        if (!loesung) FilledTonalButton(onClick = { loesung = true }, modifier = Modifier.fillMaxWidth()) { Text("Lösung zeigen") }
        else {
            HinweisBox(BoxArt.OK, "Eine mögliche Lösung") { Text(u.loesung, style = LeseStil) }
            Grau("Deine Fassung darf anders sein. Wichtig ist, dass sie dasselbe leistet.", Modifier.padding(bottom = 6.dp))
            Selbsteinschaetzung(onWahl)
        }
    }
}

@Composable
private fun TextAufgabe(id: String, u: Uebung.Text, raster: List<String>, breit: Boolean, onWahl: (ErgebnisStatus) -> Unit) {
    var loesung by rememberSaveable(id) { mutableStateOf(false) }
    var marken by rememberSaveable(id) { mutableStateOf(true) }
    var haken by rememberSaveable(id) { mutableStateOf(listOf<Int>()) }
    Karte {
        Text(u.titel, style = MaterialTheme.typography.titleMedium)
        KartenTitel("Auftrag", Modifier.padding(top = 8.dp))
        Text(u.auftrag, style = MaterialTheme.typography.bodyLarge)
    }
    Spacer(Modifier.height(10.dp))
    Zweispaltig(breit,
        links = {
            u.vorlage?.let { v ->
                Karte(farbe = Lernen.farben.ton, rand = null) {
                    Text((u.vlabel ?: "Vorlage").uppercase(), style = MaterialTheme.typography.labelSmall, color = Lernen.farben.text2, modifier = Modifier.padding(bottom = 6.dp))
                    Text(v, style = LeseStil)
                }
            }
        },
        rechts = {
            if (raster.isNotEmpty()) {
                Karte(innen = 0.dp) {
                    Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 4.dp)) {
                        ZeileZwischen { Text("Selbstcheck", style = MaterialTheme.typography.titleSmall); Grau("${haken.size}/${raster.size}") }
                        Grau("Erst schreiben, dann deinen Text hiermit prüfen.")
                        raster.forEachIndexed { k, r ->
                            AbhakZeile(r, k in haken, { haken = if (k in haken) haken - k else haken + k }, letzte = k == raster.lastIndex)
                        }
                    }
                }
            }
        },
    )
    Spacer(Modifier.height(10.dp))
    if (!loesung) FilledTonalButton(onClick = { loesung = true }, modifier = Modifier.fillMaxWidth()) { Text("Musterlösung zeigen") }
    else Karte {
        ZeileZwischen {
            KartenTitel("Musterlösung")
            Row { Grau("Markierungen "); androidx.compose.material3.Switch(checked = marken, onCheckedChange = { marken = it }) }
        }
        Legende(marken)
        Spacer(Modifier.height(8.dp))
        HtmlText(u.loesung, stil = LeseStil, markierungen = marken)
        Spacer(Modifier.height(10.dp))
        Selbsteinschaetzung(onWahl)
    }
}

@Composable
private fun RechenAufgabe(id: String, u: Uebung.Rechnen, onMitrechnen: ((String) -> Unit)?, onWahl: (ErgebnisStatus) -> Unit) {
    var loesung by rememberSaveable(id) { mutableStateOf(false) }
    Karte {
        Text(u.aufgabe.replace("\\n", "\n"), style = LeseStil.copy(fontSize = 18.sp, lineHeight = 28.sp), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Lernen.farben.ton).padding(12.dp))
        Spacer(Modifier.height(8.dp))
        Grau("Löse auf deinem Blatt oder rechne hier mit.", Modifier.padding(bottom = 10.dp))
        u.tipp?.let { Aufklappbar("Tipp") { Text(it, style = MaterialTheme.typography.bodyMedium) } }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (onMitrechnen != null) OutlinedButton(onClick = { onMitrechnen(u.aufgabe) }, modifier = Modifier.weight(1f)) { Text("Mitrechnen") }
            if (!loesung) FilledTonalButton(onClick = { loesung = true }, modifier = Modifier.weight(1f)) { Text("Lösung zeigen") }
        }
        if (loesung) {
            Spacer(Modifier.height(10.dp))
            HinweisBox(BoxArt.OK, "Musterlösung") { HtmlText(u.loesung, stil = LeseStil) }
            u.ergebnis?.let { Grau("Ergebnis: $it", Modifier.padding(bottom = 6.dp)) }
            Text("Hattest du es richtig?", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 6.dp))
            var gew by rememberSaveable(id + "r") { mutableStateOf<String?>(null) }
            val f = Lernen.farben
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { gew = "f"; onWahl(ErgebnisStatus.FALSCH) }, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = if (gew == "f") f.schlechtTon else Color.Transparent)) { Text("Hatte ich falsch") }
                OutlinedButton(onClick = { gew = "r"; onWahl(ErgebnisStatus.RICHTIG) }, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = if (gew == "r") f.okTon else Color.Transparent)) { Text("Richtig ✓") }
            }
        }
    }
}
