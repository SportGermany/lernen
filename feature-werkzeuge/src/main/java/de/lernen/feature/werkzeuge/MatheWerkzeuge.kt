package de.lernen.feature.werkzeuge

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.lernen.core.data.model.Paket
import de.lernen.core.data.repo.PaketRepository
import de.lernen.core.design.komponenten.Grau
import de.lernen.core.design.komponenten.Karte
import de.lernen.core.design.komponenten.KartenTitel
import de.lernen.core.design.theme.Lernen
import de.lernen.core.design.theme.LeseSchrift
import de.lernen.core.math.Parser
import de.lernen.core.math.Winkel
import de.lernen.core.math.Zahlen
import de.lernen.core.units.Einheiten
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

// ---------- Taschenrechner ----------

@Composable
internal fun Taschenrechner() {
    var eingabe by rememberSaveable { mutableStateOf("") }
    var ans by rememberSaveable { mutableStateOf(0.0) }
    var verlauf by rememberSaveable { mutableStateOf(listOf<String>()) }
    var winkel by rememberSaveable { mutableStateOf(Winkel.GRAD) }
    var fehler by rememberSaveable { mutableStateOf<String?>(null) }
    val vorschau = remember(eingabe, winkel, ans) { rechne(eingabe, winkel, ans).getOrNull() }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                FilterChip(selected = winkel == Winkel.GRAD, onClick = { winkel = Winkel.GRAD }, label = { Text("DEG (Grad)") })
                FilterChip(selected = winkel == Winkel.BOGEN, onClick = { winkel = Winkel.BOGEN }, label = { Text("RAD") })
            }
            verlauf.takeLast(6).forEach { Text(it, color = Lernen.farben.text2, fontFamily = LeseSchrift, fontSize = 16.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) }
            Spacer(Modifier.height(6.dp))
            Eingabezeile(eingabe, "Rechnung eingeben")
            Text(
                fehler ?: vorschau?.let { "= " + Zahlen.text(it, 10) } ?: "",
                color = if (fehler != null) Lernen.farben.schlecht else MaterialTheme.colorScheme.primary,
                fontSize = 26.sp, fontFamily = LeseSchrift, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            )
        }
        Tastatur(RECHNER_TASTEN, onTaste = { t ->
            fehler = null
            if (t.text == "⏎") {
                rechne(eingabe, winkel, ans).onSuccess { r -> verlauf = verlauf + "$eingabe = ${Zahlen.text(r, 10)}"; ans = r; eingabe = "" }
                    .onFailure { fehler = it.message ?: "Fehler" }
            } else eingabe = tippe(eingabe, t)
        }, modifier = Modifier.padding(bottom = 8.dp))
    }
}

private fun rechne(text: String, w: Winkel, ans: Double): Result<Double> = runCatching {
    require(text.isNotBlank()) { "" }
    val a = Parser.term(text.replace("Ans", "(${ans})"))
    require(a.variablen().isEmpty()) { "Im Taschenrechner keine Variablen" }
    val r = a.wert(emptyMap(), w)
    require(r.isFinite()) { "nicht definiert" }
    r
}

// ---------- Formelsammlung ----------

