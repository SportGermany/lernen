package de.lernen.feature.thema

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.lernen.core.data.paket.HtmlMini
import de.lernen.core.design.komponenten.AbhakZeile
import de.lernen.core.design.komponenten.Abzeichen
import de.lernen.core.design.komponenten.BoxArt
import de.lernen.core.design.komponenten.FachPunkt
import de.lernen.core.design.komponenten.Grau
import de.lernen.core.design.komponenten.HinweisBox
import de.lernen.core.design.komponenten.HtmlText
import de.lernen.core.design.komponenten.Karte
import de.lernen.core.design.komponenten.KartenTitel
import de.lernen.core.design.komponenten.Punkte
import de.lernen.core.design.komponenten.Ueberschrift
import de.lernen.core.design.komponenten.ZeileZwischen
import de.lernen.core.design.komponenten.marke
import de.lernen.core.design.theme.Lernen
import de.lernen.core.design.theme.LeseStil

@Composable
internal fun UeberblickTab(z: ThemaZustand, breit: Boolean, onSchritt: (Int) -> Unit, onStart: () -> Unit) {
    val o = z.paket.ueberblick
    val f = Lernen.farben
    var marken by rememberSaveable { mutableStateOf(true) }

    // Aufgabe
    Karte {
        if (o.quelle != null) Text(o.quelle!!, color = f.text2, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        else KartenTitel("Worum geht's?")
        o.aufgabe?.let { HtmlText(it, Modifier.padding(top = if (o.quelle != null) 6.dp else 0.dp), stil = MaterialTheme.typography.bodyLarge) }
    }
    Spacer(Modifier.height(10.dp))

    if (z.paket.istArbeitsheft) {
        HinweisBox(BoxArt.INFO) {
            Text("Leg dein Arbeitsheft daneben. Die App verweist auf die Zeilennummern in deinem Heft.", style = MaterialTheme.typography.bodyMedium)
        }
        if (o.karte.isNotEmpty()) {
            Ueberschrift("Wo steht was im Text?")
            Karte {
                o.karte.forEachIndexed { i, k ->
                    Row(Modifier.padding(vertical = 9.dp), verticalAlignment = Alignment.Top) {
                        Box(Modifier.width(76.dp)) { Abzeichen(k.getOrElse(0) { "" }) }
                        Column(Modifier.weight(1f)) {
                            Text(k.getOrElse(1) { "" }, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Grau(k.getOrElse(2) { "" })
                        }
                    }
                    if (i < o.karte.lastIndex) HorizontalDivider(color = f.ton)
                }
            }
        }
    }

    // Beispiel: Ausgangslage + Auftrag (Schreiben)
    if (o.ausgangslage != null || o.auftrag != null) {
        Ueberschrift("Beispiel-Aufgabe")
        Karte(farbe = f.ton, rand = null) {
            o.ausgangslage?.let {
                Text("AUSGANGSLAGE", style = MaterialTheme.typography.labelSmall, color = f.text2, modifier = Modifier.padding(bottom = 6.dp))
                Text(it, style = LeseStil)
            }
            o.auftrag?.let {
                KartenTitel("Auftrag", Modifier.padding(top = 12.dp))
                Text(it, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }

    // Muster (Schreiben) oder Vorher/Nachher (Überarbeiten)
    if (o.muster != null) {
        val mitMarken = o.muster!!.contains("class=")
        if (mitMarken) UeberschriftMitSchalter("So sieht's fertig aus", marken) { marken = it } else Ueberschrift("So sieht's fertig aus")
        Karte {
            if (mitMarken) Legende(marken)
            o.ueberschrift?.let { Text(it, style = LeseStil.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold), modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)) }
            HtmlText(o.muster!!, stil = LeseStil, markierungen = marken)
        }
    } else if (o.vorher != null && o.nachher != null) {
        UeberschriftMitSchalter("So sieht's vorher und nachher aus", marken) { marken = it }
        Zweispaltig(breit,
            links = {
                Karte(farbe = f.schlechtTon, rand = null) {
                    Text("VORHER · Schülertext", style = MaterialTheme.typography.labelSmall, color = f.text2, modifier = Modifier.padding(bottom = 6.dp))
                    Text(o.vorher!!, style = LeseStil)
                }
            },
            rechts = {
                Karte {
                    Text("NACHHER · überarbeitet", style = MaterialTheme.typography.labelSmall, color = f.text2)
                    Legende(marken)
                    Spacer(Modifier.height(8.dp))
                    HtmlText(o.nachher!!, stil = LeseStil, markierungen = marken)
                }
            },
        )
    }

    if (o.aufbau.isNotEmpty()) {
        Ueberschrift("Der Aufbau")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            o.aufbau.forEachIndexed { i, a ->
                Karte {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(42.dp)) { NummerPunkt(i + 1, z) }
                        Column {
                            Text(a.getOrElse(0) { "" }, style = MaterialTheme.typography.titleSmall)
                            Grau(a.getOrElse(1) { "" })
                        }
                    }
                }
            }
        }
    }

    // Weg durch den Begleiter
    Ueberschrift("Dein Weg")
    Karte(innen = 0.dp) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
            z.paket.begleiter.forEachIndexed { i, s ->
                AbhakZeile(
                    "${s.titel}  ·  ${z.fortschritt.erledigtIn(s)}/${s.muss.size}",
                    abgehakt = z.fortschritt.schrittFertig(s),
                    onUmschalten = { onSchritt(i) },
                    letzte = i == z.paket.begleiter.lastIndex,
                )
            }
        }
    }

    if (o.ziel.isNotEmpty()) {
        Ueberschrift("Am Ende hat dein Text …")
        Karte { Punkte(o.ziel) }
    }
    if (o.kriterien.isNotEmpty()) {
        Ueberschrift("Darauf achtet die Lehrkraft")
        Karte { Punkte(o.kriterien) }
    }
    o.regel?.let {
        Spacer(Modifier.height(10.dp))
        HinweisBox(BoxArt.WARNUNG, "Wichtig") { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }

    if (o.erlaubt.isNotEmpty() || o.verboten.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        val schreiben = o.muster != null
        Zweispaltig(breit,
            links = {
                Karte(farbe = f.okTon, rand = null) {
                    KartenTitel(if (schreiben) "Gehört rein" else "Das darfst du", farbe = f.ok)
                    Punkte(o.erlaubt)
                }
            },
            rechts = {
                Karte(farbe = f.schlechtTon, rand = null) {
                    KartenTitel(if (schreiben) "Gehört nicht rein" else "Das darfst du nicht", farbe = f.schlecht)
                    Punkte(o.verboten)
                }
            },
        )
    }

    Spacer(Modifier.height(14.dp))
    val erster = z.paket.begleiter.firstOrNull()?.kurz
    Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(48.dp)) {
        Text(if (z.paket.istArbeitsheft && erster != null) "Los geht's mit $erster" else "Zum Begleiter")
    }
}

