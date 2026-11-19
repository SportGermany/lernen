package de.lernen.feature.thema

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.lernen.core.data.model.Schritt
import de.lernen.core.design.komponenten.AbhakZeile
import de.lernen.core.design.komponenten.Aufklappbar
import de.lernen.core.design.komponenten.BoxArt
import de.lernen.core.design.komponenten.Grau
import de.lernen.core.design.komponenten.HinweisBox
import de.lernen.core.design.komponenten.Karte
import de.lernen.core.design.komponenten.KartenTitel
import de.lernen.core.design.komponenten.Punkte
import de.lernen.core.design.komponenten.SchrittChip
import de.lernen.core.design.komponenten.SchrittLeiste
import de.lernen.core.design.komponenten.VorherNachher
import de.lernen.core.design.komponenten.ZeileZwischen
import de.lernen.core.design.theme.Lernen

@Composable
internal fun BegleiterTab(
    z: ThemaZustand,
    breit: Boolean,
    onSchritt: (Int) -> Unit,
    onHaken: (String, Int, Boolean) -> Unit,
    onTipp: (String, Int, Int) -> Unit,
    onAntwort: (String, Int, Boolean) -> Unit,
    onNochmal: (String) -> Unit,
    onWortkiste: () -> Unit,
    onFertig: () -> Unit,
    onFrage: (String?) -> Unit = {},
) {
    val b = z.paket.begleiter
    if (b.isEmpty()) { Grau("Dieses Paket hat keinen Begleiter."); return }
    val s = b[z.schritt]
    val naechster = b.getOrNull(z.schritt + 1)

    SchrittLeiste(b.map { SchrittChip(it.kurz, z.fortschritt.schrittFertig(it)) }, z.schritt, onSchritt)
    Spacer(Modifier.height(10.dp))
    Text(s.titel, style = MaterialTheme.typography.titleLarge)
    if (s.ziel.isNotBlank()) Text("Ziel: ${s.ziel}", style = MaterialTheme.typography.bodyMedium, color = Lernen.farben.text2, modifier = Modifier.padding(top = 2.dp))
    Spacer(Modifier.height(12.dp))

    // Tablet quer: links Abhakliste, rechts Tipps und Beispiele
    Zweispaltig(
        breit,
        links = { Links(z, s, onHaken) },
        rechts = { Rechts(z, s, breit, onTipp, onAntwort, onNochmal, onWortkiste) },
    )

    TextButton(onClick = { onFrage(s.id) }, modifier = Modifier.fillMaxWidth()) { Text("Frage an Claude zu diesem Schritt") }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (z.schritt > 0) OutlinedButton(onClick = { onSchritt(z.schritt - 1) }, modifier = Modifier.height(48.dp)) { Text("‹", fontSize = 18.sp) }
        if (naechster != null) Button(onClick = { onSchritt(z.schritt + 1) }, modifier = Modifier.weight(1f).height(48.dp)) {
            Text("Weiter → ${naechster.kurz}", maxLines = 1)
        } else Button(onClick = onFertig, modifier = Modifier.weight(1f).height(48.dp)) { Text("Geschafft! Jetzt üben") }
    }
}

@Composable
private fun Links(z: ThemaZustand, s: Schritt, onHaken: (String, Int, Boolean) -> Unit) {
    s.wissen?.let {
        HinweisBox(BoxArt.INFO, "Gut zu wissen") { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }
    if (s.muss.isNotEmpty()) {
        Karte(innen = 0.dp, modifier = Modifier.padding(bottom = 10.dp)) {
            Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 4.dp)) {
                ZeileZwischen {
                    Text(s.listenTitel, style = MaterialTheme.typography.titleSmall)
                    Grau("${z.fortschritt.erledigtIn(s)}/${s.muss.size}")
                }
                s.muss.forEachIndexed { i, m ->
                    val an = z.fortschritt.istAbgehakt(s.id, i)
                    AbhakZeile(m, an, onUmschalten = { onHaken(s.id, i, !an) }, letzte = i == s.muss.lastIndex)
                }
            }
        }
    }
    if (s.fehler.isNotEmpty()) {
        HinweisBox(BoxArt.WARNUNG, "Typische Fehler") { Punkte(s.fehler, farbe = Lernen.farben.warnung) }
    }
}