@HiltViewModel
class FormelViewModel @Inject constructor(pakete: PaketRepository) : ViewModel() {
    val sammlung = pakete.pakete.map { l -> l.firstOrNull { it.typ == Paket.TYP_FORMELSAMMLUNG } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
internal fun Formelsammlung(vm: FormelViewModel = hiltViewModel()) {
    val fs by vm.sammlung.collectAsStateWithLifecycle()
    var suche by rememberSaveable { mutableStateOf("") }
    var fach by rememberSaveable { mutableStateOf<String?>(null) }
    val f = Lernen.farben
    val gebiete = fs?.gebiete.orEmpty()
    Column {
        OutlinedTextField(
            value = suche, onValueChange = { suche = it }, singleLine = true, shape = CircleShape,
            placeholder = { Text("Suchen, z. B. Leistung, Kreis, Ω") }, leadingIcon = { Icon(Icons.Filled.Search, null) },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = fach == null, onClick = { fach = null }, label = { Text("Alle") })
            FilterChip(selected = fach == "mathe", onClick = { fach = "mathe" }, label = { Text("Mathe") })
            FilterChip(selected = fach == "physik", onClick = { fach = "physik" }, label = { Text("Physik") })
        }
        if (fs == null) Grau("Die Formelsammlung wird beim nächsten Sync geladen.")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            gebiete.filter { fach == null || it.fach == fach }.forEach { g ->
                val formeln = g.formeln.filter { it.passtZu(suche) || g.titel.contains(suche.trim(), ignoreCase = true) }
                if (formeln.isNotEmpty()) {
                    item(key = "g-" + g.id) {
                        Text(g.titel + if (g.klasse.isNotBlank()) " · ${g.klasse}. Kl." else "", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
                    }
                    items(formeln, key = { g.id + it.id }) { fo ->
                        Karte {
                            Text(fo.name, fontWeight = FontWeight.Medium)
                            Text(fo.formel, fontFamily = LeseSchrift, fontSize = 20.sp, modifier = Modifier.padding(vertical = 6.dp))
                            fo.groessen.forEach { gr ->
                                Row(Modifier.padding(vertical = 1.dp)) {
                                    Text(gr.getOrElse(0) { "" }, fontFamily = LeseSchrift, modifier = Modifier.width(96.dp))
                                    Text(gr.getOrElse(1) { "" }, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                    Text(gr.getOrElse(2) { "" }, style = MaterialTheme.typography.bodySmall, color = f.text2)
                                }
                            }
                            if (fo.umstellungen.isNotEmpty()) {
                                HorizontalDivider(color = f.ton, modifier = Modifier.padding(vertical = 6.dp))
                                Grau("Umgestellt: " + fo.umstellungen.joinToString("   ·   "))
                            }
                            fo.hinweis?.let { Grau(it, Modifier.padding(top = 4.dp)) }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ---------- Einheiten umrechnen ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun Umrechner() {
    var kat by rememberSaveable { mutableStateOf(0) }
    val (name, einheiten) = Einheiten.KATEGORIEN[kat]
    var von by rememberSaveable(kat) { mutableStateOf(einheiten.first()) }
    var nach by rememberSaveable(kat) { mutableStateOf(einheiten.getOrElse(1) { einheiten.first() }) }
    var wert by rememberSaveable { mutableStateOf("1") }
    val ergebnis = remember(wert, von, nach) {
        wert.replace(',', '.').toDoubleOrNull()?.let { runCatching { Einheiten.umrechnen(it, von, nach) }.getOrNull() }
    }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Einheiten.KATEGORIEN.forEachIndexed { i, (n, _) -> FilterChip(selected = kat == i, onClick = { kat = i }, label = { Text(n) }) }
        }
        Karte {
            KartenTitel(name)
            OutlinedTextField(wert, { wert = it }, label = { Text("Wert") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            Grau("von", Modifier.padding(top = 10.dp, bottom = 4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { einheiten.forEach { e -> FilterChip(selected = von == e, onClick = { von = e }, label = { Text(e) }) } }
            Grau("nach", Modifier.padding(top = 6.dp, bottom = 4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { einheiten.forEach { e -> FilterChip(selected = nach == e, onClick = { nach = e }, label = { Text(e) }) } }
            Spacer(Modifier.height(12.dp))
            Text(
                if (ergebnis == null) "–" else "$wert $von = ${Zahlen.text(ergebnis, 8)} $nach",
                fontFamily = LeseSchrift, fontSize = 22.sp,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Lernen.farben.ton).padding(12.dp),
            )
        }
    }
}

// ---------- Funktionsplotter ----------

@Composable
internal fun Plotter(vorbelegt: String?) {
    var f1 by rememberSaveable { mutableStateOf(vorbelegt ?: "0,5x² − 2") }
    var f2 by rememberSaveable { mutableStateOf("") }
    var aktiv by rememberSaveable { mutableStateOf(0) }
    var bereich by rememberSaveable { mutableStateOf(10.0) }
    val farben = listOf(MaterialTheme.colorScheme.primary, Lernen.farben.schlecht)
    val l = Lernen.farben
    val messer = rememberTextMeasurer()
    val terme = listOf(f1, f2).map { t -> if (t.isBlank()) null else runCatching { Parser.term(t.substringAfter("=")) }.getOrNull() }
    Column(Modifier.fillMaxSize()) {
        listOf(f1, f2).forEachIndexed { i, t ->
            Row(Modifier.padding(bottom = 6.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("f${i + 1}(x) =", color = farben[i], fontFamily = LeseSchrift, modifier = Modifier.width(70.dp))
                Eingabezeile(t, if (i == 0) "z. B. 2x + 1" else "zweite Funktion (optional)", aktiv = aktiv == i, onClick = { aktiv = i }, modifier = Modifier.weight(1f))
            }
            if (t.isNotBlank() && terme[i] == null) Text("Kann ich nicht lesen.", color = l.schlecht, style = MaterialTheme.typography.bodySmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = { bereich = (bereich / 2).coerceAtLeast(0.5) }) { Text("Zoom +") }
            OutlinedButton(onClick = { bereich = (bereich * 2).coerceAtMost(1000.0) }) { Text("Zoom −") }
            OutlinedButton(onClick = { bereich = 10.0 }) { Text("±10") }
        }
        Canvas(Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp).clip(RoundedCornerShape(12.dp)).background(l.karte)) {
            val w = size.width; val h = size.height
            val xr = bereich; val yr = bereich * h / w
            fun px(x: Double) = ((x + xr) / (2 * xr) * w).toFloat()
            fun py(y: Double) = ((yr - y) / (2 * yr) * h).toFloat()
            val schritt = 10.0.pow(floor(log10(xr / 2))).let { if (xr / it > 12) it * 2 else it }
            var g = -floor(xr / schritt) * schritt
            while (g <= xr) { drawLine(l.ton, Offset(px(g), 0f), Offset(px(g), h), 1f); g += schritt }
            g = -floor(yr / schritt) * schritt
            while (g <= yr) { drawLine(l.ton, Offset(0f, py(g)), Offset(w, py(g)), 1f); g += schritt }
            drawLine(l.text2, Offset(0f, py(0.0)), Offset(w, py(0.0)), 2f)
            drawLine(l.text2, Offset(px(0.0), 0f), Offset(px(0.0), h), 2f)
            val stil = TextStyle(fontSize = 10.sp, color = l.text2)
            drawText(messer, Zahlen.text(schritt), Offset(px(schritt) + 2, py(0.0) + 2), stil)
            drawText(messer, Zahlen.text(schritt), Offset(px(0.0) + 4, py(schritt) - 14), stil)
            terme.forEachIndexed { i, a ->
                a ?: return@forEachIndexed
                val p = Path(); var offen = false
                val n = 600
                for (k in 0..n) {
                    val x = -xr + 2 * xr * k / n
                    val y = runCatching { a.wert(mapOf("x" to x), Winkel.BOGEN) }.getOrDefault(Double.NaN)
                    if (!y.isFinite() || abs(y) > yr * 50) { offen = false; continue }
                    if (!offen) { p.moveTo(px(x), py(y)); offen = true } else p.lineTo(px(x), py(y))
                }
                drawPath(p, farben[i], style = Stroke(width = 4f))
            }
        }
        Tastatur(MATHE_TASTEN.map { r -> r.map { if (it.text == "⏎") Taste("Fertig", "⏎", Taste.Art.HAUPT, 2f) else it } }, onTaste = { t ->
            if (t.text == "⏎" || t.text == "=") return@Tastatur
            if (aktiv == 0) f1 = tippe(f1, t) else f2 = tippe(f2, t)
        }, modifier = Modifier.padding(bottom = 8.dp))
    }
}