@Composable
private fun NummerPunkt(n: Int, z: ThemaZustand) {
    // Fachfarbe wie beim Fach-Punkt, aber mit Zahl
    Box(Modifier.width(30.dp).height(30.dp).clip(RoundedCornerShape(10.dp)).background(de.lernen.core.design.theme.fachTon(z.fach)), contentAlignment = Alignment.Center) {
        Text("$n", color = de.lernen.core.design.theme.fachFarbe(z.fach), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun UeberschriftMitSchalter(titel: String, an: Boolean, onAendern: (Boolean) -> Unit) {
    ZeileZwischen {
        Ueberschrift(titel, Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Grau("Markierungen")
            Spacer(Modifier.width(6.dp))
            Switch(checked = an, onCheckedChange = onAendern)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun Legende(an: Boolean) {
    if (!an) return
    val f = Lernen.farben
    FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        HtmlMini.Markierung.TEXTMARKEN.forEach { m ->
            Text(
                m.bedeutung, fontSize = 12.sp,
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(f.marke(m)).padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

/** Auf breiten Bildschirmen nebeneinander, sonst untereinander. */
@Composable
internal fun Zweispaltig(breit: Boolean, links: @Composable () -> Unit, rechts: @Composable () -> Unit) {
    if (breit) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(Modifier.weight(1f)) { links() }
            Column(Modifier.weight(1f)) { rechts() }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { links(); rechts() }
    }
}

@Composable
internal fun FachKopf(z: ThemaZustand) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FachPunkt(z.fach, 30.dp)
        Spacer(Modifier.width(8.dp))
        Grau(z.paket.stufe)
    }
}
