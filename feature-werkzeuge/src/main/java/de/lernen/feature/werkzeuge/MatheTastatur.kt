package de.lernen.feature.werkzeuge

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.lernen.core.design.theme.Lernen
import de.lernen.core.design.theme.LeseSchrift

/** Eine Taste: Beschriftung, eingefügter Text und Art. */
data class Taste(val anzeige: String, val text: String = anzeige, val art: Art = Art.NORMAL, val breite: Float = 1f) {
    enum class Art { NORMAL, ZAHL, OP, AKTION, HAUPT }
}

val ENTF = Taste("⌫", "", Taste.Art.AKTION)
val ALLES_LOESCHEN = Taste("AC", "", Taste.Art.AKTION)

/** Mathe-Tastatur für Mitrechnen und Plotter. */
val MATHE_TASTEN = listOf(
    listOf(Taste("x"), Taste("y"), Taste("a"), Taste("x²", "²"), Taste("√", "√(")),
    listOf(Taste("7", art = Taste.Art.ZAHL), Taste("8", art = Taste.Art.ZAHL), Taste("9", art = Taste.Art.ZAHL), Taste("("), Taste(")")),
    listOf(Taste("4", art = Taste.Art.ZAHL), Taste("5", art = Taste.Art.ZAHL), Taste("6", art = Taste.Art.ZAHL), Taste("·", art = Taste.Art.OP), Taste(":", art = Taste.Art.OP)),
    listOf(Taste("1", art = Taste.Art.ZAHL), Taste("2", art = Taste.Art.ZAHL), Taste("3", art = Taste.Art.ZAHL), Taste("+", art = Taste.Art.OP), Taste("−", art = Taste.Art.OP)),
    listOf(Taste("0", art = Taste.Art.ZAHL), Taste(",", art = Taste.Art.ZAHL), Taste("=", art = Taste.Art.OP), Taste("π"), ENTF),
    listOf(Taste("a/b", "/", Taste.Art.OP), Taste("xⁿ", "^"), Taste("b"), Taste("Prüfen", "⏎", Taste.Art.HAUPT, 2f)),
)

/** Tastatur für den Taschenrechner. */
val RECHNER_TASTEN = listOf(
    listOf(Taste("sin", "sin("), Taste("cos", "cos("), Taste("tan", "tan("), Taste("√", "√("), Taste("xⁿ", "^")),
    listOf(Taste("ln", "ln("), Taste("log", "log("), Taste("π"), Taste("("), Taste(")")),
    listOf(Taste("7", art = Taste.Art.ZAHL), Taste("8", art = Taste.Art.ZAHL), Taste("9", art = Taste.Art.ZAHL), ENTF, ALLES_LOESCHEN),
    listOf(Taste("4", art = Taste.Art.ZAHL), Taste("5", art = Taste.Art.ZAHL), Taste("6", art = Taste.Art.ZAHL), Taste("·", art = Taste.Art.OP), Taste(":", art = Taste.Art.OP)),
    listOf(Taste("1", art = Taste.Art.ZAHL), Taste("2", art = Taste.Art.ZAHL), Taste("3", art = Taste.Art.ZAHL), Taste("+", art = Taste.Art.OP), Taste("−", art = Taste.Art.OP)),
    listOf(Taste("0", art = Taste.Art.ZAHL), Taste(",", art = Taste.Art.ZAHL), Taste("x²", "²"), Taste("Ans", "Ans"), Taste("=", "⏎", Taste.Art.HAUPT)),
)

@Composable
fun Tastatur(tasten: List<List<Taste>>, onTaste: (Taste) -> Unit, modifier: Modifier = Modifier) {
    val f = Lernen.farben
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        tasten.forEach { reihe ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                reihe.forEach { t ->
                    val (hg, vg) = when (t.art) {
                        Taste.Art.ZAHL -> f.karte to f.text
                        Taste.Art.OP -> f.ton to MaterialTheme.colorScheme.primary
                        Taste.Art.AKTION -> f.ton to f.text2
                        Taste.Art.HAUPT -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
                        Taste.Art.NORMAL -> f.ton to f.text
                    }
                    Box(
                        Modifier.weight(t.breite).height(46.dp).clip(RoundedCornerShape(12.dp)).background(hg)
                            .then(if (t.art == Taste.Art.ZAHL) Modifier.border(1.dp, f.rand, RoundedCornerShape(12.dp)) else Modifier)
                            .clickable { onTaste(t) }
                            .semantics { contentDescription = beschreibung(t) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(t.anzeige, color = vg, fontSize = if (t.anzeige.length > 3) 14.sp else 18.sp, fontWeight = if (t.art == Taste.Art.HAUPT) FontWeight.Medium else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

private fun beschreibung(t: Taste) = when (t.anzeige) {
    "⌫" -> "Löschen"; "AC" -> "Alles löschen"; "·" -> "mal"; ":" -> "geteilt"; "−" -> "minus"; "+" -> "plus"
    "x²" -> "hoch zwei"; "xⁿ" -> "hoch"; "√" -> "Wurzel"; "a/b" -> "Bruchstrich"; "π" -> "Pi"; "=" -> "gleich"
    else -> t.anzeige
}

/** Eingabezeile mit blinkendem Strich am Ende (ohne Systemtastatur). */
@Composable
fun Eingabezeile(text: String, platzhalter: String, modifier: Modifier = Modifier, aktiv: Boolean = true, onClick: (() -> Unit)? = null) {
    val f = Lernen.farben
    val blink by rememberInfiniteTransition(label = "cursor").animateFloat(1f, 0f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "c")
    Row(
        modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(12.dp)).background(f.karte)
            .border(if (aktiv) 2.dp else 1.dp, if (aktiv) MaterialTheme.colorScheme.primary else f.rand, RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (text.isEmpty()) Text(platzhalter, color = f.text2, fontSize = 18.sp, fontFamily = LeseSchrift, modifier = Modifier.weight(1f, false))
        else Text(text, fontSize = 20.sp, fontFamily = LeseSchrift, modifier = Modifier.weight(1f, false))
        if (aktiv) Box(Modifier.padding(start = 1.dp).height(24.dp).alpha(blink).background(MaterialTheme.colorScheme.primary).padding(horizontal = 1.dp)) {}
    }
}

/** Wendet eine Taste auf einen Eingabetext an. */
fun tippe(text: String, t: Taste): String = when (t) {
    ENTF -> text.dropLast(if (text.endsWith("√(") || text.endsWith("Ans")) (if (text.endsWith("Ans")) 3 else 2) else 1)
    ALLES_LOESCHEN -> ""
    else -> text + t.text
}

