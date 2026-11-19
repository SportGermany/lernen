package de.lernen.core.design.komponenten

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.lernen.core.data.model.Fach
import de.lernen.core.design.theme.Lernen
import de.lernen.core.design.theme.LeseStil
import de.lernen.core.design.theme.fachFarbe
import de.lernen.core.design.theme.fachTon

val KartenForm = RoundedCornerShape(16.dp)

/** Weiße Karte mit dünnem Rand (wie .card in der Konzept-HTML). */
@Composable
fun Karte(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    farbe: Color = Lernen.farben.karte,
    rand: Color? = Lernen.farben.rand,
    linksStreifen: Color? = null,
    innen: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val m = modifier
        .fillMaxWidth()
        .clip(KartenForm)
        .background(farbe)
        .then(if (rand != null) Modifier.border(1.dp, rand, KartenForm) else Modifier)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    Row(m.height(IntrinsicSize.Min)) {
        if (linksStreifen != null) Box(Modifier.width(4.dp).fillMaxHeight().background(linksStreifen))
        Column(Modifier.padding(innen).weight(1f), content = content)
    }
}


/** Abschnittsüberschrift zwischen Karten. */
@Composable
fun Ueberschrift(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = modifier.padding(top = 14.dp, bottom = 8.dp))
}

/** Kleine fette Zwischenüberschrift in einer Karte. */
@Composable
fun KartenTitel(text: String, modifier: Modifier = Modifier, farbe: Color = Color.Unspecified) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = farbe, modifier = modifier.padding(bottom = 6.dp))
}

@Composable
fun Grau(text: String, modifier: Modifier = Modifier, maxZeilen: Int = Int.MAX_VALUE) {
    Text(
        text, style = MaterialTheme.typography.bodySmall, color = Lernen.farben.text2, modifier = modifier,
        maxLines = maxZeilen, overflow = TextOverflow.Ellipsis,
    )
}

/** Farbiger Punkt mit Fach-Kürzel. */
@Composable
fun FachPunkt(fach: Fach?, groesse: Dp = 38.dp) {
    Box(
        Modifier.size(groesse).clip(RoundedCornerShape(groesse * 0.32f)).background(fachTon(fach)),
        contentAlignment = Alignment.Center,
    ) {
        Text(fach?.kuerzel ?: "?", color = fachFarbe(fach), fontWeight = FontWeight.SemiBold, fontSize = (groesse.value * 0.37f).sp)
    }
}

/** Pille mit Fachname. */
@Composable
fun FachPille(fach: Fach?) {
    Text(
        fach?.name ?: "Unbekannt",
        color = fachFarbe(fach), fontSize = 12.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier.clip(CircleShape).background(fachTon(fach)).padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

/** Kleines Abzeichen (z. B. "12/52"). */
@Composable
fun Abzeichen(text: String, farbe: Color = MaterialTheme.colorScheme.primary) {
    Text(
        text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = farbe,
        modifier = Modifier.clip(CircleShape).background(Lernen.farben.ton).padding(horizontal = 9.dp, vertical = 3.dp),
    )
}

/** Dezenter Fortschrittsbalken. */
@Composable
fun Balken(anteil: Float, farbe: Color, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Lernen.farben.ton)) {
        Box(Modifier.fillMaxWidth(anteil.coerceIn(0f, 1f)).height(6.dp).clip(RoundedCornerShape(3.dp)).background(farbe))
    }
}

enum class BoxArt { WARNUNG, INFO, TIPP, OK, SCHLECHT }

/** Farbiger Hinweiskasten: Typische Fehler, Gut zu wissen, Tipp, Richtig/Falsch. */
@Composable
fun HinweisBox(art: BoxArt, titel: String? = null, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val f = Lernen.farben
    val (hg, vg) = when (art) {
        BoxArt.WARNUNG -> f.warnungTon to f.warnung
        BoxArt.INFO -> f.infoTon to f.text
        BoxArt.TIPP -> f.ton to MaterialTheme.colorScheme.primary
        BoxArt.OK -> f.okTon to f.ok
        BoxArt.SCHLECHT -> f.schlechtTon to f.schlecht
    }
    Row(modifier.fillMaxWidth().padding(bottom = 10.dp).height(IntrinsicSize.Min).clip(RoundedCornerShape(12.dp)).background(hg)) {
        if (art == BoxArt.TIPP) Box(Modifier.width(3.dp).fillMaxHeight().background(vg))
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            if (titel != null) Text(
                titel.uppercase(), style = MaterialTheme.typography.labelSmall, color = vg,
                modifier = Modifier.padding(bottom = 3.dp),
            )
            content()
        }
    }
}

/** Aufzählung mit Punkten. */
@Composable
fun Punkte(items: List<String>, farbe: Color = Color.Unspecified) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { t ->
            Row {
                Text("•", color = farbe, modifier = Modifier.width(14.dp))
                Text(t, style = MaterialTheme.typography.bodyMedium, color = farbe)
            }
        }
    }
}

/** Aufklappbare Karte (wie <details>). */
@Composable
fun Aufklappbar(titel: String, modifier: Modifier = Modifier, offen: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    var auf by rememberSaveable(titel) { mutableStateOf(offen) }
    Karte(modifier.padding(bottom = 10.dp), innen = 0.dp) {
        Row(
            Modifier.fillMaxWidth().clickable { auf = !auf }.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(titel, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Icon(
                if (auf) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (auf) "Zuklappen" else "Aufklappen", tint = Lernen.farben.text2,
            )
        }
        AnimatedVisibility(auf) {
            Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp), content = content)
        }
    }
}

/** Vorher-/Nachher-Beispiel (rot/grün hinterlegt). */
@Composable
fun VorherNachher(vorher: String, nachher: String, vorherLabel: String, nachherLabel: String, nebeneinander: Boolean) {
    val f = Lernen.farben
    @Composable
    fun Feld(label: String, html: String, hg: Color, mod: Modifier) {
        Column(mod.clip(RoundedCornerShape(12.dp)).background(hg).padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = f.text2, modifier = Modifier.padding(bottom = 3.dp))
            HtmlText(html, stil = LeseStil.copy(fontSize = 15.sp, lineHeight = 23.sp), markierungen = false)
        }
    }
    if (nebeneinander) {
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Feld(vorherLabel, escape(vorher), f.schlechtTon, Modifier.weight(1f))
            Feld(nachherLabel, nachher, f.okTon, Modifier.weight(1f))
        }
    } else {
        Column(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Feld(vorherLabel, escape(vorher), f.schlechtTon, Modifier.fillMaxWidth())
            Feld(nachherLabel, nachher, f.okTon, Modifier.fillMaxWidth())
        }
    }
}

/** "vorher" ist reiner Text (kein HTML) – spitze Klammern daher entschärfen. */
private fun escape(t: String) = t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

/** Platzhalter für Bereiche, die in einem späteren Meilenstein kommen. */
@Composable
fun KommtSpaeter(titel: String, text: String, modifier: Modifier = Modifier) {
    Karte(modifier) {
        Text(titel, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Grau(text)
    }
}

/** Zeile mit Abstand zwischen Elementen. */
@Composable
fun ZeileZwischen(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) =
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, content = content)

@Composable
fun UmrandeteFlaeche(modifier: Modifier = Modifier, content: @Composable () -> Unit) =
    Surface(modifier, shape = KartenForm, border = BorderStroke(1.dp, Lernen.farben.rand), color = Lernen.farben.karte, content = content)