@Composable
private fun Rechts(
    z: ThemaZustand, s: Schritt, breit: Boolean,
    onTipp: (String, Int, Int) -> Unit,
    onAntwort: (String, Int, Boolean) -> Unit,
    onNochmal: (String) -> Unit,
    onWortkiste: () -> Unit,
) {
    val f = Lernen.farben

    // Tipps in Stufen
    if (s.hinweise.isNotEmpty()) {
        val n = (z.tipps[s.id] ?: 0).coerceIn(0, s.hinweise.size)
        Karte(Modifier.padding(bottom = 10.dp)) {
            KartenTitel("Hilfe in Stufen")
            Grau("Erst selbst probieren, dann Tipp für Tipp aufdecken.", Modifier.padding(bottom = 8.dp))
            s.hinweise.take(n).forEachIndexed { i, h ->
                HinweisBox(BoxArt.TIPP, "Tipp ${i + 1}" + if (i == s.hinweise.lastIndex) " · letzter" else "") {
                    Text(h, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (n < s.hinweise.size) {
                FilledTonalButton(onClick = { onTipp(s.id, n, s.hinweise.size) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Tipp ${n + 1} von ${s.hinweise.size} zeigen")
                }
            }
        }
    }

    // Prüffrage: bei falscher Antwort wird die richtige NICHT gezeigt.
    s.frage?.let { q ->
        val gewaehlt = z.antworten[s.id]
        val beantwortet = gewaehlt != null
        val richtig = gewaehlt == q.richtig
        Karte(Modifier.padding(bottom = 10.dp)) {
            KartenTitel("Kurz prüfen")
            Text(q.frage, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 10.dp))
            q.optionen.forEachIndexed { k, o ->
                val (rand, hg) = when {
                    beantwortet && k == gewaehlt && richtig -> f.ok to f.okTon
                    beantwortet && k == gewaehlt -> f.schlecht to f.schlechtTon
                    else -> f.rand to f.karte
                }
                OutlinedButton(
                    onClick = { onAntwort(s.id, k, k == q.richtig) },
                    enabled = !beantwortet,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, rand),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = hg, contentColor = f.text, disabledContainerColor = hg, disabledContentColor = f.text),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                ) { Text(o, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start, style = MaterialTheme.typography.bodyLarge) }
            }
            if (beantwortet) {
                if (richtig) {
                    HinweisBox(BoxArt.OK) {
                        Text("Richtig.", style = MaterialTheme.typography.titleSmall, color = f.ok)
                        if (q.warum.isNotBlank()) Text(q.warum, style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    HinweisBox(BoxArt.SCHLECHT) {
                        Text("Nicht ganz.", style = MaterialTheme.typography.titleSmall, color = f.schlecht)
                        Text(
                            "Schau dir die Liste „${s.listenTitel}“" + (if (s.hinweise.isNotEmpty()) " und die Tipps" else "") + " nochmal an, dann probier es erneut.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    OutlinedButton(onClick = { onNochmal(s.id) }, modifier = Modifier.fillMaxWidth()) { Text("Nochmal versuchen") }
                }
            }
        }
    }

    if (s.vorher != null && s.nachher != null) {
        VorherNachher(s.vorher!!, s.nachher!!, s.vorherLabel, s.nachherLabel, nebeneinander = false)
    }
    s.hilfen.forEach { h ->
        Aufklappbar(h.titel) { Punkte(h.items) }
    }
    if (s.wortkiste && z.paket.wortkiste.isNotEmpty()) {
        OutlinedButton(onClick = onWortkiste, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) { Text("Wortkiste öffnen") }
    }
}

